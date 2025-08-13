package com.InterviewManager.interview_slot_manager.controller;

import com.InterviewManager.interview_slot_manager.DTO.User.UserRegistrationDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserResponseDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserUpdateDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserUpdatePasswordDTO;
import com.InterviewManager.interview_slot_manager.entity.User;
import com.InterviewManager.interview_slot_manager.entity.UserPrincipal;
import com.InterviewManager.interview_slot_manager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        User user = userService.registerUser(registrationDTO);
        UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        return ResponseEntity.ok(userResponseDTO);
    }


    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long userId, @RequestBody UserUpdateDTO userUpdateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Long authenticatedUserId = userPrincipal.getUser().getId();

        boolean isUpdatingOwnProfile = userId.equals(authenticatedUserId);

        boolean hasAdminPrivileges = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isUpdatingOwnProfile && !hasAdminPrivileges) {
            throw new AccessDeniedException("You are not allowed to update other users' profiles.");
        }

        UserResponseDTO updatedUser = userService.updateUser(userId, userUpdateDTO);

        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/updateUser/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponseDTO> updateeUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        UserResponseDTO updatedUser = userService.updateUser(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/change-password")
    public ResponseEntity<UserResponseDTO> updateUserPassword(@PathVariable Long id, @Valid @RequestBody UserUpdatePasswordDTO userUpdateDTO) {
        UserResponseDTO updatedUser = userService.changePassword(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
