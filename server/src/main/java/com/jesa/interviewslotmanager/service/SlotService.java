package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.dto.slot.SlotRequestDTO;
import com.jesa.interviewslotmanager.dto.slot.SlotResponseDTO;
import com.jesa.interviewslotmanager.dto.slot.SlotUpdateDTO;
import com.jesa.interviewslotmanager.entity.Slot;
import com.jesa.interviewslotmanager.exception.ResourceNotFoundException;
import com.jesa.interviewslotmanager.repository.SlotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

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

    public SlotResponseDTO getSlotById(Long slotId)
    {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "id", slotId));
        return modelMapper.map(slot, SlotResponseDTO.class);
    }

    public List<SlotResponseDTO> getAllSlots()
    {
        return slotRepository.findAll().stream()
                .map(slot -> modelMapper.map(slot, SlotResponseDTO.class))
                .toList();
    }

    public SlotResponseDTO updateSlot(Long slotId, SlotUpdateDTO slotUpdateDTO)
    {
        Slot existingSlot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "id", slotId));

        modelMapper.map(slotUpdateDTO, existingSlot);
        updateDuration(existingSlot);
        Slot updatedSlot = slotRepository.save(existingSlot);
        return modelMapper.map(updatedSlot, SlotResponseDTO.class);
    }

    public SlotResponseDTO patchSlot(Long slotId, SlotUpdateDTO slotUpdateDTO)
    {
        Slot existingSlot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "id", slotId));

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

    public void deleteSlot(Long slotId)
    {
        if (!slotRepository.existsById(slotId))
        {
            throw new ResourceNotFoundException(Slot.class.getSimpleName(), "id", slotId);

        }
        slotRepository.deleteById(slotId);
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