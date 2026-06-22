package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.dto.user.UserUpdatePasswordDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_READ_USER')")
    public ResponseEntity<PageResponse<UserResponseDTO>> getAllUsers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<UserResponseDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{publicId}/approval")
    @PreAuthorize("hasAuthority('CAN_APPROVE_USER')")
    public ResponseEntity<Void> approveUser(@PathVariable UUID publicId) {
        userService.approveUser(publicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getMyData(Authentication authentication) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        UserResponseDTO userResponseDTO = userService.getUserResponseByPublicId(customUserDetails.getPublicId());
        return ResponseEntity.ok(userResponseDTO);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_READ_USER') or #publicId == authentication.principal.publicId")
    public ResponseEntity<UserResponseDTO> getUserByPublicId(@PathVariable UUID publicId) {
        UserResponseDTO userResponseDTO = userService.getUserResponseByPublicId(publicId);
        return ResponseEntity.ok(userResponseDTO);
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_USER')")
    public ResponseEntity<UserResponseDTO> updateUserByPublicId(@PathVariable UUID publicId,
                                                                @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        UserResponseDTO updatedUser = userService.updateUserFully(publicId, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_USER')")
    public ResponseEntity<UserResponseDTO> partiallyUpdateUserByPublicId(@PathVariable UUID publicId,
                                                                         @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        UserResponseDTO updatedUser = userService.updateUserPartially(publicId, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateMyAccountFully(Authentication authentication,
                                                                @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        UserResponseDTO updatedUser = userService.updateUserFully(customUserDetails.getPublicId(), userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateMyAccountPartially(Authentication authentication,
                                                                    @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        UserResponseDTO updatedUser = userService.updateUserPartially(customUserDetails.getPublicId(), userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyAccount(Authentication authentication) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.deleteUserByPublicId(customUserDetails.getPublicId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_DELETE_USER')")
    public ResponseEntity<Void> deleteUserByPublicId(@PathVariable UUID publicId) {
        userService.deleteUserByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated() or hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserPassword(Authentication authentication,
                                                   @Valid @RequestBody UserUpdatePasswordDTO userUpdateDTO) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.changePassword(customUserDetails.getPublicId(), userUpdateDTO);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/non-approved")
    @PreAuthorize("hasAuthority('CAN_APPROVE_USER')")
    public ResponseEntity<PageResponse<UserResponseDTO>> getNonApprovedUsers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<UserResponseDTO> nonApprovedUsers = userService.getNonApprovedUsers(pageable);
        return ResponseEntity.ok(nonApprovedUsers);
    }
}
