package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private AppProperties appProperties;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(appProperties.getJwtSecret()).thenReturn("test-secret-key-must-be-at-least-32-chars!!");
        lenient().when(appProperties.getJwtExpirationMs()).thenReturn(3_600_000L);
        jwtService = new JwtService(appProperties);
    }

    private UserDetails userDetails(String email, String role) {
        return new User(email, "password", List.of(new SimpleGrantedAuthority(role)));
    }

    @Test
    void generateToken_returnsNonNullToken() {
        UserDetails ud = userDetails("user@example.com", "ROLE_USER");
        String token = jwtService.generateToken(ud, 0L);
        assertThat(token).isNotBlank();
    }

    @Test
    void validate_withValidToken_returnsEmailAndTokenVersion() {
        UserDetails ud = userDetails("user@example.com", "ROLE_USER");
        String token = jwtService.generateToken(ud, 5L);
        assertThat(jwtService.validate(token))
                .hasValue(new JwtService.ValidToken("user@example.com", 5L));
    }

    @Test
    void validate_withGarbageToken_returnsEmpty() {
        assertThat(jwtService.validate("not.a.token")).isEmpty();
    }

    @Test
    void validate_withExpiredToken_returnsEmpty() {
        when(appProperties.getJwtExpirationMs()).thenReturn(-1000L);
        jwtService = new JwtService(appProperties);
        UserDetails ud = userDetails("user@example.com", "ROLE_USER");
        String token = jwtService.generateToken(ud, 0L);
        assertThat(jwtService.validate(token)).isEmpty();
    }
}
