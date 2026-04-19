package com.mrpaulwoods.equipment.backend.repository;

import com.mrpaulwoods.equipment.backend.entity.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProcedureRepository extends JpaRepository<Procedure, UUID> {

    @Query("SELECT DISTINCT p FROM Procedure p LEFT JOIN FETCH p.history")
    List<Procedure> findAllWithHistory();
}
