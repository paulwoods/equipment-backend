package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.entity.User;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new user account")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping
    public ResponseEntity<UserCreateResponse> create(@Valid @RequestBody UserRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request, callerRoles(authentication)));
    }

    @Operation(summary = "List all users (paginated)")
    @GetMapping
    public ResponseEntity<Page<UserListResponse>> findAll(
            @PageableDefault(size = 20, sort = "email", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Update a user account")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserUpdateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        UUID currentUserId = currentUserId(authentication);
        return ResponseEntity.ok(userService.update(id, request, currentUserId, callerRoles(authentication)));
    }

    @Operation(summary = "Delete a user account")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        UUID currentUserId = currentUserId(authentication);
        userService.delete(id, currentUserId, callerRoles(authentication));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update own name and email")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<UserSelfUpdateResponse> updateMe(
            @Valid @RequestBody UserSelfUpdateRequest request,
            Authentication authentication) {
        UUID currentUserId = currentUserId(authentication);
        return ResponseEntity.ok(userService.updateSelf(currentUserId, request));
    }

    @Operation(summary = "Change own password")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody UserPasswordChangeRequest request,
            Authentication authentication) {
        UUID currentUserId = currentUserId(authentication);
        userService.changePassword(currentUserId, request);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Set<String> callerRoles(Authentication authentication) {
        Set<String> roles = new HashSet<>();
        authentication.getAuthorities().forEach(a -> {
            String authority = a.getAuthority();
            if (authority.startsWith("ROLE_")) {
                roles.add(authority.substring(5));
            }
        });
        return roles;
    }
}
