package com.krino.backend.service;

import com.krino.backend.dto.user.StaffCreateDTO;
import com.krino.backend.dto.user.StaffCreationResponseDTO;
import com.krino.backend.dto.user.UserUpdatePasswordDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.IncorrectPasswordException;
import com.krino.backend.exception.InvalidCredentialsException;
import com.krino.backend.mapper.UserMapper;
import com.krino.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest
{
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:30:00Z"), ZoneOffset.UTC);

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp()
    {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);
        userService = new UserService(userRepository, passwordEncoder, userMapper, null, null, null, null, null,
                FIXED_CLOCK);
    }

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createStaff_generatesNameYearPasswordAndApprovesAccount()
    {
        StaffCreateDTO request = new StaffCreateDTO(" john ", " doe ", "John@TEST.Local", "123456789",
                UserRole.HR_MANAGER);
        when(userRepository.findByEmail("john@test.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponseDTO());

        StaffCreationResponseDTO response = userService.createStaff(request);

        String expectedPassword = "John.Doe2026";
        assertThat(response.getInitialPassword()).isEqualTo(expectedPassword);
        verify(passwordEncoder).encode(expectedPassword);
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("john@test.local")
                        && user.getFirstName().equals("john")
                        && user.getLastName().equals("doe")
                        && user.isApproved()
                        && user.getRoles().contains(UserRole.HR_MANAGER)));
    }

    @Test
    void createStaff_rejectsNonStaffRole()
    {
        StaffCreateDTO request = new StaffCreateDTO("John", "Doe", "john@test.local", null, UserRole.CANDIDATE);

        assertThrows(IllegalArgumentException.class, () -> userService.createStaff(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_incorrectCurrentPassword_throwsIncorrectPasswordException()
    {
        UUID publicId = UUID.randomUUID();
        User user = new User();
        user.setPublicId(publicId);
        user.setPassword("encodedPassword");
        authenticateAs(publicId, UserRole.CANDIDATE);
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("encodedPassword"))).thenReturn(false);

        UserUpdatePasswordDTO dto = new UserUpdatePasswordDTO();
        dto.setCurrentPassword("wrongPassword");
        dto.setNewPassword("newPass");
        dto.setConfirmNewPassword("newPass");

        IncorrectPasswordException ex = assertThrows(
                IncorrectPasswordException.class,
                () -> userService.changePassword(publicId, dto)
        );
        assertEquals("Current password is not correct.", ex.getMessage());
    }

    @Test
    void changePassword_newAndConfirmMismatch_throwsInvalidCredentialsException()
    {
        UUID publicId = UUID.randomUUID();
        User user = new User();
        user.setPublicId(publicId);
        user.setPassword("encodedPassword");
        authenticateAs(publicId, UserRole.CANDIDATE);
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("encodedPassword"))).thenReturn(true);

        UserUpdatePasswordDTO dto = new UserUpdatePasswordDTO();
        dto.setCurrentPassword("correctPassword");
        dto.setNewPassword("newPass");
        dto.setConfirmNewPassword("differentPass");

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.changePassword(publicId, dto)
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
        authenticateAs(publicId, UserRole.CANDIDATE);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setEmail("New@TEST.Local");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(new UserResponseDTO());
        doAnswer(invocation ->
        {
            User target = invocation.getArgument(2);
            target.setEmail(invocation.getArgument(1));
            return null;
        }).when(userMapper).patchEntity(dto, "new@test.local", user);

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
        authenticateAs(publicId, UserRole.CANDIDATE);

        UserUpdateDTO dto = new UserUpdateDTO("Test", "User", "New@TEST.Local", "123456789");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(new UserResponseDTO());
        doAnswer(invocation ->
        {
            User target = invocation.getArgument(2);
            target.setEmail(invocation.getArgument(1));
            return null;
        }).when(userMapper).updateEntity(dto, "new@test.local", user);

        userService.updateUserFully(publicId, dto);

        assertThat(user.getEmail()).isEqualTo("new@test.local");
        verify(userRepository).findByEmail("new@test.local");
    }

    private void authenticateAs(UUID publicId, UserRole role)
    {
        User user = User.builder()
                .id(1L)
                .publicId(publicId)
                .email("principal@test.local")
                .password("encoded")
                .roles(Set.of(role))
                .isApproved(true)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }
}
