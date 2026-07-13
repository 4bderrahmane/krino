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
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SecurityUtilities;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {
    public static final String PUBLIC_ID = "publicId";
    public static final String EMAIL = "email";
    public static final String FIELD = "field";
    public static final String VALUE = "value";
    private static final String ADMIN = "ADMIN";
    private static final String HR_MANAGER = "HR_MANAGER";
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
    private final CvStorageService cvStorageService;
    private final PasswordGenerator passwordGenerator;

    public List<User> getAllInterviewers() {
        return userRepository.findByRolesContaining(UserRole.INTERVIEWER);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserResponseDTO createStaff(StaffCreateDTO request) {
        if (request.getRole() != UserRole.HR_MANAGER && request.getRole() != UserRole.INTERVIEWER) {
            throw new IllegalArgumentException("Staff role must be HR_MANAGER or INTERVIEWER.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResourceConflictException(String.format(EMAIL_ALREADY_TAKEN_MESSAGE, normalizedEmail),
                    ErrorCode.DATA_CONFLICT,
                    Map.of(FIELD, EMAIL, VALUE, normalizedEmail));
        }

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        String phoneNumber = request.getPhoneNumber() == null ? null : request.getPhoneNumber().trim();
        String initialPassword = passwordGenerator.generate();

        User user = new User(normalizedEmail, passwordEncoder.encode(initialPassword), firstName, lastName, phoneNumber);
        user.addRole(request.getRole());
        user.setApproved(true);
        // Receiving the emailed initial password already proves inbox ownership, so staff
        // accounts skip the explicit verification link.
        user.setEmailVerified(true);
        user.setMustChangePassword(true);

        User savedUser = userRepository.save(user);

        // The initial password reaches the new staff member by email only; it is never
        // returned over the API.
        emailDispatcher.sendInitialPassword(savedUser.getEmail(), savedUser.getFirstName(), initialPassword);

        return userMapper.toResponse(savedUser);
    }


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

    public UserResponseDTO getUserResponseByPublicId(UUID publicId) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, ADMIN, HR_MANAGER);
        return userMapper.toResponse(getUserByPublicId(publicId));
    }

    public ApplicationService.ResumeDownload downloadResume(UUID publicId) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, ADMIN, HR_MANAGER);
        User user = getUserByPublicId(publicId);
        if (!StringUtils.hasText(user.getResumeObjectKey())) {
            throw new ResourceNotFoundException("Resume file not found for this user.");
        }

        InputStream inputStream = cvStorageService.downloadResume(user.getResumeObjectKey());
        return new ApplicationService.ResumeDownload(
                StringUtils.hasText(user.getResumeOriginalFilename())
                        ? user.getResumeOriginalFilename()
                        : "resume.pdf",
                StringUtils.hasText(user.getResumeContentType())
                        ? user.getResumeContentType()
                        : "application/pdf",
                user.getResumeSizeBytes(),
                inputStream);
    }

    public PageResponse<UserResponseDTO> getAllUsers(Pageable pageable) {
        SecurityUtilities.requireAnyRole(ADMIN, HR_MANAGER);
        return PageResponse.from(userRepository.findAll(pageable),
                userMapper::toResponse);
    }

    public UserResponseDTO updateUserPartially(UUID publicId, UserUpdateDTO userUpdateDTO) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, ADMIN, HR_MANAGER);
        User currentUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        String normalizedEmail = null;
        boolean emailChanged = false;
        if (userUpdateDTO.getEmail() != null) {
            normalizedEmail = normalizeEmail(userUpdateDTO.getEmail());
            emailChanged = !normalizedEmail.equals(currentUser.getEmail());
            if (emailChanged) {
                String emailToValidate = normalizedEmail;
                userRepository.findByEmail(emailToValidate)
                        .filter(user -> !user.getPublicId().equals(publicId))
                        .ifPresent(user ->
                        {
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

        User updatedPartially = userRepository.save(currentUser);
        return userMapper.toResponse(updatedPartially);
    }

    public UserResponseDTO updateUserFully(UUID publicId, UserUpdateDTO userUpdateDTO) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, ADMIN, HR_MANAGER);
        User existingUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        if (userUpdateDTO.getEmail() == null
                || userUpdateDTO.getFirstName() == null || userUpdateDTO.getLastName() == null
                || userUpdateDTO.getPhoneNumber() == null) {
            throw new IllegalArgumentException("All fields must be provided for a full update.");
        }

        String normalizedEmail = normalizeEmail(userUpdateDTO.getEmail());
        boolean emailChanged = !normalizedEmail.equals(existingUser.getEmail());

        if (emailChanged) {
            userRepository.findByEmail(normalizedEmail)
                    .filter(user -> !user.getPublicId().equals(publicId))
                    .ifPresent(user ->
                    {
                        throw new ResourceConflictException(String.format(EMAIL_ALREADY_TAKEN_MESSAGE,
                                normalizedEmail), ErrorCode.DATA_CONFLICT,
                                Map.of(FIELD, EMAIL, VALUE, normalizedEmail));
                    });
        }

        userMapper.updateEntity(userUpdateDTO, normalizedEmail, existingUser);
        if (emailChanged) {
            requireVerificationOfNewEmail(existingUser);
        }

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toResponse(updatedUser);
    }

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

        existingUser.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));
        // They've chosen their own password — the initial-password reminder is done.
        existingUser.setMustChangePassword(false);

        userRepository.save(existingUser);
    }

    public void deleteUserByPublicId(UUID publicId) {
        SecurityUtilities.requireCurrentUserOrAnyRole(publicId, ADMIN);
        User userToDelete = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        // Applications, interviews and slots keep the recruitment history; deleting a
        // user that owns any of them would orphan those records (FK violation). A candidate's
        // interviews hang off their applications, so the application check already covers them.
        if (applicationRepository.existsByCandidate(userToDelete)
                || interviewRepository.existsByInterviewer(userToDelete)
                || slotRepository.existsByInterviewer(userToDelete)) {
            throw new ResourceConflictException(
                    "User has applications, interviews or slots and cannot be deleted.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "User"));
        }

        refreshTokenRepository.deleteAllByUserId(userToDelete.getId());
        userRepository.delete(userToDelete);
    }

    public void setApproval(UUID publicId, boolean approved) {
        SecurityUtilities.requireAnyRole(ADMIN, HR_MANAGER);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, publicId));

        user.setApproved(approved);

        if (!approved) refreshTokenRepository.deleteAllByUserId(user.getId());
    }

    public PageResponse<UserResponseDTO> getNonApprovedUsers(Pageable pageable) {
        SecurityUtilities.requireAnyRole(ADMIN, HR_MANAGER);
        return PageResponse.from(userRepository.findByIsApprovedFalse(pageable),
                userMapper::toResponse);
    }

    public CustomUserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));
        return new CustomUserDetails(user);
    }

    /**
     * A changed email address is as unproven as at registration: drop the verified flag and
     * send a fresh verification link to the new address. Sessions issued before the change
     * keep working (token refresh does not re-check the flag), but the next password login is
     * blocked until the link is used — so a mistyped address can still be corrected from the
     * surviving session instead of locking the account out.
     */
    private void requireVerificationOfNewEmail(User user) {
        user.setEmailVerified(false);
        emailVerificationService.sendVerificationEmail(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
