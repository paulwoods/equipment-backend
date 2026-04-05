package com.mrpaulwoods.equipment.backend.repository;

import com.mrpaulwoods.equipment.backend.model.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcedureRepository extends JpaRepository<Procedure, UUID> {
}
