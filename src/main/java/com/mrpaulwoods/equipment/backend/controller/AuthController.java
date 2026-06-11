package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.ForgotPasswordRequest;
import com.mrpaulwoods.equipment.backend.dto.LoginRequest;
import com.mrpaulwoods.equipment.backend.dto.ResetPasswordRequest;
import com.mrpaulwoods.equipment.backend.dto.UserResponse;
import com.mrpaulwoods.equipment.backend.service.AuthService;
import com.mrpaulwoods.equipment.backend.service.CookieService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh, and current-user endpoints")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    @Operation(summary = "Log in with email and password")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthService.IssuedTokens tokens = authService.login(
                loginRequest.email(), loginRequest.password(), request.getRemoteAddr());

        cookieService.setAccessTokenCookie(request, response, tokens.accessToken());
        cookieService.setRefreshTokenCookie(request, response, tokens.rawRefreshToken());

        return ResponseEntity.ok(Map.of("email", tokens.email()));
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
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.forgotPassword(request.email(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "If the email exists in our system, you will receive reset instructions shortly."));
    }

    @Operation(summary = "Reset password using a token")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated"));
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
