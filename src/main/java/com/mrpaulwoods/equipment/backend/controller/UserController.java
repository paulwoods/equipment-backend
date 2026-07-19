package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.service.CallerContext;
import com.mrpaulwoods.equipment.backend.service.RoleTier;
import com.mrpaulwoods.equipment.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new user account")
    @PreAuthorize(RoleTier.MANAGE_USERS)
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request, CallerContext caller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request, caller));
    }

    @Operation(summary = "List all users (paginated)")
    @PreAuthorize(RoleTier.MANAGE_USERS)
    @GetMapping
    public ResponseEntity<Page<UserResponse>> findAll(
            @PageableDefault(size = 20, sort = "email", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @Operation(summary = "Get user by ID")
    @PreAuthorize(RoleTier.MANAGE_USERS)
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Update a user account")
    @PreAuthorize(RoleTier.MANAGE_USERS)
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request,
            CallerContext caller) {
        return ResponseEntity.ok(userService.update(id, request, caller));
    }

    @Operation(summary = "Delete a user account")
    @PreAuthorize(RoleTier.MANAGE_USERS)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, CallerContext caller) {
        userService.delete(id, caller);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update own name and email")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UserSelfUpdateRequest request,
            CallerContext caller) {
        return ResponseEntity.ok(userService.updateSelf(caller.userId(), request));
    }

    @Operation(summary = "Change own password")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody UserPasswordChangeRequest request,
            CallerContext caller) {
        userService.changePassword(caller.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
