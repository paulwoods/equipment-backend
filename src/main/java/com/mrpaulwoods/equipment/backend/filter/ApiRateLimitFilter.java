package com.mrpaulwoods.equipment.backend.filter;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.exception.ProblemDetails;
import com.mrpaulwoods.equipment.backend.ratelimit.WindowedCounter;
import com.mrpaulwoods.equipment.backend.ratelimit.WindowedCounterFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class ApiRateLimitFilter implements Filter {

    private static final List<String> SKIP_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/version",
            "/actuator/"
    );

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final WindowedCounter<String> requests;

    public ApiRateLimitFilter(AppProperties appProperties, ObjectMapper objectMapper,
                              WindowedCounterFactory counterFactory) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.requests = counterFactory.create(
                Duration.ofMillis(appProperties.getApiRateLimitWindowMs()));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = httpRequest.getRemoteAddr();
        int current = requests.increment(clientIp);

        if (current > appProperties.getApiRateLimitMaxRequests()) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ProblemDetail problem = ProblemDetails.of(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many requests", "Rate Limit Exceeded", path);
            objectMapper.writeValue(httpResponse.getWriter(), problem);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        for (String skip : SKIP_PATHS) {
            if (path.startsWith(skip)) {
                return true;
            }
        }
        return false;
    }
}
