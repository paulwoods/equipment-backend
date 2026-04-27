package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.PasswordResetToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String createResetToken(User user) {
        passwordResetTokenRepository.deleteByUser(user);

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        passwordResetTokenRepository.delete(resetToken);
    }
}
