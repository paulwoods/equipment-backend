package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.service.JwtService;
import com.mrpaulwoods.equipment.backend.service.RefreshTokenService;
import com.mrpaulwoods.equipment.backend.service.UserDetailsServiceImpl;
import com.mrpaulwoods.equipment.backend.service.UserService;
import com.mrpaulwoods.equipment.backend.util.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SetupControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private SetupController setupController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(setupController).build();
    }

    @Test
    void status_whenNoUsers_returnsSetupRequired() throws Exception {
        when(userService.isSetupRequired()).thenReturn(true);

        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true));
    }

    @Test
    void status_whenUsersExist_returnsSetupNotRequired() throws Exception {
        when(userService.isSetupRequired()).thenReturn(false);

        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(false));
    }

    @Test
    void setup_whenNoUsers_createsSystemAdminAndReturnsEmail() throws Exception {
        when(userService.isSetupRequired()).thenReturn(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setRole(Role.SYSTEM_ADMIN);
        when(userService.createInternal("admin@example.com", "admin@example.com", "password123", Role.SYSTEM_ADMIN)).thenReturn(user);

        UserDetails ud = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "hashed", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(ud);
        when(jwtService.generateToken(ud)).thenReturn("access-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        String body = """
                {"email": "admin@example.com", "password": "password123"}
                """;

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void setup_overHttps_setsSecureHardenedCookies() throws Exception {
        when(appProperties.getCookieSecure()).thenReturn(true);
        when(userService.isSetupRequired()).thenReturn(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setRole(Role.SYSTEM_ADMIN);
        when(userService.createInternal("admin@example.com", "admin@example.com", "password123", Role.SYSTEM_ADMIN)).thenReturn(user);

        UserDetails ud = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "hashed", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(ud);
        when(jwtService.generateToken(ud)).thenReturn("access-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        MvcResult result = mockMvc.perform(post("/api/v1/setup")
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123"}
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
        when(userService.isSetupRequired()).thenReturn(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setRole(Role.SYSTEM_ADMIN);
        when(userService.createInternal("admin@example.com", "admin@example.com", "password123", Role.SYSTEM_ADMIN)).thenReturn(user);

        UserDetails ud = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "hashed", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(ud);
        when(jwtService.generateToken(ud)).thenReturn("access-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        MvcResult result = mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123"}
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
        when(userService.isSetupRequired()).thenReturn(false);

        String body = """
                {"email": "another@example.com", "password": "password123"}
                """;

        mockMvc.perform(post("/api/v1/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
}
