package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.user.StaffCreateDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.dto.user.UserUpdatePasswordDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.InvalidCredentialsException;
import com.krino.backend.exception.IncorrectPasswordException;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.UserMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.security.PasswordGenerator;
import com.krino.backend.service.email.EmailDispatcher;
import com.krino.backend.service.email.EmailVerificationService;
import com.krino.backend.service.resume.ResumeDownload;
import com.krino.backend.service.resume.ResumeStorageService;
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SecurityUtilities;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    public static final String PUBLIC_ID = "publicId";
    public static final String EMAIL = "email";
    public static final String FIELD = "field";
    public static final String VALUE = "value";
    public static final String RESOURCE = "resource";
    private static final String EMAIL_ALREADY_TAKEN_MESSAGE = "Email '%s' is already taken.";

    private final EmailDispatcher emailDispatcher;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final SlotRepository slotRepository;
    private final ResumeStorageService resumeStorageService;
    private final PasswordGenerator passwordGenerator;

    public List<User> getAllInterviewers() {
        return userRepository.findByRolesContaining(UserRole.INTERVIEWER);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public UserResponseDTO createStaff(StaffCreateDTO request) {
        if (request.getRole() != UserRole.HR_MANAGER && request.getRole() != UserRole.INTERVIEWER) {
            throw new IllegalArgumentException("Staff role must be HR_MANAGER or INTERVIEWER.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResourceConflictException(
                    String.format(EMAIL_ALREADY_TAKEN_MESSAGE, normalizedEmail),
                    ErrorCode.DATA_CONFLICT,
                    Map.of(FIELD, EMAIL, VALUE, normalizedEmail));
        }

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        String phoneNumber = request.getPhoneNumber() == null ? null : request.getPhoneNumber().trim();
        String initialPassword = passwordGenerator.generate();

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(initialPassword))
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .isApproved(true)
                .emailVerified(true)
                .mustChangePassword(true)
                .roles(EnumSet.of(request.getRole()))
                .build();

        User savedUser = userRepository.save(user);

        // The initial password reaches the new staff member by email only;
        // it is never returned over the API.
        emailDispatcher.sendInitialPassword(savedUser.getEmail(), savedUser.getFirstName(), initialPassword);

        return userMapper.toResponse(savedUser);
    }


    @Transactional
    public User addRoleToUser(Long userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));

        user.getRoles().add(role);
        return userRepository.save(user);
    }

    public User getUserByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserResponseByPublicId(UUID publicId) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, UserRole.ADMIN, UserRole.HR_MANAGER);
        return userMapper.toResponse(getUserByPublicId(publicId));
    }

    public ResumeDownload downloadResume(UUID publicId) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, UserRole.ADMIN, UserRole.HR_MANAGER);
        User user = getUserByPublicId(publicId);
        if (!StringUtils.hasText(user.getResumeObjectKey())) {
            throw new ResourceNotFoundException("Resume file not found for this user.");
        }

        InputStream inputStream = resumeStorageService.downloadResume(user.getResumeObjectKey());
        return new ResumeDownload(
                StringUtils.hasText(user.getResumeOriginalFilename())
                        ? user.getResumeOriginalFilename()
                        : "resume.pdf",
                StringUtils.hasText(user.getResumeContentType())
                        ? user.getResumeContentType()
                        : "application/pdf",
                user.getResumeSizeBytes(),
                inputStream);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponseDTO> getAllUsers(Pageable pageable) {
        SecurityUtilities.requireAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER);
        return PageResponse.from(userRepository.findAll(pageable),
                userMapper::toResponse);
    }

    @Transactional
    public UserResponseDTO updateUserPartially(UUID publicId, UserUpdateDTO userUpdateDTO) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, UserRole.ADMIN, UserRole.HR_MANAGER);
        User currentUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        requireAdministratorPrivilegeForAdminTarget(currentUser);
        requireCurrentVersion(userUpdateDTO, currentUser);

        String normalizedEmail = null;
        boolean emailChanged = false;
        if (userUpdateDTO.getEmail() != null) {
            normalizedEmail = normalizeEmail(userUpdateDTO.getEmail());
            emailChanged = !normalizedEmail.equals(currentUser.getEmail());
            if (emailChanged) {
                String emailToValidate = normalizedEmail;
                userRepository.findByEmail(emailToValidate)
                        .filter(user -> !user.getPublicId().equals(publicId))
                        .ifPresent(user -> {
                            throw new ResourceConflictException(String.format(EMAIL_ALREADY_TAKEN_MESSAGE,
                                    emailToValidate), ErrorCode.DATA_CONFLICT,
                                    Map.of(FIELD, EMAIL, VALUE, emailToValidate));
                        });
            }
        }

        userMapper.patchEntity(userUpdateDTO, normalizedEmail, currentUser);
        if (emailChanged) {
            requireVerificationOfNewEmail(currentUser);
        }

        User updatedPartially = userRepository.saveAndFlush(currentUser);
        return userMapper.toResponse(updatedPartially);
    }

    @Transactional
    public UserResponseDTO updateUserFully(UUID publicId, UserUpdateDTO userUpdateDTO) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, UserRole.ADMIN, UserRole.HR_MANAGER);
        User existingUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        requireAdministratorPrivilegeForAdminTarget(existingUser);
        requireCurrentVersion(userUpdateDTO, existingUser);

        if (userUpdateDTO.getEmail() == null
                || userUpdateDTO.getFirstName() == null
                || userUpdateDTO.getLastName() == null
                || userUpdateDTO.getPhoneNumber() == null) {
            throw new IllegalArgumentException("All fields must be provided for a full update.");
        }

        String normalizedEmail = normalizeEmail(userUpdateDTO.getEmail());
        boolean emailChanged = !normalizedEmail.equals(existingUser.getEmail());

        if (emailChanged) {
            userRepository.findByEmail(normalizedEmail)
                    .filter(user -> !user.getPublicId().equals(publicId))
                    .ifPresent(user -> {
                        throw new ResourceConflictException(
                                String.format(EMAIL_ALREADY_TAKEN_MESSAGE,
                                        normalizedEmail), ErrorCode.DATA_CONFLICT,
                                Map.of(FIELD, EMAIL, VALUE, normalizedEmail));
                    });
        }

        userMapper.updateEntity(userUpdateDTO, normalizedEmail, existingUser);
        if (emailChanged) requireVerificationOfNewEmail(existingUser);

        User updatedUser = userRepository.saveAndFlush(existingUser);
        return userMapper.toResponse(updatedUser);
    }

    @Transactional
    public void changePassword(UUID publicId, UserUpdatePasswordDTO passwordChangeDTO) {
        SecurityUtilities.requireCurrentUser(publicId);
        User existingUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        if (!passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), existingUser.getPassword())) {
            throw new IncorrectPasswordException("Current password is not correct.");
        }

        if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmNewPassword())) {
            throw new InvalidCredentialsException("New password and confirmation do not match.");
        }

        if (existingUser.isMustChangePassword()
                && passwordEncoder.matches(passwordChangeDTO.getNewPassword(), existingUser.getPassword())) {
            throw new IllegalArgumentException("The new password must differ from the initial password.");
        }

        existingUser.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));
        existingUser.setMustChangePassword(false);

        userRepository.save(existingUser);
        refreshTokenRepository.revokeAllUserTokens(existingUser.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteUserByPublicId(UUID publicId) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, UserRole.ADMIN);
        User userToDelete = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        requireAdministratorPrivilegeForAdminTarget(userToDelete);
        requireAnotherAdministratorSurvives(userToDelete, "deleted");

        // Applications, interviews and slots keep the recruitment history; deleting a
        // user that owns any of them would orphan those records (FK violation). A candidate's
        // interviews hang off their applications, so the application check already covers them.
        if (applicationRepository.existsByCandidate(userToDelete)
                || interviewRepository.existsByInterviewer(userToDelete)
                || slotRepository.existsByInterviewer(userToDelete)) {
            throw new ResourceConflictException(
                    "User has applications, interviews or slots and cannot be deleted.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(RESOURCE, "User"));
        }

        refreshTokenRepository.deleteAllByUserId(userToDelete.getId());
        userRepository.delete(userToDelete);
        if (StringUtils.hasText(userToDelete.getResumeObjectKey())) {
            resumeStorageService.deleteResumeAfterCommit(userToDelete.getResumeObjectKey());
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void setApproval(UUID publicId, boolean approved) {
        SecurityUtilities.requireAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        requireAdministratorPrivilegeForAdminTarget(user);

        // Reactivating is always safe; only the deactivating direction can lock somebody out.
        if (!approved) {
            rejectSelfDeactivation(user);
            requireAnotherAdministratorSurvives(user, "deactivated");
        }

        user.setApproved(approved);

        if (!approved) refreshTokenRepository.deleteAllByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponseDTO> getNonApprovedUsers(Pageable pageable) {
        SecurityUtilities.requireAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER);
        return PageResponse.from(userRepository.findByIsApprovedFalse(pageable),
                userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));
        return new CustomUserDetails(user);
    }

    private void requireAdministratorPrivilegeForAdminTarget(User target) {
        if (isAdministrator(target) && !SecurityUtilities.hasAnyRole(UserRole.ADMIN)) {
            throw new AccessDeniedException("Only an administrator can manage an administrator account.");
        }
    }

    // this is basically so we don't left with no Admin to administrate the app.
    private void requireAnotherAdministratorSurvives(User target, String attemptedAction) {
        if (!isAdministrator(target)) {
            return;
        }

        long remainingAdministrators = userRepository.countByRolesContainingAndIsApprovedTrueAndIdNot(UserRole.ADMIN,
                target.getId());
        if (remainingAdministrators == 0) {
            throw new ResourceConflictException(
                    "The last active administrator cannot be " + attemptedAction + ". Grant the ADMIN role to another" +
                            " account first.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(RESOURCE, "User"));
        }
    }

    private void rejectSelfDeactivation(User target) {
        boolean actingOnSelf = SecurityUtilities.getCurrentUserId()
                .map(currentUserId -> currentUserId.equals(target.getId()))
                .orElse(false);

        if (actingOnSelf) {
            throw new ResourceConflictException(
                    "You cannot deactivate your own account.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(RESOURCE, "User"));
        }
    }

    private static boolean isAdministrator(User user) {
        return user.hasRole(UserRole.ADMIN);
    }

    private static void requireCurrentVersion(UserUpdateDTO request, User user) {
        if (request.getVersion() == null) {
            throw new IllegalArgumentException("Profile version is required.");
        }
        if (request.getVersion() != user.getVersion()) {
            throw new ResourceConflictException("Profile changed since it was loaded.", ErrorCode.STALE_PROFILE);
        }
    }

    private void requireVerificationOfNewEmail(User user) {
        user.setEmailVerified(false);
        emailVerificationService.sendVerificationEmail(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
