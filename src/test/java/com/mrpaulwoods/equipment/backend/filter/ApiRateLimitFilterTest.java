package com.mrpaulwoods.equipment.backend.filter;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.ratelimit.WindowedCounterFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiRateLimitFilterTest {

    private AppProperties appProperties;
    private AtomicLong nanos;
    private ApiRateLimitFilter filter;
    private FilterChain chain;
    private HttpServletResponse response;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        appProperties = new AppProperties();
        appProperties.setApiRateLimitMaxRequests(3);
        appProperties.setApiRateLimitWindowMs(60_000L);
        nanos = new AtomicLong();
        Ticker ticker = nanos::get;
        filter = new ApiRateLimitFilter(appProperties, new tools.jackson.databind.ObjectMapper(),
                new WindowedCounterFactory(ticker));

        chain = mock(FilterChain.class);
        response = mock(HttpServletResponse.class);
        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    private HttpServletRequest requestFor(String path, String ip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getRemoteAddr()).thenReturn(ip);
        return request;
    }

    @Test
    void doFilter_belowLimit_passesThroughEachTime() throws Exception {
        for (int i = 0; i < 3; i++) {
            filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), response, chain);
        }

        verify(chain, org.mockito.Mockito.times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_incrementThenCheck_blocksOnceCountExceedsMax() throws Exception {
        // max = 3: the 4th increment yields count 4, which is > 3 -> blocked.
        for (int i = 0; i < 3; i++) {
            filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), response, chain);
        }

        filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), response, chain);

        verify(chain, org.mockito.Mockito.times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(response).setStatus(429);
    }

    @Test
    void doFilter_exactlyAtMax_isNotBlocked() throws Exception {
        // increment(key) > max is the blocking condition, so a count equal to max must pass.
        appProperties.setApiRateLimitMaxRequests(1);
        filter = new ApiRateLimitFilter(appProperties, new tools.jackson.databind.ObjectMapper(),
                new WindowedCounterFactory(nanos::get));

        filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), response, chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_oneOverMax_isBlocked() throws Exception {
        appProperties.setApiRateLimitMaxRequests(1);
        filter = new ApiRateLimitFilter(appProperties, new tools.jackson.databind.ObjectMapper(),
                new WindowedCounterFactory(nanos::get));

        filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), response, chain);
        filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), response, chain);

        verify(chain, org.mockito.Mockito.times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(response).setStatus(429);
        assertThat(responseBody.toString()).contains("Rate Limit Exceeded");
    }

    @Test
    void doFilter_countsAreIsolatedPerClientIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), response, chain);
        }
        // Different IP starts its own window and should not be blocked yet.
        filter.doFilter(requestFor("/api/v1/equipment", "9.9.9.9"), response, chain);

        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_blockExpiresAfterWindow() throws Exception {
        for (int i = 0; i < 4; i++) {
            filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), response, chain);
        }
        verify(response).setStatus(429);

        nanos.addAndGet(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(60_001L));

        HttpServletResponse secondResponse = mock(HttpServletResponse.class);
        filter.doFilter(requestFor("/api/v1/equipment", "1.2.3.4"), secondResponse, chain);

        verify(secondResponse, never()).setStatus(429);
    }

    @Test
    void doFilter_skipsRateLimitingForLoginPath() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(requestFor("/api/v1/auth/login", "1.2.3.4"), response, chain);
        }

        verify(chain, org.mockito.Mockito.times(5)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_skipsRateLimitingForRefreshPath() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(requestFor("/api/v1/auth/refresh", "1.2.3.4"), response, chain);
        }

        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_skipsRateLimitingForActuatorPaths() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(requestFor("/actuator/health", "1.2.3.4"), response, chain);
        }

        verify(response, never()).setStatus(429);
    }
}
