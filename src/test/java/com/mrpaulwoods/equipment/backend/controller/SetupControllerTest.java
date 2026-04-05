package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.model.RefreshToken;
import com.mrpaulwoods.equipment.backend.model.Role;
import com.mrpaulwoods.equipment.backend.model.User;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import com.mrpaulwoods.equipment.backend.service.JwtService;
import com.mrpaulwoods.equipment.backend.service.RefreshTokenService;
import com.mrpaulwoods.equipment.backend.service.UserDetailsServiceImpl;
import com.mrpaulwoods.equipment.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SetupControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private SetupController setupController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(setupController).build();
    }

    @Test
    void status_whenNoUsers_returnsSetupRequired() throws Exception {
        when(userRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true));
    }

    @Test
    void status_whenUsersExist_returnsSetupNotRequired() throws Exception {
        when(userRepository.count()).thenReturn(1L);

        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(false));
    }

    @Test
    void setup_whenNoUsers_createsAdminAndReturnsEmail() throws Exception {
        when(userRepository.count()).thenReturn(0L);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setRole(Role.ADMIN);
        when(userService.createInternal("admin@example.com", "secret", Role.ADMIN)).thenReturn(user);

        UserDetails ud = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "hashed", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(ud);
        when(jwtService.generateToken(ud)).thenReturn("access-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

        String body = """
                {"email": "admin@example.com", "password": "secret"}
                """;

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void setup_whenUsersExist_returns409() throws Exception {
        when(userRepository.count()).thenReturn(1L);

        String body = """
                {"email": "another@example.com", "password": "secret"}
                """;

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void setup_withInvalidBody_returns400() throws Exception {
        String body = """
                {"email": "not-an-email", "password": ""}
                """;

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
