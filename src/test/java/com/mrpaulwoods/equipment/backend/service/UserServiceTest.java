package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import com.mrpaulwoods.equipment.backend.util.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
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
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser(UUID id, String name, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setRole(role);
        return user;
    }

    // --- create ---

    @Test
    void create_systemAdminCreatingAnyRole_succeeds() {
        UUID id = UUID.randomUUID();
        var request = new UserRequest("Alice", "alice@example.com", "secret", Role.ADMIN);

        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });

        UserCreateResponse response = userService.create(request, Role.SYSTEM_ADMIN);

        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void create_adminCreatingUserRole_succeeds() {
        UUID id = UUID.randomUUID();
        var request = new UserRequest("Alice", "alice@example.com", "secret", Role.USER);

        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });

        UserCreateResponse response = userService.create(request, Role.ADMIN);

        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.name()).isEqualTo("Alice");
    }

    @Test
    void create_adminCreatingEditRole_succeeds() {
        UUID id = UUID.randomUUID();
        var request = new UserRequest("Bob", "bob@example.com", "secret", Role.EDIT);

        given(userRepository.findByEmail("bob@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });

        UserCreateResponse response = userService.create(request, Role.ADMIN);

        assertThat(response.role()).isEqualTo(Role.EDIT);
    }

    @Test
    void create_adminCreatingAdminRole_throwsForbidden() {
        var request = new UserRequest("Admin2", "admin2@example.com", "secret", Role.ADMIN);

        assertThatThrownBy(() -> userService.create(request, Role.ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.FORBIDDEN.value());

        then(userRepository).should(never()).save(any());
    }

    @Test
    void create_adminCreatingSystemAdminRole_throwsForbidden() {
        var request = new UserRequest("SA", "sa@example.com", "secret", Role.SYSTEM_ADMIN);

        assertThatThrownBy(() -> userService.create(request, Role.ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.FORBIDDEN.value());

        then(userRepository).should(never()).save(any());
    }

    @Test
    void create_withDuplicateEmail_throwsConflict() {
        var request = new UserRequest("Existing", "existing@example.com", "secret", Role.USER);

        given(userRepository.findByEmail("existing@example.com"))
                .willReturn(Optional.of(sampleUser(UUID.randomUUID(), "Existing", "existing@example.com", Role.USER)));

        assertThatThrownBy(() -> userService.create(request, Role.ADMIN))
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
        var page = new org.springframework.data.domain.PageImpl<>(List.of(
                sampleUser(id1, "Alice", "a@example.com", Role.ADMIN),
                sampleUser(id2, "Bob", "b@example.com", Role.USER)
        ));
        given(userRepository.findAll(pageable)).willReturn(page);

        var result = userService.findAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).email()).isEqualTo("a@example.com");
        assertThat(result.getContent().get(0).name()).isEqualTo("Alice");
        assertThat(result.getContent().get(1).role()).isEqualTo(Role.USER);
    }

    // --- findById ---

    @Test
    void findById_existingId_returnsDetail() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com", Role.USER);
        given(userRepository.findById(id)).willReturn(Optional.of(user));

        UserDetailResponse result = userService.findById(id);

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
        User user = sampleUser(id, "Old", "old@example.com", Role.ADMIN);
        var request = new UserUpdateRequest("New", "new@example.com", Role.ADMIN);

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        UserUpdateResponse result = userService.update(id, request, currentUserId, Role.SYSTEM_ADMIN);

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void update_adminUpdatingUserRole_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = sampleUser(id, "Old Name", "old@example.com", Role.USER);
        var request = new UserUpdateRequest("New Name", "new@example.com", Role.EDIT);

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        UserUpdateResponse result = userService.update(id, request, currentUserId, Role.ADMIN);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.role()).isEqualTo(Role.EDIT);
    }

    @Test
    void update_adminUpdatingAdminUser_throwsForbidden() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = sampleUser(id, "Admin2", "admin2@example.com", Role.ADMIN);
        var request = new UserUpdateRequest("Admin2", "admin2@example.com", Role.ADMIN);

        given(userRepository.findById(id)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.update(id, request, currentUserId, Role.ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.FORBIDDEN.value());

        then(userRepository).should(never()).save(any());
    }

    @Test
    void update_selfModification_throwsForbidden() {
        UUID id = UUID.randomUUID();
        var request = new UserUpdateRequest("Me", "me@example.com", Role.USER);

        assertThatThrownBy(() -> userService.update(id, request, id, Role.SYSTEM_ADMIN))
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
        User user = sampleUser(id, "Alice", "alice@example.com", Role.USER);
        User other = sampleUser(otherId, "Bob", "taken@example.com", Role.USER);
        var request = new UserUpdateRequest("Alice", "taken@example.com", Role.USER);

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("taken@example.com")).willReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.update(id, request, currentUserId, Role.ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void update_nonExistingUser_throwsNotFound() {
        UUID id = UUID.randomUUID();
        var request = new UserUpdateRequest("Alice", "alice@example.com", Role.USER);
        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(id, request, UUID.randomUUID(), Role.ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    // --- updateSelf ---

    @Test
    void updateSelf_happyPath_updatesNameAndEmail() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Old Name", "old@example.com", Role.USER);
        var request = new UserSelfUpdateRequest("New Name", "new@example.com");

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        UserSelfUpdateResponse result = userService.updateSelf(id, request);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void updateSelf_emailUnchanged_doesNotCheckUniqueness() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com", Role.USER);
        var request = new UserSelfUpdateRequest("Alice Updated", "alice@example.com");

        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        UserSelfUpdateResponse result = userService.updateSelf(id, request);

        assertThat(result.name()).isEqualTo("Alice Updated");
        then(userRepository).should(never()).findByEmail(any());
    }

    @Test
    void updateSelf_emailTakenByOtherUser_throwsConflict() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User user = sampleUser(id, "Alice", "alice@example.com", Role.USER);
        User other = sampleUser(otherId, "Bob", "taken@example.com", Role.USER);
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

    // --- delete ---

    @Test
    void delete_adminDeletingUserAccount_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User target = sampleUser(id, "Alice", "alice@example.com", Role.USER);
        given(userRepository.findById(id)).willReturn(Optional.of(target));

        userService.delete(id, currentUserId, Role.ADMIN);

        then(userRepository).should().deleteById(id);
    }

    @Test
    void delete_adminDeletingAdminAccount_throwsForbidden() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User target = sampleUser(id, "Admin2", "admin2@example.com", Role.ADMIN);
        given(userRepository.findById(id)).willReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.delete(id, currentUserId, Role.ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.FORBIDDEN.value());

        then(userRepository).should(never()).deleteById(any());
    }

    @Test
    void delete_systemAdminDeletingAdminAccount_succeeds() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User target = sampleUser(id, "Admin2", "admin2@example.com", Role.ADMIN);
        given(userRepository.findById(id)).willReturn(Optional.of(target));

        userService.delete(id, currentUserId, Role.SYSTEM_ADMIN);

        then(userRepository).should().deleteById(id);
    }

    @Test
    void delete_selfDelete_throwsForbidden() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> userService.delete(id, id, Role.SYSTEM_ADMIN))
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

        assertThatThrownBy(() -> userService.delete(id, currentUserId, Role.ADMIN))
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
        User user = sampleUser(id, "Alice", "test@example.com", Role.USER);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        assertThat(userService.findByEmail("test@example.com")).contains(user);
    }
}
