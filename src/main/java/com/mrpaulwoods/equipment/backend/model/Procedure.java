package com.mrpaulwoods.equipment.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Procedure {
    private String id;
    private String name;
    private String description;
    private String steps;
    private String requiredTools;
    private int intervalDays;
    private List<Perform> history = new ArrayList<>();
}
