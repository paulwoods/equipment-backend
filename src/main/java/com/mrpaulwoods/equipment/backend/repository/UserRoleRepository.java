package com.mrpaulwoods.equipment.backend.repository;

import com.mrpaulwoods.equipment.backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
