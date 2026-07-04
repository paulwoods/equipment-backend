package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * First-run admin account creation, serialized by a Postgres advisory lock.
 * Separated from {@link UserService} (CRUD + self-service) so the bootstrap
 * concern — which runs once, before any caller exists to authorize — has its
 * own module and its own collaborator set ({@link EntityManager} for the lock,
 * {@link UserService} for the actual user creation).
 */
@Service
@RequiredArgsConstructor
public class AdminBootstrap {

    // Arbitrary application-wide key for the Postgres advisory lock guarding first-run setup.
    private static final long SETUP_ADVISORY_LOCK_KEY = 4_242_424_242L;

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public User createInitialAdmin(String email, String password) {
        // Serialize concurrent setup attempts: the advisory lock is held until the
        // transaction ends, so the count re-check below is authoritative.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(?1)")
                .setParameter(1, SETUP_ADVISORY_LOCK_KEY)
                .getSingleResult();
        if (userRepository.count() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Setup already completed");
        }
        return userService.createInternal(email, email, password, Set.of("SYSTEM_ADMIN", "ADMIN", "EDIT", "USER"));
    }

    @Transactional(readOnly = true)
    public boolean isSetupRequired() {
        return userRepository.count() == 0;
    }
}