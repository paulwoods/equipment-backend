package com.mrpaulwoods.equipment.backend.repository;

import com.mrpaulwoods.equipment.backend.entity.TokenEntity;
import com.mrpaulwoods.equipment.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenRepository<T extends TokenEntity> extends JpaRepository<T, UUID> {
    Optional<T> findByToken(String token);

    void deleteByUser(User user);
}
