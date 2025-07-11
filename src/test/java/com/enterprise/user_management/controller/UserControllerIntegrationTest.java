package com.enterprise.user_management.controller;

import com.enterprise.user_management.entity.User;
import com.enterprise.user_management.enums.UserRole;
import com.enterprise.user_management.repository.UserRepository;
import com.enterprise.user_management.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private User testUser;
    private User adminUser;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create test users
        testUser = createAndSaveUser("testuser", "test@example.com", UserRole.USER);
        adminUser = createAndSaveUser("adminuser", "admin@example.com", UserRole.ADMIN);

        // Generate tokens
        userToken = jwtUtil.generateTokenSimple(testUser.getUsername());
        adminToken = jwtUtil.generateTokenSimple(adminUser.getUsername());
    }

    private User createAndSaveUser(String username, String email, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Test
    void healthCheck_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/users/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("User Management API"));
    }

    @Test
    void getAllUsers_WithoutToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_WithValidToken_ShouldReturnUsers() throws Exception {
        mockMvc.perform(get("/api/users/all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getUserById_WithValidToken_ShouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/" + testUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void getUserById_WithValidTokenButNonExistentUser_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/users/999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserStats_WithAdminToken_ShouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/users/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists())
                .andExpect(jsonPath("$.activeUsers").exists());
    }

    @Test
    void getUserStats_WithUserToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/users/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void activateUser_WithAdminToken_ShouldSucceed() throws Exception {
        mockMvc.perform(patch("/api/users/" + testUser.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User activated successfully"));
    }

    @Test
    void activateUser_WithUserToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(patch("/api/users/" + testUser.getId() + "/activate")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_WithValidData_ShouldSucceed() throws Exception {
        String updateJson = """
            {
                "firstName": "Updated",
                "lastName": "Name",
                "email": "updated@example.com"
            }
            """;

        mockMvc.perform(put("/api/users/" + testUser.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    void deleteUser_WithValidId_ShouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }
}