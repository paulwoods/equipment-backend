package com.mrpaulwoods.equipment.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procedure")
@Getter
@Setter
@NoArgsConstructor
public class Procedure extends UuidEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    @JsonIgnore
    private Equipment equipment;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @Column(columnDefinition = "TEXT")
    private String requiredTools;

    @Column(nullable = false)
    private int intervalDays;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Perform> history = new ArrayList<>();
}
