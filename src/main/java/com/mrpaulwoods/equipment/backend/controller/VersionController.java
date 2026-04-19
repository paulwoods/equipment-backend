package com.mrpaulwoods.equipment.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/version")
@RequiredArgsConstructor
@Tag(name = "Version", description = "Application build information")
public class VersionController {

    private final BuildProperties buildProperties;

    @Operation(summary = "Return the current application version")
    @GetMapping
    public Map<String, String> getVersion() {
        return Map.of("version", buildProperties.getVersion());
    }
}
