package com.mrpaulwoods.equipment.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "equipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    @Id
    private UUID id;

    private String manufacturer;
    private String modelNumber;
    private String serialNumber;
    private String assetTag;
    private String location;

    @Enumerated(EnumType.STRING)
    private EquipmentStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate purchaseDate;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Procedure> procedures = new ArrayList<>();

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
