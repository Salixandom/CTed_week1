package com.enterprise.user_management.service;

import com.enterprise.user_management.dto.UserCreateDTO;
import com.enterprise.user_management.dto.UserResponseDTO;
import com.enterprise.user_management.dto.UserUpdateDTO;
import com.enterprise.user_management.enums.UserRole;
import com.enterprise.user_management.dto.PasswordChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    UserResponseDTO createUser(UserCreateDTO userCreateDTO);
    UserResponseDTO getUserById(Long id);
    UserResponseDTO getUserByUsername(String username);
    List<UserResponseDTO> getAllUsers();
    Page<UserResponseDTO> getAllUsers(Pageable pageable);
    UserResponseDTO updateUser(Long id, UserUpdateDTO userUpdateDTO);
    void deleteUser(Long id);
    void activateUser(Long id);
    void deactivateUser(Long id);
    List<UserResponseDTO> getUsersByRole(UserRole role);
    Page<UserResponseDTO> searchUsers(String search, Pageable pageable);
    long getTotalUserCount();
    long getActiveUserCount();
    long getUserCountByRole(UserRole role);
    UserResponseDTO changePassword(String username, PasswordChangeRequest request);
}
