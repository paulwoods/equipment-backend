package com.mrpaulwoods.equipment.backend.controller;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.ratelimit.SetupRateLimiterService;
import com.mrpaulwoods.equipment.backend.ratelimit.WindowedCounterFactory;
import com.mrpaulwoods.equipment.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SetupControllerTest {

    @Mock
    private AdminBootstrap adminBootstrap;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    private AppProperties appProperties;

    private static final String SETUP_TOKEN = "operator-secret";

    private SetupController setupController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setSetupToken(SETUP_TOKEN);
        setupController = new SetupController(
                adminBootstrap,
                jwtService,
                refreshTokenService,
                userDetailsService,
                new CookieService(appProperties),
                new SetupRateLimiterService(appProperties, new WindowedCounterFactory(Ticker.systemTicker())),
                appProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(setupController).build();
    }

    @Test
    void status_whenNoUsers_returnsSetupRequired() throws Exception {
        when(adminBootstrap.isSetupRequired()).thenReturn(true);

        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true));
    }

    @Test
    void status_whenUsersExist_returnsSetupNotRequired() throws Exception {
        when(adminBootstrap.isSetupRequired()).thenReturn(false);

        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(false));
    }

    @Test
    void setup_whenNoUsers_createsSystemAdminAndReturnsEmail() throws Exception {
        when(adminBootstrap.isSetupRequired()).thenReturn(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        when(adminBootstrap.createInitialAdmin("admin@example.com", "password123")).thenReturn(user);

        UserDetails ud = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "hashed", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"), new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_EDIT"), new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(ud);
        when(jwtService.generateToken(eq(ud), anyLong())).thenReturn("access-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken("hashed-token");
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(new RefreshTokenService.IssuedRefreshToken(UUID.randomUUID().toString(), rt));

        String body = """
                {"email": "admin@example.com", "password": "password123", "setupToken": "operator-secret"}
                """;

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void setup_overHttps_setsSecureHardenedCookies() throws Exception {
        appProperties.setCookieSecure(true);
        when(adminBootstrap.isSetupRequired()).thenReturn(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        when(adminBootstrap.createInitialAdmin("admin@example.com", "password123")).thenReturn(user);

        UserDetails ud = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "hashed", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"), new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_EDIT"), new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(ud);
        when(jwtService.generateToken(eq(ud), anyLong())).thenReturn("access-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken("hashed-token");
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(new RefreshTokenService.IssuedRefreshToken(UUID.randomUUID().toString(), rt));

        MvcResult result = mockMvc.perform(post("/api/v1/setup")
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123", "setupToken": "operator-secret"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(2);

        String accessHeader = setCookies.stream().filter(h -> h.startsWith("access_token=")).findFirst().orElseThrow();
        assertThat(accessHeader).contains("Path=/");
        assertThat(accessHeader).contains("Max-Age=3600");
        assertThat(accessHeader).contains("HttpOnly");
        assertThat(accessHeader).contains("SameSite=Lax");
        assertThat(accessHeader).contains("Secure");

        String refreshHeader = setCookies.stream().filter(h -> h.startsWith("refresh_token=")).findFirst().orElseThrow();
        assertThat(refreshHeader).contains("Path=/api/v1/auth");
        assertThat(refreshHeader).contains("Max-Age=604800");
        assertThat(refreshHeader).contains("HttpOnly");
        assertThat(refreshHeader).contains("SameSite=Lax");
        assertThat(refreshHeader).contains("Secure");
    }

    @Test
    void setup_overHttp_doesNotSetSecureFlag() throws Exception {
        when(adminBootstrap.isSetupRequired()).thenReturn(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        when(adminBootstrap.createInitialAdmin("admin@example.com", "password123")).thenReturn(user);

        UserDetails ud = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "hashed", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"), new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_EDIT"), new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(ud);
        when(jwtService.generateToken(eq(ud), anyLong())).thenReturn("access-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken("hashed-token");
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(new RefreshTokenService.IssuedRefreshToken(UUID.randomUUID().toString(), rt));

        MvcResult result = mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123", "setupToken": "operator-secret"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        for (String h : setCookies) {
            assertThat(h).doesNotContain("Secure");
            assertThat(h).contains("HttpOnly");
            assertThat(h).contains("SameSite=Lax");
        }
    }

    @Test
    void setup_whenUsersExist_returns409() throws Exception {
        when(adminBootstrap.isSetupRequired()).thenReturn(false);

        String body = """
                {"email": "another@example.com", "password": "password123", "setupToken": "operator-secret"}
                """;

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void setup_whenServiceDetectsConcurrentSetup_returns409() throws Exception {
        when(adminBootstrap.isSetupRequired()).thenReturn(true);
        when(adminBootstrap.createInitialAdmin("admin@example.com", "password123"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Setup already completed"));

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123", "setupToken": "operator-secret"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void setup_withInvalidBody_returns400() throws Exception {
        String body = """
                {"email": "not-an-email", "password": ""}
                """;

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setup_withWrongToken_returns403AndCreatesNothing() throws Exception {
        when(adminBootstrap.isSetupRequired()).thenReturn(true);

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "attacker@evil.com", "password": "password123", "setupToken": "guess"}
                                """))
                .andExpect(status().isForbidden());

        verify(adminBootstrap, never()).createInitialAdmin(anyString(), anyString());
    }

    @Test
    void setup_withNoToken_returns403AndCreatesNothing() throws Exception {
        when(adminBootstrap.isSetupRequired()).thenReturn(true);

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "attacker@evil.com", "password": "password123"}
                                """))
                .andExpect(status().isForbidden());

        verify(adminBootstrap, never()).createInitialAdmin(anyString(), anyString());
    }

    @Test
    void setup_whenNoTokenIsConfigured_failsClosedWith503() throws Exception {
        appProperties.setSetupToken("");
        when(adminBootstrap.isSetupRequired()).thenReturn(true);

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "attacker@evil.com", "password": "password123", "setupToken": "anything"}
                                """))
                .andExpect(status().isServiceUnavailable());

        verify(adminBootstrap, never()).createInitialAdmin(anyString(), anyString());
    }

    @Test
    void setup_pastTheAttemptBudget_returns429BeforeTouchingTheBootstrap() throws Exception {
        appProperties.setSetupMaxAttempts(2);
        when(adminBootstrap.isSetupRequired()).thenReturn(true);
        setupController = new SetupController(
                adminBootstrap,
                jwtService,
                refreshTokenService,
                userDetailsService,
                new CookieService(appProperties),
                new SetupRateLimiterService(appProperties, new WindowedCounterFactory(Ticker.systemTicker())),
                appProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(setupController).build();

        String guess = """
                {"email": "attacker@evil.com", "password": "password123", "setupToken": "guess"}
                """;

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/setup").contentType(MediaType.APPLICATION_JSON).content(guess))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(post("/api/v1/setup").contentType(MediaType.APPLICATION_JSON).content(guess))
                .andExpect(status().isTooManyRequests());

        verify(adminBootstrap, never()).createInitialAdmin(anyString(), anyString());
    }
}
