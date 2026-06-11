package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.RoleResponse;
import com.mrpaulwoods.equipment.backend.dto.UserResponse;
import com.mrpaulwoods.equipment.backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;
    private final LoginRateLimiterService loginRateLimiter;
    private final ForgotPasswordRateLimiterService forgotPasswordRateLimiter;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Encoded at startup so the dummy hash always matches the encoder's current cost
    // factor; used to equalize timing when the user does not exist.
    private final String dummyPasswordHash;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserDetailsServiceImpl userDetailsService,
            UserService userService,
            LoginRateLimiterService loginRateLimiter,
            ForgotPasswordRateLimiterService forgotPasswordRateLimiter,
            PasswordResetService passwordResetService,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.loginRateLimiter = loginRateLimiter;
        this.forgotPasswordRateLimiter = forgotPasswordRateLimiter;
        this.passwordResetService = passwordResetService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record IssuedTokens(String email, String accessToken, String rawRefreshToken) {
    }

    public IssuedTokens login(String email, String password, String clientIp) {
        if (loginRateLimiter.isBlocked(clientIp, email)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts");
        }

        Optional<User> existingUser = userService.findByEmail(email);
        if (existingUser.isEmpty()) {
            // Equalize timing with the password-mismatch path so attackers cannot enumerate users.
            passwordEncoder.matches(password, dummyPasswordHash);
            loginRateLimiter.recordFailure(clientIp, email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            User user = existingUser.get();
            String accessToken = jwtService.generateToken(userDetails, user.getTokenVersion());
            RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            loginRateLimiter.recordSuccess(clientIp, email);
            return new IssuedTokens(userDetails.getUsername(), accessToken, refreshToken.rawToken());
        } catch (AuthenticationException e) {
            loginRateLimiter.recordFailure(clientIp, email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    public void logout(String email) {
        if (email == null) {
            return;
        }
        userService.findByEmail(email).ifPresent(user -> {
            refreshTokenService.deleteByUser(user);
            userService.bumpTokenVersion(user);
        });
    }

    public IssuedTokens refresh(String refreshTokenValue) {
        if (refreshTokenValue == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token");
        }

        RefreshTokenService.IssuedRefreshToken newRefreshToken = refreshTokenService.validateAndRotate(refreshTokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        User user = newRefreshToken.refreshToken().getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails, user.getTokenVersion());

        return new IssuedTokens(user.getEmail(), newAccessToken, newRefreshToken.rawToken());
    }

    public void forgotPassword(String email, String clientIp) {
        if (forgotPasswordRateLimiter.isBlocked(clientIp)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts");
        }

        forgotPasswordRateLimiter.recordRequest(clientIp);

        userService.findByEmail(email).ifPresent(user -> {
            String token = passwordResetService.createResetToken(user);
            try {
                emailService.sendPasswordResetEmail(email, token);
            } catch (Exception e) {
                log.error("Failed to send password reset email", e);
            }
        });
    }

    public void resetPassword(String token, String newPassword) {
        passwordResetService.resetPassword(token, newPassword);
    }

    public UserResponse currentUser(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Set<RoleResponse> roles = user.getUserRoles().stream()
                .map(ur -> new RoleResponse(ur.getRole().getId(), ur.getRole().getName()))
                .collect(Collectors.toSet());

        return new UserResponse(user.getId(), user.getName(), email, roles);
    }
}
