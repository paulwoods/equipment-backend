package com.mrpaulwoods.equipment.backend;

import com.mrpaulwoods.equipment.backend.dto.ExportResponse;
import com.mrpaulwoods.equipment.backend.dto.ImportRequest;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.service.ExportService;
import com.mrpaulwoods.equipment.backend.service.ImportService;
import com.mrpaulwoods.equipment.backend.service.UserService;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Boots the full application context against a real Postgres so the Flyway
 * migrations, the JOIN FETCH JPQL queries, and the native advisory-lock query
 * are exercised for real. Requires Docker; runs via the failsafe plugin
 * ({@code ./mvnw verify}), not the surefire unit-test phase.
 */
@SpringBootTest
@Testcontainers
class BackendApplicationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("app.jwt-secret", () -> "integration-test-secret-0123456789-abcdefghijklmnop");
        registry.add("spring.mail.username", () -> "it-user");
        registry.add("spring.mail.password", () -> "it-password");
        registry.add("scheduling.enabled", () -> "false");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ImportService importService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private UserService userService;

    @Test
    void flywayMigrationsApplyAgainstRealPostgres() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(8);
    }

    @Test
    @Transactional
    void joinFetchQueriesExecuteAgainstRealPostgres() {
        importSampleEquipment();

        List<Equipment> equipment = equipmentRepository.findAllWithProcedures();

        assertThat(equipment).hasSize(1);
        assertThat(equipment.getFirst().getProcedures()).hasSize(1);
        assertThat(equipment.getFirst().getProcedures().getFirst().getName()).isEqualTo("Oil change");
    }

    @Test
    @Transactional
    void importThenExportRoundTrips() {
        ImportResult imported = importSampleEquipment();
        assertThat(imported.equipmentImported()).isEqualTo(1);

        List<ExportResponse.EquipmentExport> exported = exportService.exportAll();

        assertThat(exported).hasSize(1);
        ExportResponse.EquipmentExport equipment = exported.getFirst();
        assertThat(equipment.manufacturer()).isEqualTo("Acme");
        assertThat(equipment.modelNumber()).isEqualTo("X100");
        assertThat(equipment.status()).isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(equipment.procedures()).hasSize(1);
        ExportResponse.ProcedureExport procedure = equipment.procedures().getFirst();
        assertThat(procedure.name()).isEqualTo("Oil change");
        assertThat(procedure.history()).hasSize(1);
        assertThat(procedure.history().getFirst().date()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    @Transactional
    void createInitialAdminIsGuardedByAdvisoryLockAndUserCount() {
        User admin = userService.createInitialAdmin("admin@example.com", "super-secret-password");

        assertThat(admin.getId()).isNotNull();
        assertThat(admin.getUserRoles()).hasSize(4);

        assertThatThrownBy(() -> userService.createInitialAdmin("second@example.com", "another-password"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    private ImportResult importSampleEquipment() {
        var perform = new ImportRequest.PerformImport(null, LocalDate.of(2024, 6, 1), "Done");
        var procedure = new ImportRequest.ProcedureImport(
                null, "Oil change", null, "Drain and refill", null, 90, List.of(perform));
        var equipment = new ImportRequest.EquipmentImport(
                null, "Acme", "X100", "SN-001", null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2024, 1, 15), List.of(procedure));
        return importService.importEquipment(List.of(equipment));
    }
}
