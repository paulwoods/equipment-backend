package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.PasswordResetToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.PasswordResetTokenRepository;
import com.mrpaulwoods.equipment.backend.util.TokenHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User sampleUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("oldhash");
        return user;
    }

    @Test
    void createResetToken_deletesExistingTokensAndReturnsNewToken() {
        User user = sampleUser();
        given(passwordResetTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String token = passwordResetService.createResetToken(user);

        then(passwordResetTokenRepository).should().deleteByUser(user);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        then(passwordResetTokenRepository).should().save(captor.capture());
        assertThat(token).isNotBlank();
        assertThat(captor.getValue().getToken())
                .isNotEqualTo(token)
                .isEqualTo(TokenHasher.sha256Hex(token));
    }

    @Test
    void createResetToken_setsExpiryToOneHour() {
        User user = sampleUser();
        given(passwordResetTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        passwordResetService.createResetToken(user);

        then(passwordResetTokenRepository).should().save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_withValidToken_encodesAndDeletesToken() {
        User user = sampleUser();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(TokenHasher.sha256Hex("valid-token"));
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        given(passwordResetTokenRepository.findByToken(TokenHasher.sha256Hex("valid-token"))).willReturn(Optional.of(token));
        given(passwordEncoder.encode("newpass")).willReturn("newhash");

        passwordResetService.resetPassword("valid-token", "newpass");

        assertThat(user.getPassword()).isEqualTo("newhash");
        then(passwordResetTokenRepository).should().delete(token);
    }

    @Test
    void resetPassword_withMissingToken_throwsBadRequest() {
        given(passwordResetTokenRepository.findByToken(TokenHasher.sha256Hex("missing"))).willReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("missing", "newpass"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());

        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    void resetPassword_withExpiredToken_throwsBadRequest() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(TokenHasher.sha256Hex("expired-token"));
        token.setUser(sampleUser());
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        given(passwordResetTokenRepository.findByToken(TokenHasher.sha256Hex("expired-token"))).willReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newpass"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());

        then(passwordEncoder).should(never()).encode(any());
        then(passwordResetTokenRepository).should(never()).delete(any());
    }
}
