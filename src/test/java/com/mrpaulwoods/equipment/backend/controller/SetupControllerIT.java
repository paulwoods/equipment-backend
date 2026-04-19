package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.service.UserService;
import com.mrpaulwoods.equipment.backend.support.BaseIT;
import com.mrpaulwoods.equipment.backend.util.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for US-001 Initial Admin Setup.
 *
 * <p>Exercises the real Spring Security filter chain, JPA persistence against a Postgres container,
 * BCrypt password hashing, JWT issuance, and refresh-token persistence end-to-end.
 *
 * <p>Docker must be available on the host for Testcontainers to start the Postgres container.
 */
class SetupControllerIT extends BaseIT {

    private static final String SETUP_STATUS = "/api/v1/setup/status";
    private static final String SETUP = "/api/v1/setup";
    private static final String PROTECTED_ENDPOINT = "/api/v1/equipment";

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void status_returnsSetupRequiredTrue_whenNoUsersExist() {
        ResponseEntity<Map<String, Boolean>> response = getStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("setupRequired", true);
    }

    @Test
    void status_returnsSetupRequiredFalse_whenAdminExists() {
        userService.createInternal("existing@example.com", "pw123456", Role.ADMIN);

        ResponseEntity<Map<String, Boolean>> response = getStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("setupRequired", false);
    }

    @Test
    void setup_createsFirstUserAsAdmin_andPersistsBcryptHashedPassword() {
        String email = "admin@example.com";
        String password = "correct-horse-battery-staple";

        ResponseEntity<Map<String, String>> response = postSetup(email, password);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", email);

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        User created = users.get(0);
        assertThat(created.getEmail()).isEqualTo(email);
        assertThat(created.getRole()).isEqualTo(Role.ADMIN);
        assertThat(created.getPassword()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, created.getPassword())).isTrue();
    }

    @Test
    void setup_issuesAccessAndRefreshCookies_andPersistsRefreshToken() {
        ResponseEntity<Map<String, String>> response = postSetup("admin@example.com", "pw123456");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies).anyMatch(c -> c.startsWith("access_token="));
        assertThat(cookies).anyMatch(c -> c.startsWith("refresh_token="));

        User createdUser = userRepository.findByEmail("admin@example.com").orElseThrow();
        assertThat(refreshTokenRepository.findAll())
                .hasSize(1)
                .allSatisfy(rt -> assertThat(rt.getUser().getId()).isEqualTo(createdUser.getId()));
    }

    @Test
    void setup_returnsConflict_whenAnyUserAlreadyExists() {
        userService.createInternal("first@example.com", "pw123456", Role.ADMIN);
        long userCountBefore = userRepository.count();
        long refreshTokensBefore = refreshTokenRepository.count();

        ResponseEntity<Map<String, String>> response = postSetup("second@example.com", "pw123456");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(userRepository.count()).isEqualTo(userCountBefore);
        assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokensBefore);
    }

    @Test
    void setup_afterSuccess_statusFlipsToSetupRequiredFalse() {
        assertThat(getStatus().getBody()).containsEntry("setupRequired", true);

        ResponseEntity<Map<String, String>> setupResponse = postSetup("admin@example.com", "pw123456");
        assertThat(setupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(getStatus().getBody()).containsEntry("setupRequired", false);
    }

    @Test
    void setup_issuedAccessTokenAuthenticatesProtectedEndpoint() {
        ResponseEntity<Map<String, String>> setupResponse = postSetup("admin@example.com", "pw123456");
        assertThat(setupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String accessCookie = setupResponse.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("access_token="))
                .map(c -> c.split(";", 2)[0])
                .findFirst()
                .orElseThrow();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, accessCookie);

        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                url(PROTECTED_ENDPOINT),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> unauthenticated = restTemplate.getForEntity(url(PROTECTED_ENDPOINT), String.class);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Map<String, Boolean>> getStatus() {
        return restTemplate.exchange(
                url(SETUP_STATUS),
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Boolean>>() {
                }
        );
    }

    private ResponseEntity<Map<String, String>> postSetup(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        return restTemplate.exchange(
                url(SETUP),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new org.springframework.core.ParameterizedTypeReference<Map<String, String>>() {
                }
        );
    }
}
