package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.entity.RoleEntity;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.entity.UserRole;
import com.mrpaulwoods.equipment.backend.repository.RoleRepository;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import com.mrpaulwoods.equipment.backend.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private static final Set<String> ADMIN_MANAGEABLE_ROLES = Set.of("USER", "EDIT", "ADMIN");
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public User createInternal(String name, String email, String password, Set<String> roleNames) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(user);
        assignRoles(saved, roleNames);
        return saved;
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public UserResponse findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(this::notFound);
        return toResponse(user);
    }

    @Transactional
    public UserResponse create(UserRequest request, Set<String> callerRoleNames) {
        validateRoleNames(request.roles());
        for (String roleName : request.roles()) {
            assertCallerCanManageRole(callerRoleNames, roleName);
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw emailInUse();
        }
        User saved = createInternal(request.name(), request.email(), request.password(), request.roles());
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request, UUID currentUserId, Set<String> callerRoleNames) {
        if (id.equals(currentUserId) && !callerRoleNames.contains("SYSTEM_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify your own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(this::notFound);

        validateRoleNames(request.roles());
        for (String existingRole : getRoleNames(user)) {
            assertCallerCanManageRole(callerRoleNames, existingRole);
        }
        for (String requestedRole : request.roles()) {
            assertCallerCanManageRole(callerRoleNames, requestedRole);
        }

        if (id.equals(currentUserId) && !request.roles().contains("SYSTEM_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove your own SYSTEM_ADMIN role");
        }

        if (!user.getEmail().equals(request.email())) {
            userRepository.findByEmail(request.email()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw emailInUse();
                }
            });
        }

        user.setName(request.name());
        user.setEmail(request.email());
        updateRoles(user, request.roles());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse updateSelf(UUID currentUserId, UserSelfUpdateRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(this::notFound);

        if (!user.getEmail().equals(request.email())) {
            userRepository.findByEmail(request.email()).ifPresent(existing -> {
                if (!existing.getId().equals(currentUserId)) {
                    throw emailInUse();
                }
            });
        }

        user.setName(request.name());
        user.setEmail(request.email());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public void changePassword(UUID currentUserId, UserPasswordChangeRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(this::notFound);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenService.deleteByUser(user);
    }

    @Transactional
    public void bumpTokenVersion(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId, Set<String> callerRoleNames) {
        if (id.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete your own account");
        }
        User target = userRepository.findById(id)
                .orElseThrow(this::notFound);
        for (String roleName : getRoleNames(target)) {
            assertCallerCanManageRole(callerRoleNames, roleName);
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    public boolean isSetupRequired() {
        return userRepository.count() == 0;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private void assignRoles(User user, Set<String> roleNames) {
        for (String roleName : roleNames) {
            RoleEntity role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName));
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            user.getUserRoles().add(userRole);
        }
    }

    private void updateRoles(User user, Set<String> roleNames) {
        user.getUserRoles().clear();
        userRoleRepository.deleteByUserId(user.getId());
        assignRoles(user, roleNames);
    }

    private void validateRoleNames(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        for (String roleName : roleNames) {
            if (!roleRepository.findByName(roleName).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
            }
        }
    }

    private Set<String> getRoleNames(User user) {
        Set<String> names = new HashSet<>();
        for (UserRole ur : user.getUserRoles()) {
            names.add(ur.getRole().getName());
        }
        return names;
    }

    private Set<RoleResponse> toRoleResponses(User user) {
        Set<RoleResponse> responses = new HashSet<>();
        for (UserRole ur : user.getUserRoles()) {
            responses.add(new RoleResponse(ur.getRole().getId(), ur.getRole().getName()));
        }
        return responses;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), toRoleResponses(user));
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    private ResponseStatusException emailInUse() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
    }

    private void assertCallerCanManageRole(Set<String> callerRoleNames, String targetRoleName) {
        if (callerRoleNames.contains("SYSTEM_ADMIN")) {
            return;
        }
        if (callerRoleNames.contains("ADMIN") && ADMIN_MANAGEABLE_ROLES.contains(targetRoleName)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient privileges to manage role: " + targetRoleName);
    }
}
