package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.dto.GoogleConfigResponse;
import com.mrpaulwoods.equipment.backend.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

/**
 * Google sign-in. The browser obtains an ID token from Google Identity Services
 * and posts it here; once verified, the caller receives exactly the same access
 * and refresh tokens a password login would produce, so everything downstream —
 * the JWT filter, refresh rotation, logout — is unchanged.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleAuthService {

    /** Auto-provisioned accounts start at the lowest tier; an admin promotes from there. */
    private static final Set<String> DEFAULT_ROLES = Set.of("USER");

    private final JwtDecoder googleIdTokenDecoder;
    private final AppProperties appProperties;
    private final UserService userService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AdminBootstrap adminBootstrap;

    public GoogleConfigResponse config() {
        return isEnabled()
                ? new GoogleConfigResponse(true, appProperties.getGoogleClientId())
                : new GoogleConfigResponse(false, null);
    }

    public AuthService.IssuedTokens login(String credential) {
        if (!isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured");
        }

        Jwt idToken = decode(credential);
        String googleSub = idToken.getSubject();
        String email = idToken.getClaimAsString("email");

        if (googleSub == null || googleSub.isBlank() || email == null || email.isBlank()) {
            throw unauthorized("Google account is missing a subject or email");
        }
        // An unverified address could belong to anyone, so honouring it would let a
        // Google account claim — or create — an account for a mailbox it does not own.
        if (!isEmailVerified(idToken)) {
            throw unauthorized("Google account email is not verified");
        }

        User user = resolveUser(googleSub, email, idToken.getClaimAsString("name"));

        UserDetails userDetails = userDetailsService.toUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails, user.getTokenVersion());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthService.IssuedTokens(user.getEmail(), accessToken, refreshToken.rawToken());
    }

    private boolean isEnabled() {
        String clientId = appProperties.getGoogleClientId();
        return clientId != null && !clientId.isBlank();
    }

    private Jwt decode(String credential) {
        try {
            return googleIdTokenDecoder.decode(credential);
        } catch (JwtException e) {
            log.warn("Rejected Google ID token: {}", e.getMessage());
            throw unauthorized("Invalid Google credential");
        }
    }

    /** Google sends {@code email_verified} as a boolean, but tolerate the string form. */
    private boolean isEmailVerified(Jwt idToken) {
        Object claim = idToken.getClaim("email_verified");
        return Boolean.TRUE.equals(claim) || "true".equals(claim);
    }

    private User resolveUser(String googleSub, String email, String name) {
        Optional<User> bySub = userService.findByGoogleSub(googleSub);
        if (bySub.isPresent()) {
            return bySub.get();
        }

        Optional<User> byEmail = userService.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            // The email is already tied to a different Google account. Silently
            // re-pointing it would hand one Google identity another's account.
            if (existing.getGoogleSub() != null && !existing.getGoogleSub().equals(googleSub)) {
                throw unauthorized("This email is linked to a different Google account");
            }
            return userService.linkGoogleAccount(existing, googleSub);
        }

        return provision(googleSub, email, name);
    }

    private User provision(String googleSub, String email, String name) {
        // Auto-provisioning before first-run setup would leave the system with users
        // but no administrator, and would let a passer-by take the first account.
        if (adminBootstrap.isSetupRequired()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete initial setup before signing in with Google");
        }

        try {
            return userService.createFromGoogle(
                    name == null || name.isBlank() ? email : name, email, googleSub, DEFAULT_ROLES);
        } catch (DataIntegrityViolationException e) {
            // Two sign-ins for a brand-new account raced; one insert won, so use it.
            log.debug("Concurrent Google provisioning for {}, reusing the winning row", email);
            return userService.findByGoogleSub(googleSub)
                    .or(() -> userService.findByEmail(email))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the account"));
        }
    }

    private ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }
}
