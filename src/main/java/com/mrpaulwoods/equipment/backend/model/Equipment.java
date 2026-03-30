package com.mrpaulwoods.equipment.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    private String id;
    private String manufacturer;
    private String modelNumber;
    private String serialNumber;
    private String assetTag;
    private String location;
    private EquipmentStatus status;
    private String description;
    private LocalDate purchaseDate;
    private List<Procedure> procedures = new ArrayList<>();
}
