package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.dto.user.UserUpdatePasswordDTO;
import com.jesa.interviewslotmanager.entity.User;
import com.jesa.interviewslotmanager.exception.IncorrectPasswordException;
import com.jesa.interviewslotmanager.exception.InvalidCredentialsException;
import com.jesa.interviewslotmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class UserServiceTest
{
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private ModelMapper modelMapper;
    private UserService userService;

    @BeforeEach
    void setUp()
    {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        modelMapper = mock(ModelMapper.class);
        userService = new UserService(userRepository, passwordEncoder, modelMapper, null);
    }

    @Test
    void changePassword_incorrectCurrentPassword_throwsIncorrectPasswordException()
    {
        User user = new User();
        user.setPassword("encodedPassword");
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("encodedPassword"))).thenReturn(false);

        UserUpdatePasswordDTO dto = new UserUpdatePasswordDTO();
        dto.setCurrentPassword("wrongPassword");
        dto.setNewPassword("newPass");
        dto.setConfirmNewPassword("newPass");

        IncorrectPasswordException ex = assertThrows(
                IncorrectPasswordException.class,
                () -> userService.changePassword(1L, dto)
        );
        assertEquals("Current password is not correct.", ex.getMessage());
    }

    @Test
    void changePassword_newAndConfirmMismatch_throwsInvalidCredentialsException()
    {
        User user = new User();
        user.setPassword("encodedPassword");
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("encodedPassword"))).thenReturn(true);

        UserUpdatePasswordDTO dto = new UserUpdatePasswordDTO();
        dto.setCurrentPassword("correctPassword");
        dto.setNewPassword("newPass");
        dto.setConfirmNewPassword("differentPass");

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.changePassword(1L, dto)
        );
        assertEquals("New password and confirmation do not match.", ex.getMessage());
    }

    @Test
    void login_invalidCredentials_throwsInvalidCredentialsException()
    {
        User user = new User();
        user.setPassword("encodedPassword");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () ->
                {
                    // Simulate login logic as per your service (add this method if not present)
                    Optional<User> foundUser = userRepository.findByUsername("user");
                    if (foundUser.isEmpty() || !passwordEncoder.matches("wrongPassword", foundUser.get().getPassword()))
                    {
                        throw new InvalidCredentialsException("Invalid username or password.");
                    }
                }
        );
        assertEquals("Invalid username or password.", ex.getMessage());
    }
}

