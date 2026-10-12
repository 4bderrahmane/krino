package com.krino.backend.service;

import com.krino.backend.dto.user.StaffCreateDTO;
import com.krino.backend.dto.user.UserUpdatePasswordDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.IncorrectPasswordException;
import com.krino.backend.exception.InvalidCredentialsException;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.mapper.UserMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.security.PasswordGenerator;
import java.security.SecureRandom;

import com.krino.backend.service.email.EmailDispatcher;
import com.krino.backend.service.email.EmailVerificationService;
import com.krino.backend.service.resume.ResumeStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest
{
    private EmailDispatcher emailDispatcher;
    private EmailVerificationService emailVerificationService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private RefreshTokenRepository refreshTokenRepository;
    private ApplicationRepository applicationRepository;
    private InterviewRepository interviewRepository;
    private SlotRepository slotRepository;
    private ResumeStorageService resumeStorageService;
    private UserService userService;
    private PasswordGenerator passwordGenerator;

    @BeforeEach
    void setUp()
    {
        emailDispatcher = mock(EmailDispatcher.class);
        emailVerificationService = mock(EmailVerificationService.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        interviewRepository = mock(InterviewRepository.class);
        slotRepository = mock(SlotRepository.class);
        resumeStorageService = mock(ResumeStorageService.class);
        // A plain SecureRandom keeps the unit test off the blocking /dev/random source.
        passwordGenerator = new PasswordGenerator(new SecureRandom());
        userService = new UserService(emailDispatcher, emailVerificationService, userRepository, passwordEncoder,
                userMapper, refreshTokenRepository, applicationRepository, interviewRepository, slotRepository,
                resumeStorageService, passwordGenerator);
    }

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createStaff_generatesRandomPasswordApprovesAccountAndEmailsCredentials()
    {
        StaffCreateDTO request = new StaffCreateDTO(" john ", " doe ", "John@TEST.Local", "123456789",
                UserRole.HR_MANAGER);
        when(userRepository.findByEmail("john@test.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponseDTO());

        UserResponseDTO response = userService.createStaff(request);
        assertThat(response).isNotNull();

        // The password is delivered by email only, never in the API response, so capture the
        // generated value from the email send.
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailDispatcher).sendInitialPassword(eq("john@test.local"), eq("john"), passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        // No longer a predictable name.name+year value: it's a 16-char CSPRNG password with no
        // ambiguous characters or whitespace.
        assertThat(generatedPassword).hasSize(16);
        assertThat(generatedPassword).doesNotContainAnyWhitespaces();
        // PasswordGenerator draws from letters, digits 2-9 and the symbol set !@#$%^&*.
        assertThat(generatedPassword).matches("[A-Za-z2-9!@#$%^&*]+");
        assertThat(generatedPassword).doesNotContain("0", "O", "1", "l", "I");

        // The exact generated password is what gets hashed for the new staff member.
        verify(passwordEncoder).encode(generatedPassword);
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("john@test.local")
                        && user.getFirstName().equals("john")
                        && user.getLastName().equals("doe")
                        && user.isApproved()
                        && user.isMustChangePassword()
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
        dto.setVersion(0L);
        dto.setEmail("New@TEST.Local");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(user)).thenReturn(user);
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

        UserUpdateDTO dto = new UserUpdateDTO(0L, "Test", "User", "New@TEST.Local", "123456789");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(user)).thenReturn(user);
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

    @Test
    void updateUserPartially_emailChange_resetsVerificationAndSendsNewLink()
    {
        UUID publicId = UUID.randomUUID();
        User user = new User();
        user.setPublicId(publicId);
        user.setEmail("old@test.local");
        user.setEmailVerified(true);
        authenticateAs(publicId, UserRole.CANDIDATE);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setVersion(0L);
        dto.setEmail("new@test.local");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(new UserResponseDTO());
        doAnswer(invocation ->
        {
            User target = invocation.getArgument(2);
            target.setEmail(invocation.getArgument(1));
            return null;
        }).when(userMapper).patchEntity(dto, "new@test.local", user);

        userService.updateUserPartially(publicId, dto);

        // The new address is unproven: verified status is dropped and a link goes out to it.
        assertThat(user.isEmailVerified()).isFalse();
        verify(emailVerificationService).sendVerificationEmail(user);
    }

    @Test
    void updateUserPartially_unchangedEmail_keepsVerifiedStatus()
    {
        UUID publicId = UUID.randomUUID();
        User user = new User();
        user.setPublicId(publicId);
        user.setEmail("old@test.local");
        user.setEmailVerified(true);
        authenticateAs(publicId, UserRole.CANDIDATE);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setVersion(0L);
        dto.setEmail("Old@TEST.Local");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(new UserResponseDTO());

        userService.updateUserPartially(publicId, dto);

        assertThat(user.isEmailVerified()).isTrue();
        verify(emailVerificationService, never()).sendVerificationEmail(any(User.class));
    }

    @Test
    void updateUserFully_emailChange_resetsVerificationAndSendsNewLink()
    {
        UUID publicId = UUID.randomUUID();
        User user = new User();
        user.setPublicId(publicId);
        user.setEmail("old@test.local");
        user.setEmailVerified(true);
        authenticateAs(publicId, UserRole.CANDIDATE);

        UserUpdateDTO dto = new UserUpdateDTO(0L, "Test", "User", "New@TEST.Local", "123456789");

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(new UserResponseDTO());
        doAnswer(invocation ->
        {
            User target = invocation.getArgument(2);
            target.setEmail(invocation.getArgument(1));
            return null;
        }).when(userMapper).updateEntity(dto, "new@test.local", user);

        userService.updateUserFully(publicId, dto);

        assertThat(user.isEmailVerified()).isFalse();
        verify(emailVerificationService).sendVerificationEmail(user);
    }

    @Test
    void deleteUser_schedulesBaseResumeDeletionAfterCommit()
    {
        UUID publicId = UUID.randomUUID();
        String objectKey = "users/%s/resume/cv.pdf".formatted(publicId);
        User user = User.builder()
                .id(42L)
                .publicId(publicId)
                .password("encoded")
                .resumeObjectKey(objectKey)
                .build();
        authenticateAs(publicId, UserRole.CANDIDATE);
        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", user.getPassword())).thenReturn(true);

        userService.deleteUserByPublicId(publicId, "correct-password");

        verify(passwordEncoder).matches("correct-password", user.getPassword());
        verify(refreshTokenRepository).deleteAllByUserId(42L);
        verify(userRepository).delete(user);
        verify(resumeStorageService).deleteResumeAfterCommit(objectKey);
    }

    // --- Administrator hierarchy invariants -------------------------------------------------

    @Test
    void setApproval_hrManagerCannotDeactivateAnAdministrator()
    {
        UUID adminPublicId = UUID.randomUUID();
        User administrator = administrator(42L, adminPublicId);
        authenticateAs(UUID.randomUUID(), UserRole.HR_MANAGER);
        when(userRepository.findByPublicId(adminPublicId)).thenReturn(Optional.of(administrator));

        assertThatThrownBy(() -> userService.setApproval(adminPublicId, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only an administrator can manage an administrator account");

        assertThat(administrator.isApproved()).isTrue();
        verify(refreshTokenRepository, never()).deleteAllByUserId(anyLong());
    }

    @Test
    void updateUserPartially_hrManagerCannotEditAnAdministrator()
    {
        UUID adminPublicId = UUID.randomUUID();
        User administrator = administrator(42L, adminPublicId);
        authenticateAs(UUID.randomUUID(), UserRole.HR_MANAGER);
        when(userRepository.findByPublicId(adminPublicId)).thenReturn(Optional.of(administrator));

        UserUpdateDTO update = new UserUpdateDTO();
        update.setFirstName("Renamed");

        assertThatThrownBy(() -> userService.updateUserPartially(adminPublicId, update))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void setApproval_lastActiveAdministratorCannotBeDeactivated()
    {
        UUID targetPublicId = UUID.randomUUID();
        User administrator = administrator(42L, targetPublicId);
        authenticateAs(UUID.randomUUID(), UserRole.ADMIN);
        when(userRepository.findByPublicId(targetPublicId)).thenReturn(Optional.of(administrator));
        when(userRepository.countByRolesContainingAndIsApprovedTrueAndIdNot(UserRole.ADMIN, 42L)).thenReturn(0L);

        assertThatThrownBy(() -> userService.setApproval(targetPublicId, false))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("The last active administrator cannot be deactivated");

        assertThat(administrator.isApproved()).isTrue();
    }

    @Test
    void setApproval_administratorCanBeDeactivatedWhileAnotherRemains()
    {
        UUID targetPublicId = UUID.randomUUID();
        User administrator = administrator(42L, targetPublicId);
        authenticateAs(UUID.randomUUID(), UserRole.ADMIN);
        when(userRepository.findByPublicId(targetPublicId)).thenReturn(Optional.of(administrator));
        when(userRepository.countByRolesContainingAndIsApprovedTrueAndIdNot(UserRole.ADMIN, 42L)).thenReturn(1L);

        userService.setApproval(targetPublicId, false);

        assertThat(administrator.isApproved()).isFalse();
        verify(refreshTokenRepository).deleteAllByUserId(42L);
    }

    @Test
    void setApproval_administratorCannotDeactivateThemselves()
    {
        UUID selfPublicId = UUID.randomUUID();
        User administrator = administrator(7L, selfPublicId);
        authenticateAs(7L, selfPublicId, UserRole.ADMIN);
        when(userRepository.findByPublicId(selfPublicId)).thenReturn(Optional.of(administrator));

        assertThatThrownBy(() -> userService.setApproval(selfPublicId, false))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("You cannot deactivate your own account");

        assertThat(administrator.isApproved()).isTrue();
    }

    /** Reactivating can never lock anyone out, so none of the guards apply to it. */
    @Test
    void setApproval_reactivatingTheOnlyAdministratorIsAllowed()
    {
        UUID targetPublicId = UUID.randomUUID();
        User administrator = administrator(42L, targetPublicId);
        administrator.setApproved(false);
        authenticateAs(UUID.randomUUID(), UserRole.ADMIN);
        when(userRepository.findByPublicId(targetPublicId)).thenReturn(Optional.of(administrator));

        userService.setApproval(targetPublicId, true);

        assertThat(administrator.isApproved()).isTrue();
        verify(userRepository, never()).countByRolesContainingAndIsApprovedTrueAndIdNot(any(), anyLong());
    }

    @Test
    void deleteUserByPublicId_lastActiveAdministratorCannotBeDeleted()
    {
        UUID selfPublicId = UUID.randomUUID();
        User administrator = administrator(7L, selfPublicId);
        authenticateAs(7L, selfPublicId, UserRole.ADMIN);
        when(userRepository.findByPublicId(selfPublicId)).thenReturn(Optional.of(administrator));
        when(userRepository.countByRolesContainingAndIsApprovedTrueAndIdNot(UserRole.ADMIN, 7L)).thenReturn(0L);

        when(passwordEncoder.matches("correct-password", administrator.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUserByPublicId(selfPublicId, "correct-password"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("The last active administrator cannot be deleted");

        verify(userRepository, never()).delete(any(User.class));
    }

    private User administrator(Long id, UUID publicId)
    {
        return User.builder()
                .id(id)
                .publicId(publicId)
                .email("admin-" + id + "@test.local")
                .password("encoded")
                .roles(Set.of(UserRole.ADMIN))
                .isApproved(true)
                .build();
    }

    private void authenticateAs(UUID publicId, UserRole role)
    {
        authenticateAs(1L, publicId, role);
    }

    private void authenticateAs(Long id, UUID publicId, UserRole role)
    {
        User user = User.builder()
                .id(id)
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
