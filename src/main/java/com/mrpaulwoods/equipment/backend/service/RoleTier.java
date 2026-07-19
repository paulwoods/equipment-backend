package com.mrpaulwoods.equipment.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * Named authorization tiers for use in {@code @PreAuthorize} annotations, per
 * ADR-0028: read = any authenticated role, write = {@code EDIT} and above,
 * user management = {@code ADMIN} and above. {@code SYSTEM_ADMIN} is the
 * superset role and appears in every tier list.
 *
 * <p>Each constant is a SpEL string identical to the literals it replaces, so
 * referencing it from an annotation ({@code @PreAuthorize(RoleTier.WRITE)})
 * changes behavior in no way — it only names the tier once instead of
 * copy-pasting the role list at every endpoint.
 *
 * <p>The service-layer refinement of the same tiers — which roles a caller may
 * assign/revoke — lives in {@link #assertCallerCanManageRole(Set, String)}.
 */
public final class RoleTier {

    /** Roles an {@code ADMIN} (non-{@code SYSTEM_ADMIN}) caller may manage. */
    private static final Set<String> ADMIN_MANAGEABLE_ROLES = Set.of("USER", "EDIT", "ADMIN");

    private RoleTier() {
    }

    /** Read endpoints: any authenticated role. */
    public static final String READ = "hasAnyRole('USER', 'EDIT', 'ADMIN', 'SYSTEM_ADMIN')";

    /** Write endpoints (POST/PUT/DELETE) on domain resources: {@code EDIT} and above. */
    public static final String WRITE = "hasAnyRole('EDIT', 'ADMIN', 'SYSTEM_ADMIN')";

    /** User-management endpoints: {@code ADMIN} and above. */
    public static final String MANAGE_USERS = "hasAnyRole('ADMIN', 'SYSTEM_ADMIN')";

    /** Dashboard email trigger: aligned to {@link #MANAGE_USERS} — {@code ADMIN} and above. */
    public static final String EMAIL = "hasAnyRole('ADMIN', 'SYSTEM_ADMIN')";

    /**
     * {@code true} unless the caller may manage {@code targetRoleName}:
     * {@code SYSTEM_ADMIN} may manage any role; {@code ADMIN} may manage only
     * {@code USER}/{@code EDIT}/{@code ADMIN}; anything else is forbidden.
     */
    public static boolean canManageRole(Set<String> callerRoleNames, String targetRoleName) {
        if (callerRoleNames.contains("SYSTEM_ADMIN")) {
            return true;
        }
        return callerRoleNames.contains("ADMIN") && ADMIN_MANAGEABLE_ROLES.contains(targetRoleName);
    }

    /**
     * Throws {@code 403 FORBIDDEN} unless the caller may manage {@code targetRoleName}.
     * See {@link #canManageRole(Set, String)} for the tier rules.
     */
    public static void assertCallerCanManageRole(Set<String> callerRoleNames, String targetRoleName) {
        if (!canManageRole(callerRoleNames, targetRoleName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient privileges to manage role: " + targetRoleName);
        }
    }
}