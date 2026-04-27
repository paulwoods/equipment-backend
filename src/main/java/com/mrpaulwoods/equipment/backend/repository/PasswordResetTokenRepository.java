package com.mrpaulwoods.equipment.backend.repository;

import com.mrpaulwoods.equipment.backend.entity.PasswordResetToken;
import com.mrpaulwoods.equipment.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}
