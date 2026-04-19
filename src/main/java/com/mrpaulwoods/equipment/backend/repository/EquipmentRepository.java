package com.mrpaulwoods.equipment.backend.repository;

import com.mrpaulwoods.equipment.backend.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    @Query("SELECT DISTINCT e FROM Equipment e LEFT JOIN FETCH e.procedures")
    List<Equipment> findAllWithProcedures();
}
