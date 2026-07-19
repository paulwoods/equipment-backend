package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CookieServiceTest {

    private AppProperties appProperties;
    private CookieService cookieService;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        cookieService = new CookieService(appProperties);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    private String capturedSetCookieHeader() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), captor.capture());
        return captor.getValue();
    }

    // --- access token cookie ---

    @Test
    void setAccessTokenCookie_setsHttpOnlySameSiteLaxAndRootPath() {
        cookieService.setAccessTokenCookie(request, response, "the-token");

        String header = capturedSetCookieHeader();
        assertThat(header).startsWith("access_token=the-token");
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("SameSite=Lax");
        assertThat(header).contains("Path=/");
        // Path=/ is a prefix of Path=/api/v1/auth too, so make sure we didn't get scoped there.
        assertThat(header).doesNotContain("Path=/api/v1/auth");
    }

    @Test
    void setAccessTokenCookie_maxAgeDerivesFromJwtExpirationMs_notHardcoded() {
        appProperties.setJwtExpirationMs(45_000L);

        cookieService.setAccessTokenCookie(request, response, "the-token");

        assertThat(capturedSetCookieHeader()).contains("Max-Age=45");
    }

    @Test
    void setAccessTokenCookie_differentJwtExpiration_changesMaxAge() {
        appProperties.setJwtExpirationMs(7_200_000L);

        cookieService.setAccessTokenCookie(request, response, "the-token");

        assertThat(capturedSetCookieHeader()).contains("Max-Age=7200");
    }

    // --- refresh token cookie ---

    @Test
    void setRefreshTokenCookie_scopedToAuthPath() {
        cookieService.setRefreshTokenCookie(request, response, "raw-refresh");

        String header = capturedSetCookieHeader();
        assertThat(header).startsWith("refresh_token=raw-refresh");
        assertThat(header).contains("Path=/api/v1/auth");
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("SameSite=Lax");
    }

    @Test
    void setRefreshTokenCookie_maxAgeDerivesFromRefreshTokenDays_notHardcoded() {
        appProperties.setRefreshTokenDays(14);

        cookieService.setRefreshTokenCookie(request, response, "raw-refresh");

        assertThat(capturedSetCookieHeader()).contains("Max-Age=" + (14L * 24 * 60 * 60));
    }

    @Test
    void setRefreshTokenCookie_differentRefreshDays_changesMaxAge() {
        appProperties.setRefreshTokenDays(1);

        cookieService.setRefreshTokenCookie(request, response, "raw-refresh");

        assertThat(capturedSetCookieHeader()).contains("Max-Age=86400");
    }

    // --- secure flag / override behavior ---

    @Test
    void secureFlag_defaultsToRequestIsSecure_whenNoOverrideConfigured() {
        when(request.isSecure()).thenReturn(false);

        cookieService.setAccessTokenCookie(request, response, "t");

        assertThat(capturedSetCookieHeader()).doesNotContain("Secure");
    }

    @Test
    void secureFlag_followsRequestIsSecureTrue_whenNoOverrideConfigured() {
        when(request.isSecure()).thenReturn(true);

        cookieService.setAccessTokenCookie(request, response, "t");

        assertThat(capturedSetCookieHeader()).contains("Secure");
    }

    @Test
    void secureFlag_overrideTrue_forcesSecureOverInsecureRequest() {
        appProperties.setCookieSecure(true);
        when(request.isSecure()).thenReturn(false);

        cookieService.setAccessTokenCookie(request, response, "t");

        assertThat(capturedSetCookieHeader()).contains("Secure");
    }

    @Test
    void secureFlag_overrideFalse_suppressesSecureOverSecureRequest() {
        appProperties.setCookieSecure(false);
        when(request.isSecure()).thenReturn(true);

        cookieService.setAccessTokenCookie(request, response, "t");

        assertThat(capturedSetCookieHeader()).doesNotContain("Secure");
    }

    // --- clearCookie ---

    @Test
    void clearCookie_setsMaxAgeZeroAndGivenPath() {
        cookieService.clearCookie(request, response, "access_token", "/");

        String header = capturedSetCookieHeader();
        assertThat(header).startsWith("access_token=");
        assertThat(header).contains("Max-Age=0");
        assertThat(header).contains("Path=/");
    }

    @Test
    void clearCookie_scopesToProvidedPath() {
        cookieService.clearCookie(request, response, "refresh_token", "/api/v1/auth");

        assertThat(capturedSetCookieHeader()).contains("Path=/api/v1/auth");
    }

    // --- getCookieValue ---

    @Test
    void getCookieValue_whenPresent_returnsValue() {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "abc123")});

        assertThat(cookieService.getCookieValue(request, "access_token")).isEqualTo("abc123");
    }

    @Test
    void getCookieValue_whenNoCookies_returnsNull() {
        when(request.getCookies()).thenReturn(null);

        assertThat(cookieService.getCookieValue(request, "access_token")).isNull();
    }

    @Test
    void getCookieValue_whenNameNotPresent_returnsNull() {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("other", "abc123")});

        assertThat(cookieService.getCookieValue(request, "access_token")).isNull();
    }
}
