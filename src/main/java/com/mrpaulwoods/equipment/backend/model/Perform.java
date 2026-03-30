package com.mrpaulwoods.equipment.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Perform {
    private String id;
    private LocalDate date;
    private String notes;
}
