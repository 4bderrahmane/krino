package com.krino.backend.service;

import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.SlotMapper;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlotServiceTest
{
    private static final Instant NOW = Instant.parse("2099-01-01T08:00:00Z");
    private static final Instant START = Instant.parse("2099-01-01T09:00:00Z");
    private static final Instant END = Instant.parse("2099-01-01T09:45:00Z");

    private SlotRepository slotRepository;
    private UserRepository userRepository;
    private SlotMapper slotMapper;
    private SlotService slotService;

    @BeforeEach
    void setUp()
    {
        slotRepository = mock(SlotRepository.class);
        userRepository = mock(UserRepository.class);
        slotMapper = mock(SlotMapper.class);
        slotService = new SlotService(slotRepository, userRepository, slotMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSlot_withInterviewerId_resolvesInterviewerMapsAndSaves()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID interviewerId = UUID.randomUUID();
        SlotRequestDTO dto = requestFor(interviewerId);

        User interviewer = interviewer(true);
        Slot slot = new Slot();
        Slot saved = new Slot();
        SlotResponseDTO response = new SlotResponseDTO();

        when(userRepository.findByPublicId(interviewerId)).thenReturn(Optional.of(interviewer));
        when(slotMapper.toEntity(dto, interviewer)).thenReturn(slot);
        when(slotRepository.saveAndFlush(slot)).thenReturn(saved);
        when(slotMapper.toResponse(saved)).thenReturn(response);

        SlotResponseDTO result = slotService.createSlot(dto);

        assertThat(result).isSameAs(response);
        verify(slotRepository).saveAndFlush(slot);
    }

    @Test
    void createSlot_unknownInterviewerId_throwsResourceNotFound()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID interviewerId = UUID.randomUUID();
        SlotRequestDTO dto = requestFor(interviewerId);

        when(userRepository.findByPublicId(interviewerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slotService.createSlot(dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void createSlot_ownerWithoutInterviewerRole_throwsConflict()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID interviewerId = UUID.randomUUID();
        SlotRequestDTO dto = requestFor(interviewerId);

        User notAnInterviewer = User.builder()
                .id(2L)
                .publicId(interviewerId)
                .roles(Set.of(UserRole.HR_MANAGER))
                .isApproved(true)
                .build();
        when(userRepository.findByPublicId(interviewerId)).thenReturn(Optional.of(notAnInterviewer));

        assertThatThrownBy(() -> slotService.createSlot(dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("INTERVIEWER role");

        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void createSlot_deactivatedInterviewer_throwsConflict()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID interviewerId = UUID.randomUUID();
        SlotRequestDTO dto = requestFor(interviewerId);

        when(userRepository.findByPublicId(interviewerId)).thenReturn(Optional.of(interviewer(false)));

        assertThatThrownBy(() -> slotService.createSlot(dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("deactivated");

        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void createSlot_exclusionConstraintViolation_isReportedAsConflict()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID interviewerId = UUID.randomUUID();
        SlotRequestDTO dto = requestFor(interviewerId);

        User interviewer = interviewer(true);
        Slot slot = new Slot();
        when(userRepository.findByPublicId(interviewerId)).thenReturn(Optional.of(interviewer));
        when(slotMapper.toEntity(dto, interviewer)).thenReturn(slot);
        when(slotRepository.saveAndFlush(slot)).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("ERROR: conflicting key value violates exclusion constraint "
                        + "\"ex_slots_interviewer_no_overlap\"")));

        assertThatThrownBy(() -> slotService.createSlot(dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("overlapping");
    }

    @Test
    void createSlot_unrelatedIntegrityViolation_isNotMasked()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID interviewerId = UUID.randomUUID();
        SlotRequestDTO dto = requestFor(interviewerId);

        User interviewer = interviewer(true);
        Slot slot = new Slot();
        when(userRepository.findByPublicId(interviewerId)).thenReturn(Optional.of(interviewer));
        when(slotMapper.toEntity(dto, interviewer)).thenReturn(slot);
        when(slotRepository.saveAndFlush(slot))
                .thenThrow(new DataIntegrityViolationException("uk_slots_public_id violated"));

        assertThatThrownBy(() -> slotService.createSlot(dto))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void getSlotByPublicId_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slotService.getSlotByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSlot_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        SlotUpdateDTO update = new SlotUpdateDTO();
        assertThatThrownBy(() -> slotService.updateSlot(publicId, update))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateSlot_withBookedInterview_throwsConflictAndDoesNotSave()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID publicId = UUID.randomUUID();
        Slot slot = slotWithWindow();
        slot.setInterview(new Interview());

        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.of(slot));

        SlotUpdateDTO update = new SlotUpdateDTO();
        update.setStartAt(START.plusSeconds(3600));
        update.setEndAt(END.plusSeconds(3600));

        assertThatThrownBy(() -> slotService.updateSlot(publicId, update))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("cancel the interview first");

        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void patchSlot_exclusionConstraintViolation_isReportedAsConflict()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID publicId = UUID.randomUUID();
        Slot slot = slotWithWindow();
        slot.setId(7L);
        Instant newEnd = END.plusSeconds(1800);

        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.of(slot));
        when(slotRepository.saveAndFlush(slot)).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("ERROR: conflicting key value violates exclusion constraint "
                        + "\"ex_slots_interviewer_no_overlap\"")));

        SlotUpdateDTO update = new SlotUpdateDTO();
        update.setEndAt(newEnd);

        assertThatThrownBy(() -> slotService.patchSlot(publicId, update))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("overlapping");

        verify(slotMapper).patchEntity(update, slot);
        verify(slotRepository).saveAndFlush(slot);
    }

    // Moving only the start past the stored end would leave an inverted window, which the
    // per-field bean validation cannot see.
    @Test
    void patchSlot_startMovedPastStoredEnd_throwsValidationError()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID publicId = UUID.randomUUID();
        Slot slot = slotWithWindow();
        slot.setId(7L);

        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.of(slot));

        SlotUpdateDTO update = new SlotUpdateDTO();
        update.setStartAt(END.plusSeconds(600));

        assertThatThrownBy(() -> slotService.patchSlot(publicId, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after its start time");

        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void patchSlot_omittedStartAlreadyPassed_throwsValidationError()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID publicId = UUID.randomUUID();
        Slot slot = slotWithWindow();
        slot.setStartAt(NOW.minusSeconds(60));

        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.of(slot));

        SlotUpdateDTO update = new SlotUpdateDTO();
        update.setEndAt(END.plusSeconds(600));

        assertThatThrownBy(() -> slotService.patchSlot(publicId, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start in the future");

        verify(slotMapper, never()).patchEntity(any(), any());
        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteSlot_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slotService.deleteSlot(publicId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(slotRepository, never()).delete(any());
    }

    @Test
    void deleteSlot_withBookedInterview_throwsConflictAndDoesNotDelete()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID publicId = UUID.randomUUID();
        Slot slot = slotWithWindow();
        slot.setInterview(new Interview());

        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> slotService.deleteSlot(publicId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("cancel the interview first");

        verify(slotRepository, never()).delete(any());
    }

    @Test
    void deleteSlot_withoutInterview_deletesSlot()
    {
        authenticateAs(UserRole.ADMIN, UUID.randomUUID());
        UUID publicId = UUID.randomUUID();
        Slot slot = slotWithWindow();

        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.of(slot));

        slotService.deleteSlot(publicId);

        verify(slotRepository).delete(slot);
    }

    private SlotRequestDTO requestFor(UUID interviewerId)
    {
        SlotRequestDTO dto = new SlotRequestDTO();
        dto.setInterviewerId(interviewerId);
        dto.setStartAt(START);
        dto.setEndAt(END);
        return dto;
    }

    private Slot slotWithWindow()
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer(true));
        slot.setStartAt(START);
        slot.setEndAt(END);
        return slot;
    }

    private User interviewer(boolean approved)
    {
        return User.builder()
                .id(2L)
                .publicId(UUID.randomUUID())
                .email("interviewer@test.local")
                .roles(Set.of(UserRole.INTERVIEWER))
                .isApproved(approved)
                .build();
    }

    private void authenticateAs(UserRole role, UUID publicId)
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
