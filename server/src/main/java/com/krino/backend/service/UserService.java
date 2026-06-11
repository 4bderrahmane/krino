package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.dto.user.UserUpdatePasswordDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.User;
import com.krino.backend.entity.UserRole;
import com.krino.backend.exception.InvalidCredentialsException;
import com.krino.backend.exception.IncorrectPasswordException;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService
{
    private static final String USER_NOT_FOUND_MESSAGE = "User with public ID '%s' not found";
    private static final String USERNAME_ALREADY_TAKEN_MESSAGE = "Username '%s' is already taken.";
    private static final String EMAIL_ALREADY_TAKEN_MESSAGE = "Email '%s' is already taken.";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    public List<User> getAllInterviewers()
    {
        return userRepository.findByRolesContaining(UserRole.INTERVIEWER);
    }

    public Optional<User> findByEmail(String email)
    {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username)
    {
        return userRepository.findByUsername(username);
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

    public PageResponse<UserResponseDTO> getAllUsers(Pageable pageable)
    {
        return PageResponse.from(userRepository.findAll(pageable),
                user -> modelMapper.map(user, UserResponseDTO.class));
    }

    public UserResponseDTO updateUserPartially(UUID publicId, UserUpdateDTO userUpdateDTO)
    {
        User currentUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));

        if (userUpdateDTO.getUsername() != null && !userUpdateDTO.getUsername().equals(currentUser.getUsername()))
        {
            userRepository.findByUsername(userUpdateDTO.getUsername())
                    .filter(user -> !user.getPublicId().equals(publicId))
                    .ifPresent(user ->
                    {
                        throw new ResourceConflictException(String.format(USERNAME_ALREADY_TAKEN_MESSAGE, userUpdateDTO.getUsername()), ErrorCode.USERNAME_ALREADY_EXISTS);
                    });
            currentUser.setUsername(userUpdateDTO.getUsername());
        }

        if (userUpdateDTO.getEmail() != null && !userUpdateDTO.getEmail().equals(currentUser.getEmail()))
        {
            userRepository.findByEmail(userUpdateDTO.getEmail())
                    .filter(user -> !user.getPublicId().equals(publicId))
                    .ifPresent(user ->
                    {
                        throw new ResourceConflictException(String.format(EMAIL_ALREADY_TAKEN_MESSAGE, userUpdateDTO.getEmail()), ErrorCode.EMAIL_ALREADY_EXISTS);
                    });
            currentUser.setEmail(userUpdateDTO.getEmail());
        }

        if (userUpdateDTO.getFirstName() != null)
        {
            currentUser.setFirstName(userUpdateDTO.getFirstName());
        }
        if (userUpdateDTO.getLastName() != null)
        {
            currentUser.setLastName(userUpdateDTO.getLastName());
        }
        if (userUpdateDTO.getPhoneNumber() != null)
        {
            currentUser.setPhoneNumber(userUpdateDTO.getPhoneNumber());
        }

        User updatedPartially = userRepository.save(currentUser);
        return modelMapper.map(updatedPartially, UserResponseDTO.class);
    }

    public UserResponseDTO updateUserFully(UUID publicId, UserUpdateDTO userUpdateDTO)
    {
        User existingUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));

        if (userUpdateDTO.getUsername() == null || userUpdateDTO.getEmail() == null
                || userUpdateDTO.getFirstName() == null || userUpdateDTO.getLastName() == null
                || userUpdateDTO.getPhoneNumber() == null)
        {
            throw new IllegalArgumentException("All fields must be provided for a full update.");
        }

        if (!userUpdateDTO.getUsername().equals(existingUser.getUsername()))
        {
            userRepository.findByUsername(userUpdateDTO.getUsername())
                    .filter(user -> !user.getPublicId().equals(publicId))
                    .ifPresent(user ->
                    {
                        throw new ResourceConflictException(String.format(USERNAME_ALREADY_TAKEN_MESSAGE, userUpdateDTO.getUsername()), ErrorCode.USERNAME_ALREADY_EXISTS);
                    });
        }

        if (!userUpdateDTO.getEmail().equals(existingUser.getEmail()))
        {
            userRepository.findByEmail(userUpdateDTO.getEmail())
                    .filter(user -> !user.getPublicId().equals(publicId))
                    .ifPresent(user ->
                    {
                        throw new ResourceConflictException(String.format(EMAIL_ALREADY_TAKEN_MESSAGE, userUpdateDTO.getEmail()), ErrorCode.EMAIL_ALREADY_EXISTS);
                    });
        }

        existingUser.setUsername(userUpdateDTO.getUsername());
        existingUser.setEmail(userUpdateDTO.getEmail());
        existingUser.setFirstName(userUpdateDTO.getFirstName());
        existingUser.setLastName(userUpdateDTO.getLastName());
        existingUser.setPhoneNumber(userUpdateDTO.getPhoneNumber());

        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserResponseDTO.class);
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
                user -> modelMapper.map(user, UserResponseDTO.class));
    }

    public CustomUserDetails loadUserById(Long userId)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));
        return new CustomUserDetails(user);
    }
}
