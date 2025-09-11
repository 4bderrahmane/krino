package com.jesa.interviewslotmanager.controller;

import com.jesa.interviewslotmanager.dto.User.UserResponseDTO;
import com.jesa.interviewslotmanager.dto.User.UserUpdateDTO;
import com.jesa.interviewslotmanager.dto.User.UserUpdatePasswordDTO;
import com.jesa.interviewslotmanager.entity.User;
import com.jesa.interviewslotmanager.entity.CustomUserDetails;
import com.jesa.interviewslotmanager.service.UserService;
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
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userService.getUserById(customUserDetails.getId());
        UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        return ResponseEntity.ok(userResponseDTO);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_READ_USER') or #id == authentication.principal.id")
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
        UserResponseDTO updatedUser = userService.updateUserFully(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> partiallyUpdateUserById(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO userUpdateDTO)
    {
        UserResponseDTO updatedUser = userService.updateUserPartially(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateMyAccountFully(Authentication authentication, @Valid @RequestBody UserUpdateDTO userUpdateDTO)
    {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.updateUserFully(customUserDetails.getId(), userUpdateDTO);
        return ResponseEntity.ok("User updated successfully");
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateMyAccountPartially(Authentication authentication, @Valid @RequestBody UserUpdateDTO userUpdateDTO)
    {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.updateUserPartially(customUserDetails.getId(), userUpdateDTO);
        return ResponseEntity.ok("User updated successfully");
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyAccount(Authentication authentication)
    {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.deleteUserById(customUserDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id)
    {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated() or hasRole('ADMIN')")
    public ResponseEntity<String> updateUserPassword(Authentication authentication, @Valid @RequestBody UserUpdatePasswordDTO userUpdateDTO)
    {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.changePassword(customUserDetails.getId(), userUpdateDTO);
        return ResponseEntity.ok("User password updated successfully");
    }


    @GetMapping("/non-approved")
    public ResponseEntity<List<UserResponseDTO>> getNonApprovedUsers()
    {
        List<UserResponseDTO> nonApprovedUsers = userService.getNonApprovedUsers();
        return ResponseEntity.ok(nonApprovedUsers);
    }
}
