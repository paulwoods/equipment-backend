package com.mrpaulwoods.equipment.backend.dto;

/**
 * Tells the login page whether to render the Google button, and with which
 * OAuth client ID. The client ID is public — the browser hands it to Google.
 *
 * @param clientId null when Google sign-in is not configured
 */
public record GoogleConfigResponse(boolean enabled, String clientId) {
}
