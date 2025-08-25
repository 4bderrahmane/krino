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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController
{

    private final UserService userService;
    private final ModelMapper modelMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_READ_USER')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers()
    {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveUser(@PathVariable Long id)
    {
        userService.approveUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("User approved successfully");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getMyData(Authentication authentication)
    {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.getUserById(userPrincipal.getUserId());
        UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        return ResponseEntity.ok(userResponseDTO);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_READ_USER') or #id == authentication.principal.userId")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id)
    {
        User user = userService.getUserById(id);
        UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        return ResponseEntity.ok(userResponseDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUserById(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO userUpdateDTO)
    {
        UserResponseDTO updatedUser = userService.updateUser(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateMyAccount(Authentication authentication, @Valid @RequestBody UserUpdateDTO userUpdateDTO)
    {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserResponseDTO updatedUser = userService.updateUser(userPrincipal.getUserId(), userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyAccount(Authentication authentication)
    {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        userService.deleteUserById(userPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id)
    {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{id}/change-password")
    @PreAuthorize("#id == authentication.principal.userId or hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUserPassword(@PathVariable Long id, @Valid @RequestBody UserUpdatePasswordDTO userUpdateDTO)
    {
        UserResponseDTO updatedUser = userService.changePassword(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }


    @GetMapping("/non-approved")
    public ResponseEntity<List<UserResponseDTO>> getNonApprovedUsers() {
        List<UserResponseDTO> nonApprovedUsers = userService.getNonApprovedUsers();
        return ResponseEntity.ok(nonApprovedUsers);
    }
}
