package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.SetupRequest;
import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
@Tag(name = "Setup", description = "First-run admin account creation")
public class SetupController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;
    private final CookieService cookieService;

    @Operation(summary = "Check whether initial setup is required")
    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("setupRequired", userService.isSetupRequired());
    }

    @Operation(summary = "Create the initial admin account")
    @PostMapping
    public ResponseEntity<Map<String, String>> setup(
            @Valid @RequestBody SetupRequest setupRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (!userService.isSetupRequired()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Setup already completed");
        }

        User user = userService.createInternal(setupRequest.email(), setupRequest.email(), setupRequest.password(), Set.of("SYSTEM_ADMIN"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        cookieService.setAccessTokenCookie(request, response, accessToken);
        cookieService.setRefreshTokenCookie(request, response, refreshToken.getToken());

        return ResponseEntity.ok(Map.of("email", user.getEmail()));
    }

}
