package com.enterprise.user_management.controller;

import com.enterprise.user_management.dto.*;
import com.enterprise.user_management.security.JwtUtil;
import com.enterprise.user_management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "Authentication and authorization operations including login, registration, token validation and refresh")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Operation(
            summary = "User login",
            description = "Authenticates a user with username/email and password, returns JWT token and user information upon successful authentication"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful - JWT token and user data returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input format or missing required fields"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid username/email or password"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User account is deactivated or suspended"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too Many Requests - Rate limit exceeded for login attempts"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Parameter(
                    description = "Login credentials containing username/email and password",
                    required = true
            )
            @Valid @RequestBody AuthRequest authRequest) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            // Load user details
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());

            // Generate JWT token
            final String jwt = jwtUtil.generateToken(userDetails);

            // Get user information
            final UserResponseDTO user = userService.getUserByUsername(authRequest.getUsername());

            return ResponseEntity.ok(new AuthResponse(jwt, user));

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password", e);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Invalid username or password");
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication failed");
        }
    }

    @Operation(
            summary = "User registration",
            description = "Creates a new user account and automatically logs them in, returning JWT token and user information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration successful - User created and JWT token returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input data or validation errors"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Username or email already exists"
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Password doesn't meet security requirements"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Parameter(
                    description = "User registration data including username, email, password, and personal information",
                    required = true
            )
            @Valid @RequestBody UserCreateDTO userCreateDTO) {
        // Create new user
        UserResponseDTO user = userService.createUser(userCreateDTO);

        // Load user details for JWT generation
        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        // Generate JWT token
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt, user));
    }

    @Operation(
            summary = "Validate JWT token",
            description = "Validates a JWT token and returns token status, username, and expiration information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token validation completed - Check 'valid' field in response",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid token format or missing Authorization header"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Token is expired, malformed, or invalid"
            )
    })
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @Parameter(
                    description = "Authorization header with Bearer token",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            boolean isValid = jwtUtil.validateToken(token, userDetails);

            response.put("valid", isValid);
            response.put("username", username);
            response.put("expiresAt", jwtUtil.extractExpiration(token));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", "Invalid token");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
            summary = "Refresh JWT token",
            description = "Generates a new JWT token using a valid existing token. Extends the user's session without requiring re-authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully - New JWT token returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid token format or missing Authorization header"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Token is expired, invalid, or user no longer exists"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User account has been deactivated since token was issued"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Parameter(
                    description = "Authorization header with Bearer token to be refreshed",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(token, userDetails)) {
                String newToken = jwtUtil.generateToken(userDetails);
                UserResponseDTO user = userService.getUserByUsername(username);

                return ResponseEntity.ok(new AuthResponse(newToken, user));
            } else {
                throw new BadCredentialsException("Invalid or expired token");
            }

        } catch (Exception e) {
            throw new BadCredentialsException("Cannot refresh token", e);
        }
    }

    @Operation(
            summary = "Logout user (token blacklist)",
            description = "Logs out a user by blacklisting their current JWT token. The token will no longer be valid for authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully logged out - Token blacklisted"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid token format or missing Authorization header"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or expired token"
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Parameter(
                    description = "Authorization header with Bearer token to be blacklisted",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // Add token to blacklist (implement this in your security service)
            // tokenBlacklistService.blacklistToken(token);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully logged out");
            response.put("timestamp", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to logout");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
            summary = "Check authentication status",
            description = "Checks if the current user is authenticated and returns their basic information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User is authenticated - Returns user information",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User is not authenticated or token is invalid"
            )
    })
    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserResponseDTO> getCurrentUser(
            @Parameter(
                    description = "Authorization header with Bearer token",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            UserResponseDTO user = userService.getUserByUsername(username);

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            throw new BadCredentialsException("Invalid authentication token");
        }
    }

    @Operation(
            summary = "Forgot password",
            description = "Initiates password reset process by sending reset instructions to user's email"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset instructions sent to email"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid email format"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - No user found with the provided email"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too Many Requests - Password reset rate limit exceeded"
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Parameter(
                    description = "Email address for password reset",
                    required = true,
                    example = "user@example.com"
            )
            @RequestParam String email) {

        // Implement password reset logic here
        // passwordResetService.initiatePasswordReset(email);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset instructions sent to your email");
        response.put("email", email);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reset password",
            description = "Resets user password using a valid reset token received via email"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid password format or token format"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or expired reset token"
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - New password doesn't meet security requirements"
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Parameter(description = "Password reset token received via email", required = true)
            @RequestParam String token,
            @Parameter(description = "New password", required = true)
            @RequestParam String newPassword) {

        // Implement password reset logic here
        // passwordResetService.resetPassword(token, newPassword);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully");

        return ResponseEntity.ok(response);
    }
}