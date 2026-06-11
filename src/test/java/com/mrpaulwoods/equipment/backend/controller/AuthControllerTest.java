package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.dto.UserResponse;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.filter.JwtAuthFilter;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import com.mrpaulwoods.equipment.backend.service.AuthService;
import com.mrpaulwoods.equipment.backend.service.CookieService;
import com.mrpaulwoods.equipment.backend.service.JwtService;
import com.mrpaulwoods.equipment.backend.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private AuthService authService;

    private AppProperties appProperties;

    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        authController = new AuthController(authService, new CookieService(appProperties));
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    private AuthService.IssuedTokens issuedTokens(String email) {
        return new AuthService.IssuedTokens(email, "access-token", "raw-refresh");
    }

    @Test
    void login_withValidCredentials_returns200AndSetsEmail() throws Exception {
        String email = "admin@example.com";
        when(authService.login(eq(email), eq("password123"), anyString())).thenReturn(issuedTokens(email));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void login_overHttp_setsInsecureCookiesWithLaxAndHttpOnly() throws Exception {
        String email = "admin@example.com";
        when(authService.login(eq(email), anyString(), anyString())).thenReturn(issuedTokens(email));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123"}
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
        assertThat(refreshHeader).contains("Path=/api/v1/auth");
        assertThat(refreshHeader).contains("Max-Age=604800");
        assertThat(refreshHeader).contains("HttpOnly");
        assertThat(refreshHeader).contains("SameSite=Lax");
        assertThat(refreshHeader).doesNotContain("Secure");
    }

    @Test
    void login_withCookieSecureOverrideTrue_forcesSecureOverHttp() throws Exception {
        appProperties.setCookieSecure(true);
        String email = "admin@example.com";
        when(authService.login(eq(email), anyString(), anyString())).thenReturn(issuedTokens(email));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123"}
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
        when(authService.login(eq(email), anyString(), anyString())).thenReturn(issuedTokens(email));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123"}
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
    void login_withInvalidCredentials_returns401() throws Exception {
        when(authService.login(eq("admin@example.com"), anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "wrongpass"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_whenRateLimited_returns429() throws Exception {
        when(authService.login(eq("admin@example.com"), anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "whatever12"}
                                """))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void logout_clearsBothCookiesWithMaxAgeZero() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@example.com");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout").principal(auth).secure(true))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(2);
        for (String h : setCookies) {
            assertThat(h).contains("Max-Age=0");
            assertThat(h).contains("HttpOnly");
            assertThat(h).contains("SameSite=Lax");
            assertThat(h).contains("Secure");
        }
        String accessHeader = setCookies.stream().filter(h -> h.startsWith("access_token=")).findFirst().orElseThrow();
        assertThat(accessHeader).contains("Path=/");
        String refreshHeader = setCookies.stream().filter(h -> h.startsWith("refresh_token=")).findFirst().orElseThrow();
        assertThat(refreshHeader).contains("Path=/api/v1/auth");

        verify(authService).logout("admin@example.com");
    }

    @Test
    void logout_withoutAuthentication_stillClearsCookiesAndReturns200() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(2);
        verify(authService).logout(null);
    }

    @Test
    void logout_withAccessTokenCookie_runsJwtFilterAndDelegatesLogout() throws Exception {
        // Exercises the real JwtAuthFilter on /logout: a regression that re-adds the
        // path to shouldNotFilter would leave Authentication null and fail this test.
        JwtService jwtService = mock(JwtService.class);
        UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
        UserRepository userRepository = mock(UserRepository.class);
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(
                jwtService, userDetailsService, new CookieService(appProperties), userRepository);
        SecurityContextHolderAwareRequestFilter securityContextFilter = new SecurityContextHolderAwareRequestFilter();
        securityContextFilter.afterPropertiesSet();
        MockMvc filteredMockMvc = MockMvcBuilders.standaloneSetup(authController)
                .addFilters(jwtAuthFilter, securityContextFilter)
                .build();

        String email = "admin@example.com";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setTokenVersion(5L);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(jwtService.validate("valid-jwt")).thenReturn(Optional.of(new JwtService.ValidToken(email, 5L)));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userDetailsService.toUserDetails(user)).thenReturn(userDetails);

        try {
            filteredMockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(new jakarta.servlet.http.Cookie("access_token", "valid-jwt")))
                    .andExpect(status().isOk());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(authService).logout(email);
    }

    @Test
    void refresh_overHttps_setsSecureCookies() throws Exception {
        when(authService.refresh("old-refresh")).thenReturn(issuedTokens("admin@example.com"));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
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
        when(authService.refresh(null))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_whenAuthenticated_returnsEmailAndRoles() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin@example.com");

        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(authService.currentUser("admin@example.com"))
                .thenReturn(new UserResponse(id, "Admin", "admin@example.com", Set.of()));

        mockMvc.perform(get("/api/v1/auth/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void me_whenNullAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_whenNotAuthenticated_returns401() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/me").principal(auth))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_whenUserNotFound_returns404() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin@example.com");
        when(authService.currentUser("admin@example.com"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/v1/auth/me").principal(auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void forgotPassword_returns200WithGenericMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the email exists in our system, you will receive reset instructions shortly."));

        verify(authService).forgotPassword(eq("admin@example.com"), anyString());
    }

    @Test
    void forgotPassword_whenRateLimited_returns429() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts"))
                .when(authService).forgotPassword(eq("admin@example.com"), anyString());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com"}
                                """))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void resetPassword_withValidToken_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "valid-token", "newPassword": "newpassword123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated"));

        verify(authService).resetPassword("valid-token", "newpassword123");
    }

    @Test
    void resetPassword_withInvalidToken_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"))
                .when(authService).resetPassword(eq("invalid-token"), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "invalid-token", "newPassword": "newpassword123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
