package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;

    public String generateToken(UserDetails userDetails, long tokenVersion) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .claim("tv", tokenVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + appProperties.getJwtExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * The claims of a successfully verified, unexpired token.
     * {@code tokenVersion} is null when the token predates the "tv" claim.
     */
    public record ValidToken(String email, Long tokenVersion) {
    }

    public Optional<ValidToken> validate(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!claims.getExpiration().after(new Date())) {
                return Optional.empty();
            }
            Long tokenVersion = claims.get("tv") instanceof Number n ? n.longValue() : null;
            return Optional.of(new ValidToken(claims.getSubject(), tokenVersion));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = appProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
