package com.mrpaulwoods.equipment.backend.entity;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
public class Equipment extends UuidEntity {

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private String modelNumber;

    private String serialNumber;
    private String assetTag;
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EquipmentStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate purchaseDate;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Procedure> procedures = new ArrayList<>();
}
