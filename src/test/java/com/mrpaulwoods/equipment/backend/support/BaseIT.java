package com.mrpaulwoods.equipment.backend.support;

import com.mrpaulwoods.equipment.backend.repository.RefreshTokenRepository;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all {@code *IT.java} integration tests. Boots the full Spring context on a random port
 * and runs against a real Postgres 18 container managed by Testcontainers.
 *
 * <p>Requires Docker to be available on the host (local dev machine or CI runner).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestRestTemplate
public abstract class BaseIT {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("equipment_test")
            .withUsername("test")
            .withPassword("test");
    @LocalServerPort
    protected int port;
    @Autowired
    protected TestRestTemplate restTemplate;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }
}
