package com.enterprise.user_management.util;

import com.enterprise.user_management.dto.UserCreateDTO;
import com.enterprise.user_management.dto.UserResponseDTO;
import com.enterprise.user_management.entity.User;
import com.enterprise.user_management.enums.UserRole;

import java.time.LocalDateTime;

public class TestDataBuilder {

    public static UserCreateDTO createUserCreateDTO() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPhone("+1234567890");
        dto.setRole(UserRole.USER);
        return dto;
    }

    public static UserCreateDTO createAdminCreateDTO() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("adminuser");
        dto.setEmail("admin@example.com");
        dto.setPassword("admin123");
        dto.setFirstName("Admin");
        dto.setLastName("User");
        dto.setRole(UserRole.ADMIN);
        return dto;
    }

    public static User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("$2a$10$encodedPassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhone("+1234567890");
        user.setRole(UserRole.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    public static User createAdmin() {
        User user = new User();
        user.setId(2L);
        user.setUsername("adminuser");
        user.setEmail("admin@example.com");
        user.setPassword("$2a$10$encodedAdminPassword");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    public static UserResponseDTO createUserResponseDTO() {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(1L);
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPhone("+1234567890");
        dto.setRole(UserRole.USER);
        dto.setActive(true);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}