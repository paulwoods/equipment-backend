package com.mrpaulwoods.equipment.backend.support;

import com.mrpaulwoods.equipment.backend.repository.*;
import com.mrpaulwoods.equipment.backend.service.UserService;
import com.mrpaulwoods.equipment.backend.util.Role;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for all {@code *IT.java} integration tests. Boots the full Spring context on a random port
 * and runs against a real Postgres 18 container managed by Testcontainers.
 *
 * <p>Requires Docker to be available on the host (local dev machine or CI runner).
 * <p>The Postgres container is a singleton started once for the whole JVM — all test classes share it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
public abstract class BaseIT {

    protected static final String ADMIN_EMAIL = "admin@test.local";
    protected static final String ADMIN_PASSWORD = "AdminPass1!";
    protected static final String USER_EMAIL = "user@test.local";
    protected static final String USER_PASSWORD = "UserPass1!";

    /**
     * Single shared container for the entire test suite — started once, never stopped.
     */
    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:18")
                .withDatabaseName("equipment_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected EquipmentRepository equipmentRepository;

    @Autowired
    protected ProcedureRepository procedureRepository;

    @Autowired
    protected PerformRepository performRepository;

    @Autowired
    protected UserService userService;

    @BeforeEach
    void resetDatabase() {
        performRepository.deleteAll();
        procedureRepository.deleteAll();
        equipmentRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Logs in as ADMIN (creates account if needed) and returns the access_token cookie string.
     */
    protected String loginAsAdmin() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            userService.createInternal(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);
        }
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /**
     * Logs in as USER role (creates account if needed) and returns the access_token cookie string.
     */
    protected String loginAsUser() {
        if (userRepository.findByEmail(USER_EMAIL).isEmpty()) {
            userService.createInternal(USER_EMAIL, USER_PASSWORD, Role.USER);
        }
        return login(USER_EMAIL, USER_PASSWORD);
    }

    protected String login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
        return response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("access_token="))
                .map(c -> c.split(";", 2)[0])
                .findFirst()
                .orElseThrow(() -> new AssertionError("No access_token cookie in login response"));
    }

    protected HttpHeaders cookieHeaders(String accessTokenCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, accessTokenCookie);
        return headers;
    }

    protected HttpHeaders authJsonHeaders(String accessTokenCookie) {
        HttpHeaders headers = cookieHeaders(accessTokenCookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }
}
