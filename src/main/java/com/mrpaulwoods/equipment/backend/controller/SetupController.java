package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.SetupRequest;
import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import com.mrpaulwoods.equipment.backend.service.JwtService;
import com.mrpaulwoods.equipment.backend.service.RefreshTokenService;
import com.mrpaulwoods.equipment.backend.service.UserDetailsServiceImpl;
import com.mrpaulwoods.equipment.backend.service.UserService;
import com.mrpaulwoods.equipment.backend.util.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SetupController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("setupRequired", userRepository.count() == 0);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> setup(
            @Valid @RequestBody SetupRequest setupRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (userRepository.count() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Setup already completed");
        }

        User user = userService.createInternal(setupRequest.getEmail(), setupRequest.getPassword(), Role.ADMIN);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(3600)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken.getToken())
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(7 * 24 * 3600)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(Map.of("email", user.getEmail()));
    }
}
