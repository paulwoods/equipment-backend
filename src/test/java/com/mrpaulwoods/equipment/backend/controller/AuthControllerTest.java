package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import com.mrpaulwoods.equipment.backend.service.JwtService;
import com.mrpaulwoods.equipment.backend.service.LoginRateLimiterService;
import com.mrpaulwoods.equipment.backend.service.RefreshTokenService;
import com.mrpaulwoods.equipment.backend.service.UserDetailsServiceImpl;
import com.mrpaulwoods.equipment.backend.util.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private LoginRateLimiterService loginRateLimiter;

    private AppProperties appProperties;

    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        authController = new AuthController(
                authenticationManager,
                jwtService,
                refreshTokenService,
                userDetailsService,
                userRepository,
                appProperties,
                loginRateLimiter
        );
        lenient().when(loginRateLimiter.isBlocked(anyString(), anyString())).thenReturn(false);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    private UserDetails userDetails(String email) {
        return new org.springframework.security.core.userdetails.User(
                email, "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private User appUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(Role.ADMIN);
        return user;
    }

    private RefreshToken refreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        return rt;
    }

    @Test
    void login_withValidCredentials_returns200AndSetsEmail() throws Exception {
        String email = "admin@example.com";
        UserDetails ud = userDetails(email);
        User user = appUser(email);
        RefreshToken rt = refreshToken(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(ud)).thenReturn("access-token");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        String body = """
                {"email": "admin@example.com", "password": "admin"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void login_overHttp_setsInsecureCookiesWithLaxAndHttpOnly() throws Exception {
        String email = "admin@example.com";
        UserDetails ud = userDetails(email);
        User user = appUser(email);
        RefreshToken rt = refreshToken(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(ud)).thenReturn("access-token");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "admin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(2);
        String accessHeader = setCookies.stream().filter(h -> h.startsWith("access_token=")).findFirst().orElseThrow();
        assertThat(accessHeader).contains("Path=/");
        assertThat(accessHeader).contains("Max-Age=3600");
        assertThat(accessHeader).contains("HttpOnly");
        assertThat(accessHeader).contains("SameSite=Lax");
        assertThat(accessHeader).doesNotContain("Secure");

        String refreshHeader = setCookies.stream().filter(h -> h.startsWith("refresh_token=")).findFirst().orElseThrow();
        assertThat(refreshHeader).contains("Path=/api/auth");
        assertThat(refreshHeader).contains("Max-Age=604800");
        assertThat(refreshHeader).contains("HttpOnly");
        assertThat(refreshHeader).contains("SameSite=Lax");
        assertThat(refreshHeader).doesNotContain("Secure");
    }

    @Test
    void login_withCookieSecureOverrideTrue_forcesSecureOverHttp() throws Exception {
        appProperties.setCookieSecure(true);

        String email = "admin@example.com";
        UserDetails ud = userDetails(email);
        User user = appUser(email);
        RefreshToken rt = refreshToken(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(ud)).thenReturn("access-token");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "admin"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(2);
        for (String h : setCookies) {
            assertThat(h).contains("Secure");
        }
    }

    @Test
    void login_overHttps_setsSecureCookies() throws Exception {
        String email = "admin@example.com";
        UserDetails ud = userDetails(email);
        User user = appUser(email);
        RefreshToken rt = refreshToken(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(ud)).thenReturn("access-token");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "admin"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(2);
        for (String h : setCookies) {
            assertThat(h).contains("Secure");
            assertThat(h).contains("HttpOnly");
            assertThat(h).contains("SameSite=Lax");
        }
    }

    @Test
    void logout_clearsBothCookiesWithMaxAgeZero() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@example.com");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(post("/api/auth/logout").principal(auth).secure(true))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(2);
        for (String h : setCookies) {
            assertThat(h).contains("Max-Age=0");
            assertThat(h).contains("HttpOnly");
            assertThat(h).contains("SameSite=Lax");
            assertThat(h).contains("Secure");
            assertThat(h).contains("Path=/");
        }
    }

    @Test
    void refresh_overHttps_setsSecureCookies() throws Exception {
        String email = "admin@example.com";
        UserDetails ud = userDetails(email);
        User user = appUser(email);
        RefreshToken rotated = refreshToken(user);

        when(refreshTokenService.validateAndRotate("old-refresh")).thenReturn(Optional.of(rotated));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(ud);
        when(jwtService.generateToken(ud)).thenReturn("new-access");

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .secure(true)
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(2);
        for (String h : setCookies) {
            assertThat(h).contains("Secure");
            assertThat(h).contains("HttpOnly");
            assertThat(h).contains("SameSite=Lax");
        }
    }

    @Test
    void refresh_withoutCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        String body = """
                {"email": "admin@example.com", "password": "wrong"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_whenRateLimited_returns429AndSkipsAuthentication() throws Exception {
        when(loginRateLimiter.isBlocked(anyString(), eq("admin@example.com"))).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "whatever"}
                                """))
                .andExpect(status().isTooManyRequests());

        verify(authenticationManager, never()).authenticate(any());
        verify(loginRateLimiter, never()).recordFailure(anyString(), anyString());
    }

    @Test
    void login_withInvalidCredentials_recordsFailure() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized());

        verify(loginRateLimiter).recordFailure(anyString(), eq("admin@example.com"));
        verify(loginRateLimiter, never()).recordSuccess(anyString(), anyString());
    }

    @Test
    void login_withValidCredentials_recordsSuccess() throws Exception {
        String email = "admin@example.com";
        UserDetails ud = userDetails(email);
        User user = appUser(email);
        RefreshToken rt = refreshToken(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(ud)).thenReturn("access-token");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "admin"}
                                """))
                .andExpect(status().isOk());

        verify(loginRateLimiter).recordSuccess(anyString(), eq(email));
        verify(loginRateLimiter, never()).recordFailure(anyString(), anyString());
    }

    @Test
    void me_whenAuthenticated_returnsEmailAndRole() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin@example.com");
        when(auth.getAuthorities()).thenAnswer(_ ->
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get("/api/auth/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void me_whenNullAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_whenNotAuthenticated_returns401() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/api/auth/me").principal(auth))
                .andExpect(status().isUnauthorized());
    }
}
