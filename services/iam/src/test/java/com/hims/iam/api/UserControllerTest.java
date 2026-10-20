package com.hims.iam.api;

import com.hims.iam.api.dto.UserDtos.AssignRolesRequest;
import com.hims.iam.api.dto.UserDtos.CreateUserRequest;
import com.hims.iam.api.dto.UserDtos.UpdateUserRequest;
import com.hims.iam.api.dto.UserDtos.UserView;
import com.hims.iam.application.UserService;
import com.hims.iam.application.exceptions.ResourceNotFoundException;
import com.hims.iam.configuration.SecurityConfig;
import com.hims.iam.security.InMemoryTokenRevocationService;
import com.hims.iam.security.JwtAuthenticationFilter;
import com.hims.iam.security.JwtProperties;
import com.hims.iam.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for /api/v1/users/* (default-deny enforced; service + JWT filter mocked). */
@EnableConfigurationProperties(JwtProperties.class)
@WebMvcTest(com.hims.iam.api.UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class,
        InMemoryTokenRevocationService.class})
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void listUsers_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_returns201_neverEchoesPassword() throws Exception {
        when(userService.create(any(CreateUserRequest.class))).thenReturn(
                new UserView(5L, "adoctor", "a@h.in", "Dr. A", 77L, 3L, null, null, null, true, List.of(2L), List.of("staff")));

        mockMvc.perform(post("/api/v1/users")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"adoctor\",\"password\":\"Str0ngPass!x\",\"email\":\"a@h.in\",\"roleIds\":[2]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("adoctor"))
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.roles[0]").value("staff"));
    }

    @Test
    void createUser_shortPassword_mapsTo400() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"adoctor\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IAM003"));
    }

    @Test
    void getUser_unknownId_mapsTo404() throws Exception {
        when(userService.get(999L)).thenThrow(new ResourceNotFoundException("IAM006", "User not found: 999"));

        mockMvc.perform(get("/api/v1/users/999").with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("IAM006"));
    }

    @Test
    void updateUser_returns200() throws Exception {
        when(userService.update(eq(5L), any(UpdateUserRequest.class))).thenReturn(
                new UserView(5L, "adoctor", "new@h.in", "Dr. A", 77L, 3L, null, null, null, true, List.of(), List.of()));

        mockMvc.perform(put("/api/v1/users/5")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@h.in\",\"status\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@h.in"));
    }

    @Test
    void assignRoles_putSemantics_returnsUpdatedUser() throws Exception {
        when(userService.assignRoles(eq(5L), any(AssignRolesRequest.class))).thenReturn(
                new UserView(5L, "adoctor", null, null, null, 3L, null, null, null, true, List.of(1L), List.of("admin")));

        mockMvc.perform(put("/api/v1/users/5/roles")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleIds[0]").value(1))
                .andExpect(jsonPath("$.roles[0]").value("admin"));
    }
}
