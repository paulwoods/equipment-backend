package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminBootstrap adminBootstrap;

    private void mockAdvisoryLock() {
        Query query = mock(Query.class);
        given(entityManager.createNativeQuery(contains("pg_advisory_xact_lock"))).willReturn(query);
        given(query.setParameter(anyInt(), any())).willReturn(query);
        given(query.getSingleResult()).willReturn(Boolean.TRUE);
    }

    // --- isSetupRequired ---

    @Test
    void isSetupRequired_whenNoUsers_returnsTrue() {
        given(userRepository.count()).willReturn(0L);
        assertThat(adminBootstrap.isSetupRequired()).isTrue();
    }

    @Test
    void isSetupRequired_whenUsersExist_returnsFalse() {
        given(userRepository.count()).willReturn(1L);
        assertThat(adminBootstrap.isSetupRequired()).isFalse();
    }

    // --- createInitialAdmin ---

    @Test
    void createInitialAdmin_whenNoUsers_delegatesToCreateInternalWithAllRoles() {
        mockAdvisoryLock();
        given(userRepository.count()).willReturn(0L);
        User admin = new User();
        admin.setEmail("admin@example.com");
        given(userService.createInternal("admin@example.com", "admin@example.com", "secret",
                Set.of("SYSTEM_ADMIN", "ADMIN", "EDIT", "USER"))).willReturn(admin);

        User created = adminBootstrap.createInitialAdmin("admin@example.com", "secret");

        assertThat(created).isSameAs(admin);
        then(entityManager).should().createNativeQuery(contains("pg_advisory_xact_lock"));
    }

    @Test
    void createInitialAdmin_whenUsersAlreadyExist_throwsConflict() {
        mockAdvisoryLock();
        given(userRepository.count()).willReturn(1L);

        assertThatThrownBy(() -> adminBootstrap.createInitialAdmin("admin@example.com", "secret"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());

        then(userService).should(never()).createInternal(any(), any(), any(), any());
    }
}