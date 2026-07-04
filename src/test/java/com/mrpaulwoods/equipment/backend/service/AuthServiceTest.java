package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.UserResponse;
import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.ratelimit.ForgotPasswordRateLimiterService;
import com.mrpaulwoods.equipment.backend.ratelimit.LoginRateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserService userService;

    @Mock
    private LoginRateLimiterService loginRateLimiter;

    @Mock
    private ForgotPasswordRateLimiterService forgotPasswordRateLimiter;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        authService = new AuthService(
                authenticationManager,
                jwtService,
                refreshTokenService,
                userDetailsService,
                userService,
                loginRateLimiter,
                forgotPasswordRateLimiter,
                passwordResetService,
                emailService,
                passwordEncoder
        );
    }

    private UserDetails userDetails(String email) {
        return new org.springframework.security.core.userdetails.User(
                email, "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private User appUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        return user;
    }

    private RefreshTokenService.IssuedRefreshToken refreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken("hashed-token");
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        return new RefreshTokenService.IssuedRefreshToken("raw-refresh", rt);
    }

    @Test
    void login_withValidCredentials_returnsTokensAndRecordsSuccess() {
        String email = "admin@example.com";
        UserDetails ud = userDetails(email);
        User user = appUser(email);
        user.setTokenVersion(3L);

        when(loginRateLimiter.isBlocked("1.2.3.4", email)).thenReturn(false);
        when(userService.findByEmail(email)).thenReturn(Optional.of(user));
        Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(ud, 3L)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken(user));

        AuthService.IssuedTokens tokens = authService.login(email, "password123", "1.2.3.4");

        assertThat(tokens.email()).isEqualTo(email);
        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.rawRefreshToken()).isEqualTo("raw-refresh");
        verify(loginRateLimiter).recordSuccess("1.2.3.4", email);
        verify(loginRateLimiter, never()).recordFailure(anyString(), anyString());
    }

    @Test
    void login_whenUserNotFound_throws401AndPerformsDummyBcryptToEqualizeTiming() {
        when(loginRateLimiter.isBlocked("1.2.3.4", "ghost@example.com")).thenReturn(false);
        when(userService.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@example.com", "anypass12", "1.2.3.4"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(passwordEncoder).matches("anypass12", "dummy-hash");
        verify(authenticationManager, never()).authenticate(any());
        verify(loginRateLimiter).recordFailure("1.2.3.4", "ghost@example.com");
    }

    @Test
    void login_withInvalidCredentials_throws401AndRecordsFailure() {
        String email = "admin@example.com";
        when(loginRateLimiter.isBlocked("1.2.3.4", email)).thenReturn(false);
        when(userService.findByEmail(email)).thenReturn(Optional.of(appUser(email)));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(email, "wrongpass", "1.2.3.4"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(loginRateLimiter).recordFailure("1.2.3.4", email);
        verify(loginRateLimiter, never()).recordSuccess(anyString(), anyString());
    }

    @Test
    void login_whenRateLimited_throws429AndSkipsAuthentication() {
        when(loginRateLimiter.isBlocked("1.2.3.4", "admin@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("admin@example.com", "whatever12", "1.2.3.4"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(authenticationManager, never()).authenticate(any());
        verify(loginRateLimiter, never()).recordFailure(anyString(), anyString());
    }

    @Test
    void logout_withKnownEmail_revokesRefreshTokensAndBumpsTokenVersion() {
        User user = appUser("admin@example.com");
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        authService.logout("admin@example.com");

        verify(refreshTokenService).deleteByUser(user);
        verify(userService).bumpTokenVersion(user);
    }

    @Test
    void logout_withNullEmail_doesNothing() {
        authService.logout(null);

        verifyNoInteractions(refreshTokenService);
        verify(userService, never()).bumpTokenVersion(any());
    }

    @Test
    void logout_withUnknownEmail_doesNotRevoke() {
        when(userService.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        authService.logout("ghost@example.com");

        verify(refreshTokenService, never()).deleteByUser(any());
        verify(userService, never()).bumpTokenVersion(any());
    }

    @Test
    void refresh_withValidToken_rotatesAndReturnsNewTokens() {
        String email = "admin@example.com";
        User user = appUser(email);
        user.setTokenVersion(2L);
        UserDetails ud = userDetails(email);
        RefreshTokenService.IssuedRefreshToken rotated = refreshToken(user);

        when(refreshTokenService.validateAndRotate("old-refresh")).thenReturn(Optional.of(rotated));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(ud);
        when(jwtService.generateToken(ud, 2L)).thenReturn("new-access");

        AuthService.IssuedTokens tokens = authService.refresh("old-refresh");

        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.rawRefreshToken()).isEqualTo("raw-refresh");
    }

    @Test
    void refresh_withNullToken_throws401() {
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_withInvalidToken_throws401() {
        when(refreshTokenService.validateAndRotate("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void forgotPassword_withExistingEmail_sendsEmail() throws Exception {
        User user = appUser("admin@example.com");
        when(forgotPasswordRateLimiter.isBlocked("1.2.3.4")).thenReturn(false);
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordResetService.createResetToken(user)).thenReturn("reset-token");

        authService.forgotPassword("admin@example.com", "1.2.3.4");

        verify(forgotPasswordRateLimiter).recordRequest("1.2.3.4");
        verify(emailService).sendPasswordResetEmail("admin@example.com", "reset-token");
    }

    @Test
    void forgotPassword_withUnknownEmail_doesNotSendEmail() throws Exception {
        when(forgotPasswordRateLimiter.isBlocked("1.2.3.4")).thenReturn(false);
        when(userService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword("unknown@example.com", "1.2.3.4");

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_whenRateLimited_throws429() {
        when(forgotPasswordRateLimiter.isBlocked("1.2.3.4")).thenReturn(true);

        assertThatThrownBy(() -> authService.forgotPassword("admin@example.com", "1.2.3.4"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(forgotPasswordRateLimiter, never()).recordRequest(anyString());
    }

    @Test
    void forgotPassword_whenEmailSendFails_doesNotPropagate() throws Exception {
        User user = appUser("admin@example.com");
        when(forgotPasswordRateLimiter.isBlocked("1.2.3.4")).thenReturn(false);
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordResetService.createResetToken(user)).thenReturn("reset-token");
        doThrow(new RuntimeException("smtp down")).when(emailService).sendPasswordResetEmail(anyString(), anyString());

        authService.forgotPassword("admin@example.com", "1.2.3.4");
    }

    @Test
    void resetPassword_delegatesToPasswordResetService() {
        authService.resetPassword("token", "newPassword123");

        verify(passwordResetService).resetPassword("token", "newPassword123");
    }

    @Test
    void currentUser_whenFound_returnsResponse() {
        User user = appUser("admin@example.com");
        user.setName("Admin");
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.currentUser("admin@example.com");

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.name()).isEqualTo("Admin");
        assertThat(response.email()).isEqualTo("admin@example.com");
        assertThat(response.roles()).isEmpty();
    }

    @Test
    void currentUser_whenNotFound_throws404() {
        when(userService.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.currentUser("ghost@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
