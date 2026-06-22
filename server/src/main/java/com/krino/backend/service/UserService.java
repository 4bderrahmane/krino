package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
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
import com.krino.backend.utility.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService
{
    private static final String EMAIL_ALREADY_TAKEN_MESSAGE = "Email '%s' is already taken.";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final SlotRepository slotRepository;

    public List<User> getAllInterviewers()
    {
        return userRepository.findByRolesContaining(UserRole.INTERVIEWER);
    }

    public Optional<User> findByEmail(String email)
    {
        return userRepository.findByEmail(email);
    }

    public User addRoleToUser(Long userId, UserRole role)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));

        user.getRoles().add(role);
        return userRepository.save(user);
    }

    public User getUserByPublicId(UUID publicId)
    {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));
    }

    public UserResponseDTO getUserResponseByPublicId(UUID publicId)
    {
        return userMapper.toResponse(getUserByPublicId(publicId));
    }

    public PageResponse<UserResponseDTO> getAllUsers(Pageable pageable)
    {
        return PageResponse.from(userRepository.findAll(pageable),
                userMapper::toResponse);
    }

    public UserResponseDTO updateUserPartially(UUID publicId, UserUpdateDTO userUpdateDTO)
    {
        User currentUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));

        String normalizedEmail = null;
        if (userUpdateDTO.getEmail() != null)
        {
            normalizedEmail = normalizeEmail(userUpdateDTO.getEmail());
            if (!normalizedEmail.equals(currentUser.getEmail()))
            {
                String emailToValidate = normalizedEmail;
                userRepository.findByEmail(emailToValidate)
                        .filter(user -> !user.getPublicId().equals(publicId))
                        .ifPresent(user ->
                        {
                            throw new ResourceConflictException(String.format(EMAIL_ALREADY_TAKEN_MESSAGE, emailToValidate), ErrorCode.DATA_CONFLICT,
                                    Map.of("field", "email", "value", emailToValidate));
                        });
            }
        }

        userMapper.patchEntity(userUpdateDTO, normalizedEmail, currentUser);

        User updatedPartially = userRepository.save(currentUser);
        return userMapper.toResponse(updatedPartially);
    }

    public UserResponseDTO updateUserFully(UUID publicId, UserUpdateDTO userUpdateDTO)
    {
        User existingUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));

        if (userUpdateDTO.getEmail() == null
                || userUpdateDTO.getFirstName() == null || userUpdateDTO.getLastName() == null
                || userUpdateDTO.getPhoneNumber() == null)
        {
            throw new IllegalArgumentException("All fields must be provided for a full update.");
        }

        String normalizedEmail = normalizeEmail(userUpdateDTO.getEmail());

        if (!normalizedEmail.equals(existingUser.getEmail()))
        {
            userRepository.findByEmail(normalizedEmail)
                    .filter(user -> !user.getPublicId().equals(publicId))
                    .ifPresent(user ->
                    {
                        throw new ResourceConflictException(String.format(EMAIL_ALREADY_TAKEN_MESSAGE, normalizedEmail), ErrorCode.DATA_CONFLICT,
                                Map.of("field", "email", "value", normalizedEmail));
                    });
        }

        userMapper.updateEntity(userUpdateDTO, normalizedEmail, existingUser);

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toResponse(updatedUser);
    }

    public void changePassword(UUID publicId, UserUpdatePasswordDTO passwordChangeDTO)
    {
        User existingUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));

        if (!passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), existingUser.getPassword()))
        {
            throw new IncorrectPasswordException("Current password is not correct.");
        }

        if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmNewPassword()))
        {
            throw new InvalidCredentialsException("New password and confirmation do not match.");
        }

        existingUser.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));

        userRepository.save(existingUser);
    }

    public void deleteUserByPublicId(UUID publicId)
    {
        User userToDelete = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));

        // Applications, interviews and slots keep the recruitment history; deleting a
        // user that owns any of them would orphan those records (FK violation).
        if (applicationRepository.existsByCandidate(userToDelete)
                || interviewRepository.existsByCandidate(userToDelete)
                || interviewRepository.existsByInterviewer(userToDelete)
                || slotRepository.existsByInterviewer(userToDelete))
        {
            throw new ResourceConflictException(
                    "User has applications, interviews or slots and cannot be deleted.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "User"));
        }

        refreshTokenRepository.deleteAllByUserId(userToDelete.getId());
        userRepository.delete(userToDelete);
    }

    public void approveUser(UUID publicId)
    {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));

        user.setApproved(true);
    }

    public PageResponse<UserResponseDTO> getNonApprovedUsers(Pageable pageable)
    {
        return PageResponse.from(userRepository.findByIsApprovedFalse(pageable),
                userMapper::toResponse);
    }

    public CustomUserDetails loadUserById(Long userId)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));
        return new CustomUserDetails(user);
    }

    private static String normalizeEmail(String email)
    {
        return email.trim().toLowerCase();
    }
}
