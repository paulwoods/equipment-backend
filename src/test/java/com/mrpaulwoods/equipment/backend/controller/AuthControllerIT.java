package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.support.BaseIT;
import com.mrpaulwoods.equipment.backend.util.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for US-003 Login, US-004 Rate Limiting, US-005 Token Refresh,
 * US-006 Logout, US-007 Unauthenticated Redirect.
 */
class AuthControllerIT extends BaseIT {

    // ── US-003: Login ─────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returns200AndSetsTokenCookies() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}";

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("email");

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anyMatch(c -> c.startsWith("access_token="));
        assertThat(cookies).anyMatch(c -> c.startsWith("refresh_token="));
    }

    @Test
    void login_withInvalidPassword_returns401() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrongpassword\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_withUnknownEmail_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"nobody@example.com\",\"password\":\"whatever\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_accessTokenCookieHasMaxAge3600() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        String accessCookie = response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("access_token="))
                .findFirst().orElseThrow();
        assertThat(accessCookie).containsIgnoringCase("Max-Age=3600");
        assertThat(accessCookie).containsIgnoringCase("HttpOnly");
    }

    @Test
    void login_refreshTokenCookieHasMaxAge7Days() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        String refreshCookie = response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .findFirst().orElseThrow();
        assertThat(refreshCookie).containsIgnoringCase("Max-Age=604800");
        assertThat(refreshCookie).containsIgnoringCase("HttpOnly");
    }

    @Test
    void login_successResetsFailureCounter_allowsSubsequentLogin() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Fail twice
        String badBody = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrong\"}";
        for (int i = 0; i < 2; i++) {
            restTemplate.exchange(url("/api/v1/auth/login"), HttpMethod.POST,
                    new HttpEntity<>(badBody, headers), String.class);
        }

        // Succeed — should reset counter
        String goodBody = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}";
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(goodBody, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── US-004: Rate Limiting ──────────────────────────────────────────────────

    @Test
    void login_after5FailedAttempts_returns429() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String badBody = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrong\"}";

        for (int i = 0; i < 5; i++) {
            restTemplate.exchange(url("/api/v1/auth/login"), HttpMethod.POST,
                    new HttpEntity<>(badBody, headers), String.class);
        }

        ResponseEntity<String> blocked = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(badBody, headers), String.class);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ── US-005: Token Refresh ─────────────────────────────────────────────────

    @Test
    void refresh_withValidRefreshToken_issuesNewTokens() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);

        // Log in to get cookies
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(loginBody, loginHeaders), String.class);

        String refreshCookie = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .map(c -> c.split(";", 2)[0])
                .findFirst().orElseThrow();

        long tokensBefore = refreshTokenRepository.count();

        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, refreshCookie);

        ResponseEntity<String> refreshResponse = restTemplate.exchange(
                url("/api/v1/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(refreshHeaders), String.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> newCookies = refreshResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(newCookies).anyMatch(c -> c.startsWith("access_token="));
        assertThat(newCookies).anyMatch(c -> c.startsWith("refresh_token="));
        // Old token deleted, new one created — count stays same
        assertThat(refreshTokenRepository.count()).isEqualTo(tokensBefore);
    }

    @Test
    void refresh_withNoRefreshToken_returns401() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_withInvalidToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refresh_token=not-a-real-token");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_oldRefreshTokenIsInvalidatedAfterRotation() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(loginBody, loginHeaders), String.class);

        String originalRefreshCookie = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .map(c -> c.split(";", 2)[0])
                .findFirst().orElseThrow();

        // Use the refresh token once
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, originalRefreshCookie);
        restTemplate.exchange(url("/api/v1/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(refreshHeaders), String.class);

        // Reuse the old token — should be rejected
        ResponseEntity<String> reuse = restTemplate.exchange(
                url("/api/v1/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(refreshHeaders), String.class);

        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── US-006: Logout ────────────────────────────────────────────────────────

    @Test
    void logout_clearsTokensAndDeletesRefreshTokenFromDb() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);
        String accessCookie = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        long tokensBefore = refreshTokenRepository.count();
        assertThat(tokensBefore).isEqualTo(1);

        HttpHeaders headers = cookieHeaders(accessCookie);
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/v1/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshTokenRepository.count()).isEqualTo(0);

        // Cookies should be cleared (Max-Age=0)
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anyMatch(c -> c.startsWith("access_token=") && c.contains("Max-Age=0"));
    }

    @Test
    void logout_withoutAuth_returns200() {
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/v1/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── US-007: Unauthenticated Redirect ──────────────────────────────────────

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/v1/equipment"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withExpiredOrFakeToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "access_token=this.is.fake");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/equipment"), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── /api/auth/me ──────────────────────────────────────────────────────────

    @Test
    void me_withValidToken_returnsEmailAndRole() {
        userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);
        String accessCookie = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/auth/me"), HttpMethod.GET,
                new HttpEntity<>(cookieHeaders(accessCookie)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", ADMIN_EMAIL);
        assertThat(response.getBody()).containsKey("role");
    }
}
