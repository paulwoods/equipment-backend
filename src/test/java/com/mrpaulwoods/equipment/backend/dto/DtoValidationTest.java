package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // --- EquipmentRequest ---

    @Test
    void equipmentRequest_valid_noViolations() {
        EquipmentRequest req = new EquipmentRequest(
                "Acme", "X100", "SN-1", "TAG-1", "Lab",
                EquipmentStatus.ACTIVE, "A pump", LocalDate.of(2024, 1, 1)
        );
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void equipmentRequest_blankManufacturer_violation() {
        EquipmentRequest req = new EquipmentRequest(
                "", "X100", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.now()
        );
        Set<ConstraintViolation<EquipmentRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("manufacturer"));
    }

    @Test
    void equipmentRequest_blankModelNumber_violation() {
        EquipmentRequest req = new EquipmentRequest(
                "Acme", "  ", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.now()
        );
        Set<ConstraintViolation<EquipmentRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("modelNumber"));
    }

    @Test
    void equipmentRequest_nullStatus_violation() {
        EquipmentRequest req = new EquipmentRequest(
                "Acme", "X100", null, null, null,
                null, null, LocalDate.now()
        );
        Set<ConstraintViolation<EquipmentRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("status"));
    }

    @Test
    void equipmentRequest_nullPurchaseDate_violation() {
        EquipmentRequest req = new EquipmentRequest(
                "Acme", "X100", null, null, null,
                EquipmentStatus.ACTIVE, null, null
        );
        Set<ConstraintViolation<EquipmentRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("purchaseDate"));
    }

    // --- ProcedureRequest ---

    @Test
    void procedureRequest_valid_noViolations() {
        ProcedureRequest req = new ProcedureRequest("Oil Change", "Desc", "Step 1", "Wrench", 30);
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void procedureRequest_blankName_violation() {
        ProcedureRequest req = new ProcedureRequest("", "Desc", "Step 1", "Wrench", 30);
        Set<ConstraintViolation<ProcedureRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void procedureRequest_blankSteps_violation() {
        ProcedureRequest req = new ProcedureRequest("Oil Change", "Desc", "", "Wrench", 30);
        Set<ConstraintViolation<ProcedureRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("steps"));
    }

    @Test
    void procedureRequest_zeroIntervalDays_violation() {
        ProcedureRequest req = new ProcedureRequest("Oil Change", "Desc", "Step 1", "Wrench", 0);
        Set<ConstraintViolation<ProcedureRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("intervalDays"));
    }

    @Test
    void procedureRequest_negativeIntervalDays_violation() {
        ProcedureRequest req = new ProcedureRequest("Oil Change", "Desc", "Step 1", "Wrench", -5);
        Set<ConstraintViolation<ProcedureRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("intervalDays"));
    }

    // --- PerformRequest ---

    @Test
    void performRequest_valid_noViolations() {
        PerformRequest req = new PerformRequest(LocalDate.now(), "All good");
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void performRequest_nullDate_violation() {
        PerformRequest req = new PerformRequest(null, "Notes");
        Set<ConstraintViolation<PerformRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("date"));
    }

    @Test
    void performRequest_nullNotes_noViolation() {
        PerformRequest req = new PerformRequest(LocalDate.now(), null);
        assertThat(validator.validate(req)).isEmpty();
    }

    // --- ForgotPasswordRequest ---

    @Test
    void forgotPasswordRequest_valid_noViolations() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("user@example.com");
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void forgotPasswordRequest_blankEmail_violation() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("  ");
        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void forgotPasswordRequest_invalidEmail_violation() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("not-an-email");
        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void forgotPasswordRequest_emailTooLong_violation() {
        String longEmail = "a".repeat(250) + "@example.com";
        ForgotPasswordRequest req = new ForgotPasswordRequest(longEmail);
        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    // --- ResetPasswordRequest ---

    @Test
    void resetPasswordRequest_valid_noViolations() {
        ResetPasswordRequest req = new ResetPasswordRequest("token-value", "password123");
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void resetPasswordRequest_blankToken_violation() {
        ResetPasswordRequest req = new ResetPasswordRequest("", "password123");
        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("token"));
    }

    @Test
    void resetPasswordRequest_shortPassword_violation() {
        ResetPasswordRequest req = new ResetPasswordRequest("token", "short");
        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));
    }

    @Test
    void resetPasswordRequest_longPassword_violation() {
        String longPassword = "a".repeat(129);
        ResetPasswordRequest req = new ResetPasswordRequest("token", longPassword);
        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));
    }
}
