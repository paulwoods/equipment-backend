package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.entity.RoleEntity;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.entity.UserRole;
import com.mrpaulwoods.equipment.backend.repository.RoleRepository;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import com.mrpaulwoods.equipment.backend.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    private User sampleUser(UUID id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashed");
        return user;
    }

    private RoleEntity roleEntity(String name) {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setName(name);
        return role;
    }

    private void mockRole(String name) {
        given(roleRepository.findByName(name)).willReturn(Optional.of(roleEntity(name)));
    }

    // --- create ---

    @Test
    void create_systemAdminCreatingAnyRole_succeeds() {
        UUID id = UUID.randomUUID();
        var request = new UserRequest("Alice", "alice@example.com", "secret", Set.of("ADMIN"));

        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });
        mockRole("ADMIN");

        UserResponse response = userService.create(request, Set.of("SYSTEM_ADMIN"));

        assertThat(response.roles()).extracting(RoleResponse::name).containsExactly("ADMIN");
    }

    @Test
    void create_adminCreatingUserRole_succeeds() {
        UUID id = UUID.randomUUID();
        var request = new UserRequest("Alice", "alice@example.com", "secret", Set.of("USER"));

        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });
        mockRole("USER");

        UserResponse response = userService.create(request, Set.of("ADMIN"));

        assertThat(response.roles()).extracting(RoleResponse::name).containsExactly("USER");
        assertThat(response.name()).isEqualTo("Alice");
    }

    @Test
    void create_adminCreatingEditRole_succeeds() {
        UUID id = UUID.randomUUID();
        var request = new UserRequest("Bob", "bob@example.com", "secret", Set.of("EDIT"));

        given(userRepository.findByEmail("bob@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });
        mockRole("EDIT");

        UserResponse response = userService.create(request, Set.of("ADMIN"));

        assertThat(response.roles()).extracting(RoleResponse::name).containsExactly("EDIT");
    }

    @Test
    void create_adminCreatingAdminRole_succeeds() {
        UUID id = UUID.randomUUID();
        var request = new UserRequest("Admin2", "admin2@example.com", "secret", Set.of("ADMIN"));

        given(userRepository.findByEmail("admin2@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });
        mockRole("ADMIN");

        UserResponse response = userService.create(request, Set.of("ADMIN"));

        assertThat(response.roles()).extracting(RoleResponse::name).containsExactly("ADMIN");
    }

    @Test
    void create_adminCreatingSystemAdminRole_throwsForbidden() {
        var request = new UserRequest("SA", "sa@example.com", "secret", Set.of("SYSTEM_ADMIN"));
        mockRole("SYSTEM_ADMIN");

        assertThatThrownBy(() -> userService.create(request, Set.of("ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.FORBIDDEN.value());

        then(userRepository).should(never()).save(any());
    }

    @Test
    void create_withDuplicateEmail_throwsConflict() {
        var request = new UserRequest("Existing", "existing@example.com", "secret", Set.of("USER"));
        mockRole("USER");

        given(userRepository.findByEmail("existing@example.com"))
                .willReturn(Optional.of(sampleUser(UUID.randomUUID(), "Existing", "existing@example.com")));

        assertThatThrownBy(() -> userService.create(request, Set.of("ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());

        then(userRepository).should(never()).save(any());
    }

    // --- findAll ---

    @Test
    void findAll_returnsPagedUsers() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        User user1 = sampleUser(id1, "Alice", "a@example.com");
        User user2 = sampleUser(id2, "Bob", "b@example.com");
        var page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(user1, user2));
        given(userRepository.findAll(pageable)).willReturn(page);

        var result = userService.findAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).email()).isEqualTo("a@example.com");
        assertThat(result.getContent().get(0).name()).isEqualTo("Alice");
    }

    // --- findById ---

    @Test
    void findById_existingId_returnsDetail() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com");
        given(userRepository.findById(id)).willReturn(Optional.of(user));

        UserResponse result = userService.findById(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    void findById_nonExistingId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    // --- update ---

    @Test
    void update_systemAdminUpdatingAdminUser_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = sampleUser(id, "Old", "old@example.com");
        var request = new UserUpdateRequest("New", "new@example.com", Set.of("ADMIN"));

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        mockRole("ADMIN");

        UserResponse result = userService.update(id, request, currentUserId, Set.of("SYSTEM_ADMIN"));

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.roles()).extracting(RoleResponse::name).containsExactly("ADMIN");
    }

    @Test
    void update_adminUpdatingUserRole_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = sampleUser(id, "Old Name", "old@example.com");
        var request = new UserUpdateRequest("New Name", "new@example.com", Set.of("EDIT"));

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        mockRole("EDIT");

        UserResponse result = userService.update(id, request, currentUserId, Set.of("ADMIN"));

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.roles()).extracting(RoleResponse::name).containsExactly("EDIT");
    }

    @Test
    void update_adminUpdatingAdminUser_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = sampleUser(id, "Admin2", "admin2@example.com");
        user.getUserRoles().add(userRole(user, roleEntity("ADMIN")));
        var request = new UserUpdateRequest("Admin2", "admin2@example.com", Set.of("ADMIN"));

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        mockRole("ADMIN");

        UserResponse result = userService.update(id, request, currentUserId, Set.of("ADMIN"));

        assertThat(result.name()).isEqualTo("Admin2");
        assertThat(result.roles()).extracting(RoleResponse::name).containsExactly("ADMIN");
    }

    @Test
    void update_selfModification_byNonSystemAdmin_throwsForbidden() {
        UUID id = UUID.randomUUID();
        var request = new UserUpdateRequest("Me", "me@example.com", Set.of("USER"));

        assertThatThrownBy(() -> userService.update(id, request, id, Set.of("ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.FORBIDDEN.value());

        then(userRepository).should(never()).save(any());
    }

    @Test
    void update_selfModification_bySystemAdmin_removingOwnSystemAdminRole_throwsForbidden() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Me", "me@example.com");
        var request = new UserUpdateRequest("Me", "me@example.com", Set.of("USER"));

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        mockRole("USER");

        assertThatThrownBy(() -> userService.update(id, request, id, Set.of("SYSTEM_ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.FORBIDDEN.value());

        then(userRepository).should(never()).save(any());
    }

    @Test
    void update_emailInUseByOtherUser_throwsConflict() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com");
        User other = sampleUser(otherId, "Bob", "taken@example.com");
        var request = new UserUpdateRequest("Alice", "taken@example.com", Set.of("USER"));
        mockRole("USER");

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("taken@example.com")).willReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.update(id, request, currentUserId, Set.of("ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void update_nonExistingUser_throwsNotFound() {
        UUID id = UUID.randomUUID();
        var request = new UserUpdateRequest("Alice", "alice@example.com", Set.of("USER"));
        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(id, request, UUID.randomUUID(), Set.of("ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    // --- updateSelf ---

    @Test
    void updateSelf_happyPath_updatesNameAndEmail() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Old Name", "old@example.com");
        user.getUserRoles().add(userRole(user, roleEntity("USER")));
        var request = new UserSelfUpdateRequest("New Name", "new@example.com");

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.updateSelf(id, request);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.roles()).extracting(RoleResponse::name).containsExactly("USER");
    }

    @Test
    void updateSelf_emailUnchanged_doesNotCheckUniqueness() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com");
        var request = new UserSelfUpdateRequest("Alice Updated", "alice@example.com");

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.updateSelf(id, request);

        assertThat(result.name()).isEqualTo("Alice Updated");
        then(userRepository).should(never()).findByEmail(any());
    }

    @Test
    void updateSelf_emailTakenByOtherUser_throwsConflict() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com");
        User other = sampleUser(otherId, "Bob", "taken@example.com");
        var request = new UserSelfUpdateRequest("Alice", "taken@example.com");

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("taken@example.com")).willReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.updateSelf(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());

        then(userRepository).should(never()).save(any());
    }

    @Test
    void updateSelf_userNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        var request = new UserSelfUpdateRequest("Alice", "alice@example.com");

        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateSelf(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    // --- changePassword ---

    @Test
    void changePassword_correctCurrentPassword_savesNewHash() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com");
        var request = new UserPasswordChangeRequest("oldpass", "newpass");

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldpass", "hashed")).willReturn(true);
        given(passwordEncoder.encode("newpass")).willReturn("newhashed");
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        userService.changePassword(id, request);

        then(userRepository).should().save(any());
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com");
        var request = new UserPasswordChangeRequest("wrongpass", "newpass");

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpass", "hashed")).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());

        then(userRepository).should(never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        var request = new UserPasswordChangeRequest("pass", "newpass");

        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    // --- delete ---

    @Test
    void delete_adminDeletingUserAccount_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User target = sampleUser(id, "Alice", "alice@example.com");
        target.getUserRoles().add(userRole(target, roleEntity("USER")));
        given(userRepository.findById(id)).willReturn(Optional.of(target));

        userService.delete(id, currentUserId, Set.of("ADMIN"));

        then(userRepository).should().deleteById(id);
    }

    @Test
    void delete_adminDeletingAdminAccount_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User target = sampleUser(id, "Admin2", "admin2@example.com");
        target.getUserRoles().add(userRole(target, roleEntity("ADMIN")));
        given(userRepository.findById(id)).willReturn(Optional.of(target));

        userService.delete(id, currentUserId, Set.of("ADMIN"));

        then(userRepository).should().deleteById(id);
    }

    @Test
    void delete_systemAdminDeletingAdminAccount_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User target = sampleUser(id, "Admin2", "admin2@example.com");
        target.getUserRoles().add(userRole(target, roleEntity("ADMIN")));
        given(userRepository.findById(id)).willReturn(Optional.of(target));

        userService.delete(id, currentUserId, Set.of("SYSTEM_ADMIN"));

        then(userRepository).should().deleteById(id);
    }

    @Test
    void delete_selfDelete_throwsForbidden() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> userService.delete(id, id, Set.of("SYSTEM_ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.FORBIDDEN.value());

        then(userRepository).should(never()).deleteById(any());
    }

    @Test
    void delete_whenUserNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(id, currentUserId, Set.of("ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value());

        then(userRepository).should(never()).deleteById(any());
    }

    // --- isSetupRequired ---

    @Test
    void isSetupRequired_whenNoUsers_returnsTrue() {
        given(userRepository.count()).willReturn(0L);
        assertThat(userService.isSetupRequired()).isTrue();
    }

    @Test
    void isSetupRequired_whenUsersExist_returnsFalse() {
        given(userRepository.count()).willReturn(1L);
        assertThat(userService.isSetupRequired()).isFalse();
    }

    // --- findByEmail ---

    @Test
    void findByEmail_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "test@example.com");
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        assertThat(userService.findByEmail("test@example.com")).contains(user);
    }

    private UserRole userRole(User user, RoleEntity role) {
        UserRole ur = new UserRole();
        ur.setId(UUID.randomUUID());
        ur.setUser(user);
        ur.setRole(role);
        return ur;
    }
}
