package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.ForgotPasswordRequest;
import com.mrpaulwoods.equipment.backend.dto.GoogleConfigResponse;
import com.mrpaulwoods.equipment.backend.dto.GoogleLoginRequest;
import com.mrpaulwoods.equipment.backend.dto.LoginRequest;
import com.mrpaulwoods.equipment.backend.dto.LoginResponse;
import com.mrpaulwoods.equipment.backend.dto.MessageResponse;
import com.mrpaulwoods.equipment.backend.dto.ResetPasswordRequest;
import com.mrpaulwoods.equipment.backend.dto.UserResponse;
import com.mrpaulwoods.equipment.backend.service.AuthService;
import com.mrpaulwoods.equipment.backend.service.CookieService;
import com.mrpaulwoods.equipment.backend.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh, and current-user endpoints")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final CookieService cookieService;

    @Operation(summary = "Log in with email and password")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthService.IssuedTokens tokens = authService.login(
                loginRequest.email(), loginRequest.password(), request.getRemoteAddr());

        cookieService.setAccessTokenCookie(request, response, tokens.accessToken());
        cookieService.setRefreshTokenCookie(request, response, tokens.rawRefreshToken());

        return ResponseEntity.ok(new LoginResponse(tokens.email()));
    }

    @Operation(summary = "Report whether Google sign-in is available, and its OAuth client ID")
    @GetMapping("/google/config")
    public ResponseEntity<GoogleConfigResponse> googleConfig() {
        return ResponseEntity.ok(googleAuthService.config());
    }

    @Operation(summary = "Log in with a Google ID token")
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest googleLoginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthService.IssuedTokens tokens = googleAuthService.login(googleLoginRequest.credential());

        cookieService.setAccessTokenCookie(request, response, tokens.accessToken());
        cookieService.setRefreshTokenCookie(request, response, tokens.rawRefreshToken());

        return ResponseEntity.ok(new LoginResponse(tokens.email()));
    }

    @Operation(summary = "Log out and clear auth cookies")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        authService.logout(authentication != null ? authentication.getName() : null);
        cookieService.clearCookie(request, response, "access_token", "/");
        cookieService.clearCookie(request, response, "refresh_token", "/api/v1/auth");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rotate refresh token and issue a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.refresh(cookieService.getCookieValue(request, "refresh_token"));

        cookieService.setAccessTokenCookie(request, response, tokens.accessToken());
        cookieService.setRefreshTokenCookie(request, response, tokens.rawRefreshToken());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Request a password reset email")
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.forgotPassword(request.email(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(new MessageResponse("If the email exists in our system, you will receive reset instructions shortly."));
    }

    @Operation(summary = "Reset password using a token")
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse("Password updated"));
    }

    @Operation(summary = "Return the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return ResponseEntity.ok(authService.currentUser(authentication.getName()));
    }

}
