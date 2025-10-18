package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.dto.slot.SlotRequestDTO;
import com.jesa.interviewslotmanager.dto.slot.SlotResponseDTO;
import com.jesa.interviewslotmanager.dto.slot.SlotUpdateDTO;
import com.jesa.interviewslotmanager.entity.Slot;
import com.jesa.interviewslotmanager.exception.SlotNotFoundException;
import com.jesa.interviewslotmanager.repository.SlotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SlotService {

    private final SlotRepository slotRepository;
    private final ModelMapper modelMapper;

    public SlotResponseDTO createSlot(SlotRequestDTO slotRequestDTO) {
        Slot slot = modelMapper.map(slotRequestDTO, Slot.class);
        Slot savedSlot = slotRepository.save(slot);
        return modelMapper.map(savedSlot, SlotResponseDTO.class);
    }

    public SlotResponseDTO getSlotById(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException("id", slotId));
        return modelMapper.map(slot, SlotResponseDTO.class);
    }

    public List<SlotResponseDTO> getAllSlots() {
        return slotRepository.findAll().stream()
                .map(slot -> modelMapper.map(slot, SlotResponseDTO.class))
                .collect(Collectors.toList());
    }

    public SlotResponseDTO updateSlot(Long slotId, SlotUpdateDTO slotUpdateDTO) {
        Slot existingSlot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException("id", slotId));

        modelMapper.map(slotUpdateDTO, existingSlot);
        Slot updatedSlot = slotRepository.save(existingSlot);
        return modelMapper.map(updatedSlot, SlotResponseDTO.class);
    }

    public SlotResponseDTO patchSlot(Long slotId, SlotUpdateDTO slotUpdateDTO) {
        Slot existingSlot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException("id", slotId));

        if (slotUpdateDTO.getDurationInMinutes() != null) {
            existingSlot.setDurationInMinutes(slotUpdateDTO.getDurationInMinutes());
        }
        if (slotUpdateDTO.getIsAvailable() != null) {
            existingSlot.setAvailable(slotUpdateDTO.getIsAvailable());
        }
        if (slotUpdateDTO.getInterviewDate() != null) {
            existingSlot.setInterviewDate(java.sql.Date.valueOf(slotUpdateDTO.getInterviewDate()));
        }
        if (slotUpdateDTO.getStartTime() != null) {
            existingSlot.setStartTime(java.sql.Time.valueOf(slotUpdateDTO.getStartTime()));
        }
        if (slotUpdateDTO.getEndTime() != null) {
            existingSlot.setEndTime(java.sql.Time.valueOf(slotUpdateDTO.getEndTime()));
        }

        Slot patchedSlot = slotRepository.save(existingSlot);
        return modelMapper.map(patchedSlot, SlotResponseDTO.class);
    }

    public void deleteSlot(Long slotId) {
        if (!slotRepository.existsById(slotId)) {
            throw new SlotNotFoundException("id", slotId);
        }
        slotRepository.deleteById(slotId);
    }
}
