package com.enterprise.user_management.service;

import com.enterprise.user_management.dto.UserCreateDTO;
import com.enterprise.user_management.dto.UserResponseDTO;
import com.enterprise.user_management.entity.User;
import com.enterprise.user_management.exception.DuplicateResourceException;
import com.enterprise.user_management.exception.ResourceNotFoundException;
import com.enterprise.user_management.repository.UserRepository;
import com.enterprise.user_management.service.impl.UserServiceImpl;
import com.enterprise.user_management.util.TestDataBuilder;
import com.enterprise.user_management.dto.UserUpdateDTO;
import com.enterprise.user_management.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserCreateDTO userCreateDTO;
    private User user;

    @BeforeEach
    void setUp() {
        userCreateDTO = TestDataBuilder.createUserCreateDTO();
        user = TestDataBuilder.createUser();
    }

    @Test
    void createUser_Success() {
        // Given
        when(userRepository.existsByUsername(userCreateDTO.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(userCreateDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(userCreateDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserResponseDTO result = userService.createUser(userCreateDTO);

        // Then
        assertNotNull(result);
        assertEquals(userCreateDTO.getUsername(), result.getUsername());
        assertEquals(userCreateDTO.getEmail(), result.getEmail());
        assertEquals(userCreateDTO.getRole(), result.getRole());
        assertTrue(result.getActive());

        verify(userRepository).existsByUsername(userCreateDTO.getUsername());
        verify(userRepository).existsByEmail(userCreateDTO.getEmail());
        verify(passwordEncoder).encode(userCreateDTO.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateUsername_ThrowsException() {
        // Given
        when(userRepository.existsByUsername(userCreateDTO.getUsername())).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(userCreateDTO)
        );

        assertEquals("Username already exists: " + userCreateDTO.getUsername(), exception.getMessage());
        verify(userRepository).existsByUsername(userCreateDTO.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        UserResponseDTO result = userService.getUserById(1L);

        // Then
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(1L)
        );

        assertEquals("User not found with id: 1", exception.getMessage());
        verify(userRepository).findById(1L);
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        // Given
        when(userRepository.existsByUsername(userCreateDTO.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(userCreateDTO.getEmail())).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(userCreateDTO)
        );

        assertEquals("Email already exists: " + userCreateDTO.getEmail(), exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        UserResponseDTO result = userService.getUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_NotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserByUsername("nonexistent")
        );

        assertEquals("User not found with username: nonexistent", exception.getMessage());
    }

    @Test
    void getAllUsers_Success() {
        // Given
        List<User> users = Arrays.asList(user, TestDataBuilder.createAdmin());
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserResponseDTO> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_WithPagination_Success() {
        // Given
        List<User> users = Arrays.asList(user);
        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<UserResponseDTO> result = userService.getAllUsers(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void updateUser_Success() {
        // Given
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("Name");
        updateDTO.setEmail("updated@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserResponseDTO result = userService.updateUser(1L, updateDTO);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_DuplicateEmail_ThrowsException() {
        // Given
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("duplicate@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class, () -> userService.updateUser(1L, updateDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_NotFound_ThrowsException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void activateUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.activateUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deactivateUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.deactivateUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUsersByRole_Success() {
        // Given
        List<User> adminUsers = Arrays.asList(TestDataBuilder.createAdmin());
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(adminUsers);

        // When
        List<UserResponseDTO> result = userService.getUsersByRole(UserRole.ADMIN);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findByRole(UserRole.ADMIN);
    }

    @Test
    void getTotalUserCount_Success() {
        // Given
        when(userRepository.count()).thenReturn(5L);

        // When
        long result = userService.getTotalUserCount();

        // Then
        assertEquals(5L, result);
        verify(userRepository).count();
    }
}

