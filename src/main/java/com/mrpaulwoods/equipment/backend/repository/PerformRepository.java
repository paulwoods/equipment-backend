package com.mrpaulwoods.equipment.backend.repository;

import com.mrpaulwoods.equipment.backend.model.Perform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PerformRepository extends JpaRepository<Perform, UUID> {
}
