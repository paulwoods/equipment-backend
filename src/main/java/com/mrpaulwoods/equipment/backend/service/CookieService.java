package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;

@Service
public class CookieService {

    private final AppProperties appProperties;

    public CookieService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void setAccessTokenCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        // Cookie lifetime tracks the JWT lifetime so the cookie never outlives (or kills) the token.
        long maxAge = appProperties.getJwtExpirationMs() / 1000;
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("access_token", token, "/", maxAge, request));
    }

    public void setRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        long maxAge = Duration.ofDays(appProperties.getRefreshTokenDays()).toSeconds();
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("refresh_token", token, "/api/v1/auth", maxAge, request));
    }

    public void clearCookie(HttpServletRequest request, HttpServletResponse response, String name, String path) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(name, "", path, 0, request));
    }

    public String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String buildCookie(String name, String value, String path, long maxAge, HttpServletRequest request) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(isSecure(request))
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build()
                .toString();
    }

    private boolean isSecure(HttpServletRequest request) {
        Boolean override = appProperties.getCookieSecure();
        return override != null ? override : request.isSecure();
    }
}
