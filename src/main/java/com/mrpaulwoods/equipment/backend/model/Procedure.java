package com.mrpaulwoods.equipment.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "procedure")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Procedure {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    @JsonIgnore
    private Equipment equipment;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @Column(columnDefinition = "TEXT")
    private String requiredTools;

    private int intervalDays;

    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Perform> history = new ArrayList<>();

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
