package com.mrpaulwoods.equipment.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluates the {@link RoleTier} SpEL constants the same way {@code @PreAuthorize}
 * does at runtime, proving the *behavior* the annotations grant rather than just
 * the literal SpEL string.
 */
class RoleTierSpelTest {

    private boolean evaluate(String spel, String... roleNames) {
        List<GrantedAuthority> authorities = List.of(roleNames).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("caller", "n/a", authorities);

        SecurityExpressionRoot<Object> root = new SecurityExpressionRoot<>(authentication) {
        };
        StandardEvaluationContext context = new StandardEvaluationContext(root);

        ExpressionParser parser = new SpelExpressionParser();
        return Boolean.TRUE.equals(parser.parseExpression(spel).getValue(context, Boolean.class));
    }

    @Test
    void email_systemAdminOnly_isNowAuthorized() {
        // The behavior change this PRD workstream deliberately makes: a SYSTEM_ADMIN
        // who is not also an ADMIN can now trigger the dashboard email.
        assertThat(evaluate(RoleTier.EMAIL, "SYSTEM_ADMIN")).isTrue();
    }

    @Test
    void email_adminOnly_isAuthorized() {
        assertThat(evaluate(RoleTier.EMAIL, "ADMIN")).isTrue();
    }

    @Test
    void email_userOrEditOnly_isForbidden() {
        assertThat(evaluate(RoleTier.EMAIL, "USER")).isFalse();
        assertThat(evaluate(RoleTier.EMAIL, "EDIT")).isFalse();
    }

    @Test
    void email_isAlignedWithManageUsers() {
        for (String[] roles : new String[][]{{"SYSTEM_ADMIN"}, {"ADMIN"}, {"USER"}, {"EDIT"}}) {
            assertThat(evaluate(RoleTier.EMAIL, roles))
                    .as("EMAIL vs MANAGE_USERS for %s", (Object) roles)
                    .isEqualTo(evaluate(RoleTier.MANAGE_USERS, roles));
        }
    }

    @Test
    void readWriteManageUsers_stillGrantExpectedRoles() {
        assertThat(evaluate(RoleTier.READ, "USER")).isTrue();
        assertThat(evaluate(RoleTier.READ, "EDIT")).isTrue();
        assertThat(evaluate(RoleTier.READ, "ADMIN")).isTrue();
        assertThat(evaluate(RoleTier.READ, "SYSTEM_ADMIN")).isTrue();

        assertThat(evaluate(RoleTier.WRITE, "USER")).isFalse();
        assertThat(evaluate(RoleTier.WRITE, "EDIT")).isTrue();
        assertThat(evaluate(RoleTier.WRITE, "ADMIN")).isTrue();
        assertThat(evaluate(RoleTier.WRITE, "SYSTEM_ADMIN")).isTrue();

        assertThat(evaluate(RoleTier.MANAGE_USERS, "USER")).isFalse();
        assertThat(evaluate(RoleTier.MANAGE_USERS, "EDIT")).isFalse();
        assertThat(evaluate(RoleTier.MANAGE_USERS, "ADMIN")).isTrue();
        assertThat(evaluate(RoleTier.MANAGE_USERS, "SYSTEM_ADMIN")).isTrue();
    }
}
