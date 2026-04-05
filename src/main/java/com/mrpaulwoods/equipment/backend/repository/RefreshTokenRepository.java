package com.mrpaulwoods.equipment.backend.repository;

import com.mrpaulwoods.equipment.backend.model.RefreshToken;
import com.mrpaulwoods.equipment.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
