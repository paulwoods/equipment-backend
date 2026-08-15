package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.dto.SetupRequest;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.ratelimit.SetupRateLimiterService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Setup", description = "First-run admin account creation")
public class SetupController {

    private final AdminBootstrap adminBootstrap;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;
    private final CookieService cookieService;
    private final SetupRateLimiterService setupRateLimiter;
    private final AppProperties appProperties;

    @Operation(summary = "Check whether initial setup is required")
    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("setupRequired", adminBootstrap.isSetupRequired());
    }

    @Operation(summary = "Create the initial admin account")
    @PostMapping
    public ResponseEntity<Map<String, String>> setup(
            @Valid @RequestBody SetupRequest setupRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (!setupRateLimiter.tryAcquire(request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many setup attempts");
        }

        if (!adminBootstrap.isSetupRequired()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Setup already completed");
        }

        requireSetupToken(setupRequest.setupToken());

        User user = adminBootstrap.createInitialAdmin(setupRequest.email(), setupRequest.password());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails, user.getTokenVersion());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        cookieService.setAccessTokenCookie(request, response, accessToken);
        cookieService.setRefreshTokenCookie(request, response, refreshToken.rawToken());

        return ResponseEntity.ok(Map.of("email", user.getEmail()));
    }

    /**
     * Fails closed when no token is configured: an un-provisioned deployment refuses
     * setup rather than handing the only admin account to whoever gets there first.
     */
    private void requireSetupToken(String presented) {
        String expected = appProperties.getSetupToken();
        if (expected == null || expected.isBlank()) {
            log.error("Rejecting setup: app.setup-token (APP_SETUP_TOKEN) is not configured");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Setup is not configured");
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] presentedBytes = (presented == null ? "" : presented).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, presentedBytes)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid setup token");
        }
    }

}
