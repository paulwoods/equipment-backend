package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.entity.RefreshToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.RefreshTokenRepository;
import com.mrpaulwoods.equipment.backend.util.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AppProperties appProperties;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRefreshTokenDays(7);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, appProperties);
    }

    private User sampleUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        return user;
    }

    // --- createRefreshToken ---

    @Test
    void createRefreshToken_onlyTheHashIsPersisted_rawTokenIsNotStored() {
        User user = sampleUser();
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should().save(captor.capture());
        RefreshToken persisted = captor.getValue();

        assertThat(persisted.getToken()).isNotEqualTo(issued.rawToken());
        assertThat(persisted.getToken()).isEqualTo(TokenHasher.sha256Hex(issued.rawToken()));
    }

    @Test
    void createRefreshToken_setsUserAndExpiryFromRefreshTokenDays() {
        appProperties.setRefreshTokenDays(3);
        User user = sampleUser();
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now().plus(3, ChronoUnit.DAYS);
        refreshTokenService.createRefreshToken(user);
        Instant after = Instant.now().plus(3, ChronoUnit.DAYS);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should().save(captor.capture());
        RefreshToken persisted = captor.getValue();

        assertThat(persisted.getUser()).isSameAs(user);
        assertThat(persisted.getExpiresAt()).isBetween(before.minusSeconds(2), after.plusSeconds(2));
    }

    @Test
    void createRefreshToken_rawTokenIsRandomEachTime() {
        User user = sampleUser();
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.IssuedRefreshToken first = refreshTokenService.createRefreshToken(user);
        RefreshTokenService.IssuedRefreshToken second = refreshTokenService.createRefreshToken(user);

        assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
    }

    // --- validateAndRotate ---

    @Test
    void validateAndRotate_validToken_deletesOldAndIssuesNewToken() {
        User user = sampleUser();
        String rawOldToken = "old-raw-token";
        RefreshToken oldToken = new RefreshToken();
        oldToken.setId(UUID.randomUUID());
        oldToken.setToken(TokenHasher.sha256Hex(rawOldToken));
        oldToken.setUser(user);
        oldToken.setExpiresAt(Instant.now().plusSeconds(60));

        given(refreshTokenRepository.findByToken(TokenHasher.sha256Hex(rawOldToken)))
                .willReturn(Optional.of(oldToken));
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Optional<RefreshTokenService.IssuedRefreshToken> rotated = refreshTokenService.validateAndRotate(rawOldToken);

        assertThat(rotated).isPresent();
        assertThat(rotated.get().rawToken()).isNotEqualTo(rawOldToken);
        then(refreshTokenRepository).should().delete(oldToken);
        then(refreshTokenRepository).should().save(any());
    }

    @Test
    void validateAndRotate_oldTokenIsInvalidatedBeforeNewOneIsIssued() {
        // Regression guard: rotation must delete the old row, not merely stop
        // returning it - otherwise the old raw token would remain redeemable.
        User user = sampleUser();
        String rawOldToken = "old-raw-token";
        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken(TokenHasher.sha256Hex(rawOldToken));
        oldToken.setUser(user);
        oldToken.setExpiresAt(Instant.now().plusSeconds(60));

        given(refreshTokenRepository.findByToken(TokenHasher.sha256Hex(rawOldToken)))
                .willReturn(Optional.of(oldToken));
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        refreshTokenService.validateAndRotate(rawOldToken);

        then(refreshTokenRepository).should().delete(oldToken);
    }

    @Test
    void validateAndRotate_expiredToken_returnsEmptyAndDoesNotRotate() {
        User user = sampleUser();
        String rawOldToken = "expired-raw-token";
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken(TokenHasher.sha256Hex(rawOldToken));
        expiredToken.setUser(user);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(1));

        given(refreshTokenRepository.findByToken(TokenHasher.sha256Hex(rawOldToken)))
                .willReturn(Optional.of(expiredToken));

        Optional<RefreshTokenService.IssuedRefreshToken> rotated = refreshTokenService.validateAndRotate(rawOldToken);

        assertThat(rotated).isEmpty();
        then(refreshTokenRepository).should(never()).delete(any());
        then(refreshTokenRepository).should(never()).save(any());
    }

    @Test
    void validateAndRotate_unknownToken_returnsEmpty() {
        given(refreshTokenRepository.findByToken(any())).willReturn(Optional.empty());

        Optional<RefreshTokenService.IssuedRefreshToken> rotated = refreshTokenService.validateAndRotate("unknown-raw-token");

        assertThat(rotated).isEmpty();
        then(refreshTokenRepository).should(never()).delete(any());
    }

    @Test
    void validateAndRotate_looksUpByHashOfRawToken_neverByRawValue() {
        given(refreshTokenRepository.findByToken(any())).willReturn(Optional.empty());

        refreshTokenService.validateAndRotate("some-raw-token");

        then(refreshTokenRepository).should().findByToken(TokenHasher.sha256Hex("some-raw-token"));
        then(refreshTokenRepository).should(never()).findByToken("some-raw-token");
    }

    // --- deleteByUser ---

    @Test
    void deleteByUser_delegatesToRepository() {
        User user = sampleUser();

        refreshTokenService.deleteByUser(user);

        then(refreshTokenRepository).should().deleteByUser(user);
    }
}
