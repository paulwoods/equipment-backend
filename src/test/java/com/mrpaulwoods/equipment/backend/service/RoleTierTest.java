package com.mrpaulwoods.equipment.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTierTest {

    @Test
    void systemAdmin_canManageAnyRole() {
        for (String role : new String[]{"USER", "EDIT", "ADMIN", "SYSTEM_ADMIN"}) {
            assertThatCode(() -> RoleTier.assertCallerCanManageRole(Set.of("SYSTEM_ADMIN"), role))
                    .as("SYSTEM_ADMIN managing %s", role)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void admin_canManageUserEditAdmin() {
        for (String role : new String[]{"USER", "EDIT", "ADMIN"}) {
            assertThatCode(() -> RoleTier.assertCallerCanManageRole(Set.of("ADMIN"), role))
                    .as("ADMIN managing %s", role)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void admin_cannotManageSystemAdmin() {
        assertThatThrownBy(() -> RoleTier.assertCallerCanManageRole(Set.of("ADMIN"), "SYSTEM_ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void nonAdminCannotManageAnything() {
        assertThatThrownBy(() -> RoleTier.assertCallerCanManageRole(Set.of("USER"), "USER"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> RoleTier.assertCallerCanManageRole(Set.of("EDIT"), "USER"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void emptyCallerRoles_isForbidden() {
        assertThatThrownBy(() -> RoleTier.assertCallerCanManageRole(Set.of(), "USER"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void systemAdminAlongsideOtherRoles_stillAllowsAll() {
        assertThatCode(() -> RoleTier.assertCallerCanManageRole(Set.of("USER", "SYSTEM_ADMIN"), "SYSTEM_ADMIN"))
                .doesNotThrowAnyException();
    }
}