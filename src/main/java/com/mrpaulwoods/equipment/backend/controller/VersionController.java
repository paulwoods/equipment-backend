package com.mrpaulwoods.equipment.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/version")
@RequiredArgsConstructor
public class VersionController {

    private final BuildProperties buildProperties;

    @GetMapping
    public Map<String, String> getVersion() {
        return Map.of("version", buildProperties.getVersion());
    }
}
