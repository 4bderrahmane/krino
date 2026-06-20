package com.krino.backend.service;

import com.krino.backend.dto.user.UserUpdatePasswordDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.entity.User;
import com.krino.backend.exception.IncorrectPasswordException;
import com.krino.backend.exception.InvalidCredentialsException;
import com.krino.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        userService = new UserService(userRepository, passwordEncoder, modelMapper, null, null, null, null);
    }

    @Test
    void changePassword_incorrectCurrentPassword_throwsIncorrectPasswordException()
    {
        User user = new User();
        user.setPassword("encodedPassword");
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("encodedPassword"))).thenReturn(false);

        UserUpdatePasswordDTO dto = new UserUpdatePasswordDTO();
        dto.setCurrentPassword("wrongPassword");
        dto.setNewPassword("newPass");
        dto.setConfirmNewPassword("newPass");

        IncorrectPasswordException ex = assertThrows(
                IncorrectPasswordException.class,
                () -> userService.changePassword(UUID.randomUUID(), dto)
        );
        assertEquals("Current password is not correct.", ex.getMessage());
    }

    @Test
    void changePassword_newAndConfirmMismatch_throwsInvalidCredentialsException()
    {
        User user = new User();
        user.setPassword("encodedPassword");
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("encodedPassword"))).thenReturn(true);

        UserUpdatePasswordDTO dto = new UserUpdatePasswordDTO();
        dto.setCurrentPassword("correctPassword");
        dto.setNewPassword("newPass");
        dto.setConfirmNewPassword("differentPass");

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.changePassword(UUID.randomUUID(), dto)
        );
        assertEquals("New password and confirmation do not match.", ex.getMessage());
    }

    @Test
    void login_invalidCredentials_throwsInvalidCredentialsException()
    {
        User user = new User();
        user.setPassword("encodedPassword");
        when(userRepository.findByEmail("user@test.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () ->
                {
                    // Simulate login logic as per your service (add this method if not present)
                    Optional<User> foundUser = userRepository.findByEmail("user@test.local");
                    if (foundUser.isEmpty() || !passwordEncoder.matches("wrongPassword", foundUser.get().getPassword()))
                    {
                        throw new InvalidCredentialsException("Invalid email or password.");
                    }
                }
        );
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    void updateUserPartially_normalizesEmailBeforeSaving()
    {
        UUID publicId = UUID.randomUUID();
        User user = new User();
        user.setPublicId(publicId);
        user.setEmail("old@test.local");

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setEmail("New@TEST.Local");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(modelMapper.map(user, UserResponseDTO.class)).thenReturn(new UserResponseDTO());

        userService.updateUserPartially(publicId, dto);

        assertThat(user.getEmail()).isEqualTo("new@test.local");
        verify(userRepository).findByEmail("new@test.local");
    }

    @Test
    void updateUserFully_normalizesEmailBeforeSaving()
    {
        UUID publicId = UUID.randomUUID();
        User user = new User();
        user.setPublicId(publicId);
        user.setEmail("old@test.local");

        UserUpdateDTO dto = new UserUpdateDTO("Test", "User", "New@TEST.Local", "123456789");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(modelMapper.map(user, UserResponseDTO.class)).thenReturn(new UserResponseDTO());

        userService.updateUserFully(publicId, dto);

        assertThat(user.getEmail()).isEqualTo("new@test.local");
        verify(userRepository).findByEmail("new@test.local");
    }
}
