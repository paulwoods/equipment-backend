package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.RefreshTokenRepository;
import com.mrpaulwoods.equipment.backend.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Pairs the raw token value (sent to the client, never persisted) with the
     * stored entity, whose {@code token} column holds only the SHA-256 hash.
     */
    public record IssuedRefreshToken(String rawToken, RefreshToken refreshToken) {
    }

    @Transactional
    public IssuedRefreshToken createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(TokenHasher.sha256Hex(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS));
        return new IssuedRefreshToken(rawToken, refreshTokenRepository.save(refreshToken));
    }

    @Transactional
    public Optional<IssuedRefreshToken> validateAndRotate(String rawToken) {
        return refreshTokenRepository.findByToken(TokenHasher.sha256Hex(rawToken))
                .filter(rt -> rt.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(rt -> {
                    refreshTokenRepository.delete(rt);
                    return createRefreshToken(rt.getUser());
                });
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
