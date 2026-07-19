package com.mrpaulwoods.equipment.backend.service;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated caller of the current request, as seen by the authorization
 * seam: an id and a set of role names, exposed as behavior rather than raw data
 * so callers don't need to know the "ROLE_" prefix convention or {@link RoleTier}'s
 * tier rules.
 *
 * <p>Built by {@code com.mrpaulwoods.equipment.backend.config.CallerContextArgumentResolver}
 * from the current {@code Authentication}; the "ROLE_" prefix stripping happens
 * exactly once, there.
 */
public record CallerContext(UUID userId, Set<String> roleNames) {

    /** {@code true} if the caller holds the {@code SYSTEM_ADMIN} role. */
    public boolean isSystemAdmin() {
        return roleNames.contains("SYSTEM_ADMIN");
    }

    /** {@code true} if the caller holds the given role. */
    public boolean hasRole(String roleName) {
        return roleNames.contains(roleName);
    }

    /** Delegates to {@link RoleTier#canManageRole(Set, String)}. */
    public boolean canManage(String targetRoleName) {
        return RoleTier.canManageRole(roleNames, targetRoleName);
    }

    /** Delegates to {@link RoleTier#assertCallerCanManageRole(Set, String)}. */
    public void assertCanManage(String targetRoleName) {
        RoleTier.assertCallerCanManageRole(roleNames, targetRoleName);
    }
}
