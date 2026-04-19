package com.mrpaulwoods.equipment.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "perform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Perform {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_id", nullable = false)
    @JsonIgnore
    private Procedure procedure;

    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
