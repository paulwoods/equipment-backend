package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.dto.GoogleConfigResponse;
import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String CREDENTIAL = "a.google.id-token";
    private static final String GOOGLE_SUB = "104738291047382910473";
    private static final String EMAIL = "someone@example.com";

    @Mock
    private JwtDecoder googleIdTokenDecoder;

    @Mock
    private UserService userService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AdminBootstrap adminBootstrap;

    private AppProperties appProperties;

    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setGoogleClientId("our-app.apps.googleusercontent.com");
        googleAuthService = new GoogleAuthService(
                googleIdTokenDecoder,
                appProperties,
                userService,
                userDetailsService,
                jwtService,
                refreshTokenService,
                adminBootstrap
        );
    }

    private Jwt idToken(String sub, String email, Object emailVerified, String name) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue(CREDENTIAL)
                .header("alg", "RS256")
                .claim("iss", "https://accounts.google.com")
                .audience(List.of(appProperties.getGoogleClientId()))
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim("email_verified", emailVerified);
        if (sub != null) {
            builder.subject(sub);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        if (name != null) {
            builder.claim("name", name);
        }
        return builder.build();
    }

    private Jwt verifiedIdToken() {
        return idToken(GOOGLE_SUB, EMAIL, true, "Some One");
    }

    private User user(String email, String googleSub) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Some One");
        user.setEmail(email);
        user.setGoogleSub(googleSub);
        return user;
    }

    /** Stubs the token minting shared by every successful-login assertion. */
    private void stubTokenIssuance() {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                EMAIL, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        lenient().when(userDetailsService.toUserDetails(any(User.class))).thenReturn(userDetails);
        lenient().when(jwtService.generateToken(eq(userDetails), anyLong())).thenReturn("access-token");
        lenient().when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("raw-refresh", new RefreshToken()));
    }

    @Test
    void config_whenClientIdIsSet_reportsEnabledWithTheClientId() {
        GoogleConfigResponse config = googleAuthService.config();

        assertThat(config.enabled()).isTrue();
        assertThat(config.clientId()).isEqualTo("our-app.apps.googleusercontent.com");
    }

    @Test
    void config_whenClientIdIsBlank_reportsDisabledAndWithholdsTheClientId() {
        appProperties.setGoogleClientId("  ");

        GoogleConfigResponse config = googleAuthService.config();

        assertThat(config.enabled()).isFalse();
        assertThat(config.clientId()).isNull();
    }

    @Test
    void login_whenGoogleSignInIsNotConfigured_returns503AndNeverDecodes() {
        appProperties.setGoogleClientId("");

        assertThatThrownBy(() -> googleAuthService.login(CREDENTIAL))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(googleIdTokenDecoder, never()).decode(anyString());
    }

    @Test
    void login_withAnUnverifiableToken_returns401() {
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenThrow(new BadJwtException("bad signature"));

        assertThatThrownBy(() -> googleAuthService.login(CREDENTIAL))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_withAnUnverifiedEmail_returns401AndProvisionsNothing() {
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(idToken(GOOGLE_SUB, EMAIL, false, "Some One"));

        assertThatThrownBy(() -> googleAuthService.login(CREDENTIAL))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(userService, never()).createFromGoogle(anyString(), anyString(), anyString(), any());
    }

    @Test
    void login_withNoEmailClaim_returns401() {
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(idToken(GOOGLE_SUB, null, true, "Some One"));

        assertThatThrownBy(() -> googleAuthService.login(CREDENTIAL))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_forAKnownGoogleSub_issuesTokensWithoutTouchingEmailLookup() {
        User existing = user(EMAIL, GOOGLE_SUB);
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(verifiedIdToken());
        when(userService.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.of(existing));
        stubTokenIssuance();

        AuthService.IssuedTokens tokens = googleAuthService.login(CREDENTIAL);

        assertThat(tokens.email()).isEqualTo(EMAIL);
        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.rawRefreshToken()).isEqualTo("raw-refresh");
        verify(userService, never()).findByEmail(anyString());
        verify(userService, never()).createFromGoogle(anyString(), anyString(), anyString(), any());
    }

    @Test
    void login_forAnExistingPasswordAccount_linksTheGoogleSubRatherThanCreatingADuplicate() {
        User existing = user(EMAIL, null);
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(verifiedIdToken());
        when(userService.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(userService.linkGoogleAccount(existing, GOOGLE_SUB)).thenReturn(existing);
        stubTokenIssuance();

        AuthService.IssuedTokens tokens = googleAuthService.login(CREDENTIAL);

        assertThat(tokens.email()).isEqualTo(EMAIL);
        verify(userService).linkGoogleAccount(existing, GOOGLE_SUB);
        verify(userService, never()).createFromGoogle(anyString(), anyString(), anyString(), any());
    }

    @Test
    void login_whenTheEmailIsHeldByADifferentGoogleAccount_returns401AndDoesNotRelink() {
        User existing = user(EMAIL, "a-different-google-sub");
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(verifiedIdToken());
        when(userService.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> googleAuthService.login(CREDENTIAL))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(userService, never()).linkGoogleAccount(any(), anyString());
    }

    @Test
    void login_forAnUnknownEmail_provisionsAUserAccount() {
        User created = user(EMAIL, GOOGLE_SUB);
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(verifiedIdToken());
        when(userService.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(adminBootstrap.isSetupRequired()).thenReturn(false);
        when(userService.createFromGoogle("Some One", EMAIL, GOOGLE_SUB, Set.of("USER"))).thenReturn(created);
        stubTokenIssuance();

        AuthService.IssuedTokens tokens = googleAuthService.login(CREDENTIAL);

        assertThat(tokens.email()).isEqualTo(EMAIL);
        verify(userService).createFromGoogle("Some One", EMAIL, GOOGLE_SUB, Set.of("USER"));
    }

    @Test
    void login_forAnUnknownEmailWithNoNameClaim_fallsBackToTheEmailAsTheName() {
        User created = user(EMAIL, GOOGLE_SUB);
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(idToken(GOOGLE_SUB, EMAIL, true, null));
        when(userService.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(adminBootstrap.isSetupRequired()).thenReturn(false);
        when(userService.createFromGoogle(EMAIL, EMAIL, GOOGLE_SUB, Set.of("USER"))).thenReturn(created);
        stubTokenIssuance();

        googleAuthService.login(CREDENTIAL);

        verify(userService).createFromGoogle(EMAIL, EMAIL, GOOGLE_SUB, Set.of("USER"));
    }

    @Test
    void login_beforeFirstRunSetup_refusesToProvisionSoTheSystemNeverLacksAnAdmin() {
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(verifiedIdToken());
        when(userService.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(adminBootstrap.isSetupRequired()).thenReturn(true);

        assertThatThrownBy(() -> googleAuthService.login(CREDENTIAL))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(userService, never()).createFromGoogle(anyString(), anyString(), anyString(), any());
    }

    @Test
    void login_whenTwoFirstSignInsRace_reusesTheRowTheWinningInsertCreated() {
        User winner = user(EMAIL, GOOGLE_SUB);
        when(googleIdTokenDecoder.decode(CREDENTIAL)).thenReturn(verifiedIdToken());
        when(userService.findByGoogleSub(GOOGLE_SUB))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(adminBootstrap.isSetupRequired()).thenReturn(false);
        when(userService.createFromGoogle(anyString(), anyString(), anyString(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));
        stubTokenIssuance();

        AuthService.IssuedTokens tokens = googleAuthService.login(CREDENTIAL);

        assertThat(tokens.email()).isEqualTo(EMAIL);
    }
}
