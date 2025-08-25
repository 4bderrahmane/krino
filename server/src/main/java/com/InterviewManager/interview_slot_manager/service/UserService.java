package com.InterviewManager.interview_slot_manager.service;

import com.InterviewManager.interview_slot_manager.DTO.User.*;
import com.InterviewManager.interview_slot_manager.entity.User;
import com.InterviewManager.interview_slot_manager.entity.UserRole;
import com.InterviewManager.interview_slot_manager.exception.EmailAlreadyExistsException;
import com.InterviewManager.interview_slot_manager.exception.InvalidCredentialsException;
import com.InterviewManager.interview_slot_manager.exception.UserAlreadyExistsException;
import com.InterviewManager.interview_slot_manager.exception.UserNotFoundException;
import com.InterviewManager.interview_slot_manager.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

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

    public User updateUser(User user)
    {
        return userRepository.save(user);
    }

    public User addRoleToUser(Long userId, UserRole role)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.getRoles().add(role);

        return userRepository.save(user);
    }

    public User getUserById(Long userId)
    {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found"));
    }

    public List<UserResponseDTO> getAllUsers()
    {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .collect(Collectors.toList());
    }

    public UserResponseDTO updateUser(Long userId, UserUpdateDTO userUpdateDTO)
    {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

        if (userUpdateDTO.getUsername() != null && !userUpdateDTO.getUsername().equals(existingUser.getUsername()))
        {
            userRepository.findByUsername(userUpdateDTO.getUsername())
                    .filter(user -> !user.getId().equals(userId))
                    .ifPresent(user ->
                    {
                        throw new UserAlreadyExistsException("Username '" + userUpdateDTO.getUsername() + "' is already taken.");
                    });
            existingUser.setUsername(userUpdateDTO.getUsername());
        }

        if (userUpdateDTO.getEmail() != null && !userUpdateDTO.getEmail().equals(existingUser.getEmail()))
        {
            userRepository.findByEmail(userUpdateDTO.getEmail())
                    .filter(user -> !user.getId().equals(userId))
                    .ifPresent(user ->
                    {
                        throw new EmailAlreadyExistsException("Email '" + userUpdateDTO.getEmail() + "' is already taken.");
                    });
            existingUser.setEmail(userUpdateDTO.getEmail());
        }
        if (userUpdateDTO.getFirstName() != null)
        {
            existingUser.setFirstName(userUpdateDTO.getFirstName());
        }
        if (userUpdateDTO.getLastName() != null)
        {
            existingUser.setLastName(userUpdateDTO.getLastName());
        }
        if (userUpdateDTO.getPhoneNumber() != null)
        {
            existingUser.setPhoneNumber(userUpdateDTO.getPhoneNumber());
        }

        User updatedUser = userRepository.save(existingUser);

        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }

    public UserResponseDTO changePassword(Long userId, UserUpdatePasswordDTO passwordChangeDTO)
    {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

        if (!passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), existingUser.getPassword()))
        {
            throw new InvalidCredentialsException("Current password is not correct.");
        }

        if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmNewPassword()))
        {
            throw new InvalidCredentialsException("New password and confirmation do not match.");
        }

        existingUser.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));

        User updatedUser = userRepository.save(existingUser);

        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }

    public void deleteUserById(Long userId)
    {
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));
        userRepository.delete(userToDelete);
    }

    public void approveUser(Long id)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + id + " not found."));

        user.setApproved(true);
    }

    public List<UserResponseDTO> getNonApprovedUsers() {
        List<User> nonApprovedUsers = userRepository.findByIsApprovedFalse();
        return nonApprovedUsers.stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .collect(Collectors.toList());
    }
}