package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import com.mrpaulwoods.equipment.backend.util.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<Role> ADMIN_MANAGEABLE_ROLES = Set.of(Role.USER, Role.EDIT);

    @Transactional
    public User createInternal(String name, String email, String password, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    public Page<UserListResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(u -> new UserListResponse(u.getId(), u.getName(), u.getEmail(), u.getRole()));
    }

    public UserDetailResponse findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new UserDetailResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Transactional
    public UserCreateResponse create(UserRequest request, Role callerRole) {
        assertCallerCanManageRole(callerRole, request.role());
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        User saved = createInternal(request.name(), request.email(), request.password(), request.role());
        return new UserCreateResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public UserUpdateResponse update(UUID id, UserUpdateRequest request, UUID currentUserId, Role callerRole) {
        if (id.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify your own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        assertCallerCanManageRole(callerRole, user.getRole());
        assertCallerCanManageRole(callerRole, request.role());

        if (!user.getEmail().equals(request.email())) {
            userRepository.findByEmail(request.email()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                }
            });
        }

        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        User saved = userRepository.save(user);
        return new UserUpdateResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public UserSelfUpdateResponse updateSelf(UUID currentUserId, UserSelfUpdateRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getEmail().equals(request.email())) {
            userRepository.findByEmail(request.email()).ifPresent(existing -> {
                if (!existing.getId().equals(currentUserId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                }
            });
        }

        user.setName(request.name());
        user.setEmail(request.email());
        User saved = userRepository.save(user);
        return new UserSelfUpdateResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public void changePassword(UUID currentUserId, UserPasswordChangeRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId, Role callerRole) {
        if (id.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete your own account");
        }
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        assertCallerCanManageRole(callerRole, target.getRole());
        userRepository.deleteById(id);
    }

    private void assertCallerCanManageRole(Role callerRole, Role targetRole) {
        if (callerRole == Role.SYSTEM_ADMIN) {
            return;
        }
        if (callerRole == Role.ADMIN && ADMIN_MANAGEABLE_ROLES.contains(targetRole)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient privileges to manage this role");
    }

    public boolean isSetupRequired() {
        return userRepository.count() == 0;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
