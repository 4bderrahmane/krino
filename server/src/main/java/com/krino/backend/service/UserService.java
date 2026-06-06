package com.krino.backend.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService
{
    private static final String USER_NOT_FOUND_MESSAGE = "User with ID '%s' not found";
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

    public User getUserById(Long userId)
    {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));
    }

    public List<UserResponseDTO> getAllUsers()
    {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .toList();
    }

    public UserResponseDTO updateUserPartially(Long userId, UserUpdateDTO userUpdateDTO)
    {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));

        if (userUpdateDTO.getUsername() != null && !userUpdateDTO.getUsername().equals(currentUser.getUsername()))
        {
            userRepository.findByUsername(userUpdateDTO.getUsername())
                    .filter(user -> !user.getId().equals(userId))
                    .ifPresent(user ->
                    {
                        throw new ResourceConflictException(String.format(USERNAME_ALREADY_TAKEN_MESSAGE, userUpdateDTO.getUsername()), ErrorCode.USERNAME_ALREADY_EXISTS);
                    });
            currentUser.setUsername(userUpdateDTO.getUsername());
        }

        if (userUpdateDTO.getEmail() != null && !userUpdateDTO.getEmail().equals(currentUser.getEmail()))
        {
            userRepository.findByEmail(userUpdateDTO.getEmail())
                    .filter(user -> !user.getId().equals(userId))
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

    public UserResponseDTO updateUserFully(Long userId, UserUpdateDTO userUpdateDTO)
    {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));

        if (userUpdateDTO.getUsername() == null || userUpdateDTO.getEmail() == null
                || userUpdateDTO.getFirstName() == null || userUpdateDTO.getLastName() == null
                || userUpdateDTO.getPhoneNumber() == null)
        {
            throw new IllegalArgumentException("All fields must be provided for a full update.");
        }

        if (!userUpdateDTO.getUsername().equals(existingUser.getUsername()))
        {
            userRepository.findByUsername(userUpdateDTO.getUsername())
                    .filter(user -> !user.getId().equals(userId))
                    .ifPresent(user ->
                    {
                        throw new ResourceConflictException(String.format(USERNAME_ALREADY_TAKEN_MESSAGE, userUpdateDTO.getUsername()), ErrorCode.USERNAME_ALREADY_EXISTS);
                    });
        }

        if (!userUpdateDTO.getEmail().equals(existingUser.getEmail()))
        {
            userRepository.findByEmail(userUpdateDTO.getEmail())
                    .filter(user -> !user.getId().equals(userId))
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

    public void changePassword(Long userId, UserUpdatePasswordDTO passwordChangeDTO)
    {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));

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

    public void deleteUserById(Long userId)
    {
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));

        refreshTokenRepository.deleteAllByUserId(userId);
        userRepository.delete(userToDelete);
    }

    public void approveUser(Long id)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", id));

        user.setApproved(true);
    }

    public List<UserResponseDTO> getNonApprovedUsers()
    {
        List<User> nonApprovedUsers = userRepository.findByIsApprovedFalse();
        return nonApprovedUsers.stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .toList();
    }

    public CustomUserDetails loadUserById(Long userId)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", userId));
        return new CustomUserDetails(user);
    }
}