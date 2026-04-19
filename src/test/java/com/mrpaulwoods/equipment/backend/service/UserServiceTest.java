package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.UserRequest;
import com.mrpaulwoods.equipment.backend.dto.UserResponse;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser(UUID id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setRole(role);
        return user;
    }

    @Test
    void create_withNewEmail_savesAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        var request = new UserRequest("new@example.com", "secret", Role.USER);

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });

        UserResponse response = userService.create(request);

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void create_withDuplicateEmail_throwsConflict() {
        var request = new UserRequest("existing@example.com", "secret", Role.ADMIN);

        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(sampleUser(UUID.randomUUID(), "existing@example.com", Role.ADMIN)));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());

        verify(userRepository, never()).save(any());
    }

    @Test
    void findAll_returnsPagedUsers() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        var page = new org.springframework.data.domain.PageImpl<>(List.of(
                sampleUser(id1, "a@example.com", Role.ADMIN),
                sampleUser(id2, "b@example.com", Role.USER)
        ));
        when(userRepository.findAll(pageable)).thenReturn(page);

        var result = userService.findAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).email()).isEqualTo("a@example.com");
        assertThat(result.getContent().get(1).role()).isEqualTo(Role.USER);
    }

    @Test
    void delete_whenUserExists_deletesById() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        userService.delete(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void isSetupRequired_whenNoUsers_returnsTrue() {
        when(userRepository.count()).thenReturn(0L);
        assertThat(userService.isSetupRequired()).isTrue();
    }

    @Test
    void isSetupRequired_whenUsersExist_returnsFalse() {
        when(userRepository.count()).thenReturn(1L);
        assertThat(userService.isSetupRequired()).isFalse();
    }

    @Test
    void findByEmail_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id, "test@example.com", Role.USER);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThat(userService.findByEmail("test@example.com")).contains(user);
    }

    @Test
    void delete_whenUserNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value());

        verify(userRepository, never()).deleteById(any());
    }
}
