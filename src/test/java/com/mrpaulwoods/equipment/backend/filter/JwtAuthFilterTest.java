package com.mrpaulwoods.equipment.backend.filter;

import com.mrpaulwoods.equipment.backend.service.JwtService;
import com.mrpaulwoods.equipment.backend.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldNotFilter_returnsTrueForLoginPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/api/auth/login");
        assertTrue(jwtAuthFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_returnsTrueForRefreshPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/api/auth/refresh");
        assertTrue(jwtAuthFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_returnsTrueForLogoutPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/api/auth/logout");
        assertTrue(jwtAuthFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_returnsFalseForMePath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/api/auth/me");
        assertFalse(jwtAuthFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_returnsFalseForEquipmentPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/api/equipment");
        assertFalse(jwtAuthFilter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_skipsAuthWhenNoCookies() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getCookies()).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }
}
