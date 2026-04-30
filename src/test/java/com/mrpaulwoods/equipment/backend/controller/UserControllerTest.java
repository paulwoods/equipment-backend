package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.entity.User;
import com.mrpaulwoods.equipment.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final String ADMIN_EMAIL = "admin@example.com";

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private User adminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .addFilter((request, response, chain) -> {
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null) {
                        var wrapper = new org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper(
                                (jakarta.servlet.http.HttpServletRequest) request, "ROLE_");
                        chain.doFilter(wrapper, response);
                    } else {
                        chain.doFilter(request, response);
                    }
                })
                .build();

        adminUser = new User();
        adminUser.setId(USER_ID);
        adminUser.setEmail(ADMIN_EMAIL);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                ADMIN_EMAIL, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        org.mockito.Mockito.lenient().when(userService.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
    }

    @Test
    void create_withValidBody_returns201() throws Exception {
        authenticateAsAdmin();
        var response = new UserCreateResponse(USER_ID, "Alice", "new@example.com", Set.of(new RoleResponse(UUID.randomUUID(), "USER")));
        given(userService.create(any(), any())).willReturn(response);

        String body = """
                {"name": "Alice", "email": "new@example.com", "password": "password123", "roles": ["USER"]}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()));
    }

    @Test
    void create_withMissingFields_returns400() throws Exception {
        String body = """
                {"email": "bad"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_returnsPagedUserList() throws Exception {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        var page = new org.springframework.data.domain.PageImpl<>(List.of(
                new UserListResponse(USER_ID, "Admin", ADMIN_EMAIL, Set.of(new RoleResponse(UUID.randomUUID(), "ADMIN")))
        ), pageable, 1);
        given(userService.findAll(any())).willReturn(page);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.content[0].name").value("Admin"))
                .andExpect(jsonPath("$.content[0].roles").isArray());
    }

    @Test
    void findById_returnsUserDetail() throws Exception {
        var detail = new UserDetailResponse(USER_ID, "Alice", "alice@example.com", Set.of(new RoleResponse(UUID.randomUUID(), "USER")));
        given(userService.findById(USER_ID)).willReturn(detail);

        mockMvc.perform(get("/api/v1/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void update_withValidBody_returns200() throws Exception {
        authenticateAsAdmin();
        UUID targetId = UUID.randomUUID();
        var response = new UserUpdateResponse(targetId, "Updated", "updated@example.com", Set.of(new RoleResponse(UUID.randomUUID(), "USER")));
        given(userService.update(any(), any(), any(), any())).willReturn(response);

        String body = """
                {"name": "Updated", "email": "updated@example.com", "roles": ["USER"]}
                """;

        mockMvc.perform(put("/api/v1/users/{id}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void delete_whenExists_returns204() throws Exception {
        authenticateAsAdmin();
        UUID targetId = UUID.randomUUID();
        doNothing().when(userService).delete(any(), any(), any());

        mockMvc.perform(delete("/api/v1/users/{id}", targetId))
                .andExpect(status().isNoContent());

        then(userService).should().delete(targetId, USER_ID, Set.of("ADMIN"));
    }

    @Test
    void delete_whenNotFound_returns404() throws Exception {
        authenticateAsAdmin();
        UUID missing = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(userService).delete(any(), any(), any());

        mockMvc.perform(delete("/api/v1/users/{id}", missing))
                .andExpect(status().isNotFound());
    }

    // --- PUT /me ---

    @Test
    void updateMe_withValidBody_returns200() throws Exception {
        authenticateAsAdmin();
        var response = new UserSelfUpdateResponse(USER_ID, "Updated", "updated@example.com", Set.of(new RoleResponse(UUID.randomUUID(), "ADMIN")));
        given(userService.updateSelf(any(), any())).willReturn(response);

        String body = """
                {"name": "Updated", "email": "updated@example.com"}
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void updateMe_withMissingFields_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMe_whenEmailConflict_returns409() throws Exception {
        authenticateAsAdmin();
        given(userService.updateSelf(any(), any()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use"));

        String body = """
                {"name": "Admin", "email": "taken@example.com"}
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // --- POST /me/password ---

    @Test
    void changePassword_withValidBody_returns204() throws Exception {
        authenticateAsAdmin();
        doNothing().when(userService).changePassword(any(), any());

        String body = """
                {"currentPassword": "oldpass123", "newPassword": "newpass123"}
                """;

        mockMvc.perform(post("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_withMissingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_whenWrongCurrentPassword_returns400() throws Exception {
        authenticateAsAdmin();
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect"))
                .when(userService).changePassword(any(), any());

        String body = """
                {"currentPassword": "wrongpass1", "newPassword": "newpass123"}
                """;

        mockMvc.perform(post("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
