package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.dto.user.UserUpdatePasswordDTO;
import com.krino.backend.dto.user.StaffCreateDTO;
import com.krino.backend.dto.user.StaffCreationResponseDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.service.ApplicationService;
import com.krino.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.krino.backend.controller.ApplicationController.getResourceResponseEntity;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PageResponse<UserResponseDTO>> getAllUsers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<UserResponseDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffCreationResponseDTO> createStaff(@Valid @RequestBody StaffCreateDTO request) {
        StaffCreationResponseDTO created = userService.createStaff(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.getUser().getId())).body(created);
    }

    @PatchMapping("/{publicId}/approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Void> approveUser(@PathVariable UUID publicId) {
        userService.approveUser(publicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getMyData(Authentication authentication) {
        CustomUserDetails customUserDetails = currentUser(authentication);
        UserResponseDTO userResponseDTO = userService.getUserResponseByPublicId(customUserDetails.getPublicId());
        return ResponseEntity.ok(userResponseDTO);
    }

    @GetMapping("/me/resume")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadMyResume(Authentication authentication) {
        CustomUserDetails customUserDetails = currentUser(authentication);
        ApplicationService.ResumeDownload resume = userService.downloadResume(customUserDetails.getPublicId());
        return getResourceResponseEntity(resume);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or #publicId == authentication.principal.publicId")
    public ResponseEntity<UserResponseDTO> getUserByPublicId(@PathVariable UUID publicId) {
        UserResponseDTO userResponseDTO = userService.getUserResponseByPublicId(publicId);
        return ResponseEntity.ok(userResponseDTO);
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or #publicId == authentication.principal.publicId")
    public ResponseEntity<UserResponseDTO> updateUserByPublicId(@PathVariable UUID publicId,
                                                                @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        UserResponseDTO updatedUser = userService.updateUserFully(publicId, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or #publicId == authentication.principal.publicId")
    public ResponseEntity<UserResponseDTO> partiallyUpdateUserByPublicId(@PathVariable UUID publicId,
                                                                         @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        UserResponseDTO updatedUser = userService.updateUserPartially(publicId, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateMyAccountFully(Authentication authentication,
                                                                @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        CustomUserDetails customUserDetails = currentUser(authentication);
        UserResponseDTO updatedUser = userService.updateUserFully(customUserDetails.getPublicId(), userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateMyAccountPartially(Authentication authentication,
                                                                    @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        CustomUserDetails customUserDetails = currentUser(authentication);
        UserResponseDTO updatedUser = userService.updateUserPartially(customUserDetails.getPublicId(), userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyAccount(Authentication authentication) {
        CustomUserDetails customUserDetails = currentUser(authentication);
        userService.deleteUserByPublicId(customUserDetails.getPublicId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN') or #publicId == authentication.principal.publicId")
    public ResponseEntity<Void> deleteUserByPublicId(@PathVariable UUID publicId) {
        userService.deleteUserByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateUserPassword(Authentication authentication, @Valid @RequestBody UserUpdatePasswordDTO userUpdateDTO) {
        CustomUserDetails customUserDetails = currentUser(authentication);
        userService.changePassword(customUserDetails.getPublicId(), userUpdateDTO);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/non-approved")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PageResponse<UserResponseDTO>> getNonApprovedUsers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<UserResponseDTO> nonApprovedUsers = userService.getNonApprovedUsers(pageable);
        return ResponseEntity.ok(nonApprovedUsers);
    }

    private CustomUserDetails currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails))
            throw new AccessDeniedException("No authenticated user");

        return customUserDetails;
    }
}
