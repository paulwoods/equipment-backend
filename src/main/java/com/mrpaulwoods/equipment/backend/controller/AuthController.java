package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh, and current-user endpoints")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;
    private final CookieService cookieService;
    private final LoginRateLimiterService loginRateLimiter;
    private final ForgotPasswordRateLimiterService forgotPasswordRateLimiter;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Pre-computed BCrypt hash of a random value, used to equalize timing when the user does not exist.
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOa3wfP3o4Bz1pXQp6n1XJ8B5y9C5VYqK";

    @Operation(summary = "Log in with email and password")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String clientIp = request.getRemoteAddr();
        String email = loginRequest.email();

        if (loginRateLimiter.isBlocked(clientIp, email)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts");
        }

        Optional<User> existingUser = userService.findByEmail(email);
        if (existingUser.isEmpty()) {
            // Equalize timing with the password-mismatch path so attackers cannot enumerate users.
            passwordEncoder.matches(loginRequest.password(), DUMMY_BCRYPT_HASH);
            loginRateLimiter.recordFailure(clientIp, email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.password())
            );
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            assert userDetails != null;

            User user = existingUser.get();
            String accessToken = jwtService.generateToken(userDetails, user.getTokenVersion());
            RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            cookieService.setAccessTokenCookie(request, response, accessToken);
            cookieService.setRefreshTokenCookie(request, response, refreshToken.rawToken());

            loginRateLimiter.recordSuccess(clientIp, email);
            return ResponseEntity.ok(Map.of("email", userDetails.getUsername()));
        } catch (AuthenticationException e) {
            loginRateLimiter.recordFailure(clientIp, email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    @Operation(summary = "Log out and clear auth cookies")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (authentication != null) {
            userService.findByEmail(authentication.getName()).ifPresent(user -> {
                refreshTokenService.deleteByUser(user);
                userService.bumpTokenVersion(user);
            });
        }
        cookieService.clearCookie(request, response, "access_token", "/");
        cookieService.clearCookie(request, response, "refresh_token", "/api/v1/auth");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rotate refresh token and issue a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieService.getCookieValue(request, "refresh_token");
        if (refreshTokenValue == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token");
        }

        RefreshTokenService.IssuedRefreshToken newRefreshToken = refreshTokenService.validateAndRotate(refreshTokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        User user = newRefreshToken.refreshToken().getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails, user.getTokenVersion());

        cookieService.setAccessTokenCookie(request, response, newAccessToken);
        cookieService.setRefreshTokenCookie(request, response, newRefreshToken.rawToken());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Request a password reset email")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();

        if (forgotPasswordRateLimiter.isBlocked(clientIp)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts");
        }

        forgotPasswordRateLimiter.recordRequest(clientIp);

        Optional<User> userOpt = userService.findByEmail(request.email());
        if (userOpt.isPresent()) {
            String token = passwordResetService.createResetToken(userOpt.get());
            try {
                emailService.sendPasswordResetEmail(request.email(), token);
            } catch (Exception e) {
                log.error("Failed to send password reset email", e);
            }
        }

        return ResponseEntity.ok(Map.of("message", "If the email exists in our system, you will receive reset instructions shortly."));
    }

    @Operation(summary = "Reset password using a token")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    @Operation(summary = "Return the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok().build();
        }
        String email = authentication.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Set<RoleResponse> roles = user.getUserRoles().stream()
                .map(ur -> new RoleResponse(ur.getRole().getId(), ur.getRole().getName()))
                .collect(java.util.stream.Collectors.toSet());

        return ResponseEntity.ok(new UserResponse(user.getId(), user.getName(), email, roles));
    }

}
