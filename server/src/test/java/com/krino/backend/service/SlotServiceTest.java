package com.krino.backend.service;

import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.SlotMapper;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
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
        slotService = new SlotService(slotRepository, userRepository, slotMapper);
    }

    @Test
    void createSlot_withInterviewerId_resolvesInterviewerMapsAndSaves()
    {
        UUID interviewerId = UUID.randomUUID();
        SlotRequestDTO dto = new SlotRequestDTO();
        dto.setInterviewerId(interviewerId);

        User interviewer = new User();
        Slot slot = new Slot();
        Slot saved = new Slot();
        SlotResponseDTO response = new SlotResponseDTO();

        when(userRepository.findByPublicId(interviewerId)).thenReturn(Optional.of(interviewer));
        when(slotMapper.toEntity(dto, interviewer)).thenReturn(slot);
        when(slotRepository.save(slot)).thenReturn(saved);
        when(slotMapper.toResponse(saved)).thenReturn(response);

        SlotResponseDTO result = slotService.createSlot(dto);

        assertThat(result).isSameAs(response);
        verify(slotRepository).save(slot);
    }

    @Test
    void createSlot_unknownInterviewerId_throwsResourceNotFound()
    {
        UUID interviewerId = UUID.randomUUID();
        SlotRequestDTO dto = new SlotRequestDTO();
        dto.setInterviewerId(interviewerId);

        when(userRepository.findByPublicId(interviewerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slotService.createSlot(dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(slotRepository, never()).save(any());
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

        assertThatThrownBy(() -> slotService.updateSlot(publicId, new SlotUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(slotRepository, never()).save(any());
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
        UUID publicId = UUID.randomUUID();
        Slot slot = new Slot();
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
        UUID publicId = UUID.randomUUID();
        Slot slot = new Slot();

        when(slotRepository.findByPublicId(publicId)).thenReturn(Optional.of(slot));

        slotService.deleteSlot(publicId);

        verify(slotRepository).delete(slot);
    }
}
