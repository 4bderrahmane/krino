package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.entity.Slot;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.repository.SlotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SlotService
{

    private final SlotRepository slotRepository;
    private final ModelMapper modelMapper;

    public SlotResponseDTO createSlot(SlotRequestDTO slotRequestDTO)
    {
        Slot slot = modelMapper.map(slotRequestDTO, Slot.class);
        updateDuration(slot);
        Slot savedSlot = slotRepository.save(slot);
        return modelMapper.map(savedSlot, SlotResponseDTO.class);
    }

    public SlotResponseDTO getSlotByPublicId(UUID publicId)
    {
        Slot slot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", publicId));
        return modelMapper.map(slot, SlotResponseDTO.class);
    }

    public PageResponse<SlotResponseDTO> getAllSlots(Pageable pageable)
    {
        return PageResponse.from(slotRepository.findAll(pageable),
                slot -> modelMapper.map(slot, SlotResponseDTO.class));
    }

    public SlotResponseDTO updateSlot(UUID publicId, SlotUpdateDTO slotUpdateDTO)
    {
        Slot existingSlot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", publicId));

        modelMapper.map(slotUpdateDTO, existingSlot);
        updateDuration(existingSlot);
        Slot updatedSlot = slotRepository.save(existingSlot);
        return modelMapper.map(updatedSlot, SlotResponseDTO.class);
    }

    public SlotResponseDTO patchSlot(UUID publicId, SlotUpdateDTO slotUpdateDTO)
    {
        Slot existingSlot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", publicId));

        if (slotUpdateDTO.getAvailable() != null)
        {
            existingSlot.setAvailable(slotUpdateDTO.getAvailable());
        }
        if (slotUpdateDTO.getInterviewDate() != null)
        {
            existingSlot.setInterviewDate(slotUpdateDTO.getInterviewDate());
        }
        if (slotUpdateDTO.getStartTime() != null)
        {
            existingSlot.setStartTime(slotUpdateDTO.getStartTime());
        }
        if (slotUpdateDTO.getEndTime() != null)
        {
            existingSlot.setEndTime(slotUpdateDTO.getEndTime());
        }

        updateDuration(existingSlot);

        Slot patchedSlot = slotRepository.save(existingSlot);
        return modelMapper.map(patchedSlot, SlotResponseDTO.class);
    }

    public void deleteSlot(UUID publicId)
    {
        Slot slot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", publicId));
        slotRepository.delete(slot);
    }

    private void updateDuration(Slot slot)
    {
        if (slot.getStartTime() != null && slot.getEndTime() != null)
        {
            long minutes = Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes();
            slot.setDurationInMinutes((int) minutes);
        }
    }
}
