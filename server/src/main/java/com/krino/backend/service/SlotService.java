package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SecurityUtilities;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SlotService
{

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public SlotResponseDTO createSlot(SlotRequestDTO slotRequestDTO)
    {
        Slot slot = new Slot();
        slot.setInterviewer(resolveInterviewer(slotRequestDTO.getInterviewerId()));
        slot.setInterviewDate(slotRequestDTO.getInterviewDate());
        slot.setStartTime(slotRequestDTO.getStartTime());
        slot.setEndTime(slotRequestDTO.getEndTime());

        Slot savedSlot = slotRepository.save(slot);
        return modelMapper.map(savedSlot, SlotResponseDTO.class);
    }

    /**
     * The slot belongs to the user given in the request (HR booking on behalf of an
     * interviewer), or to the authenticated user when no interviewer is specified.
     */
    private User resolveInterviewer(UUID interviewerPublicId)
    {
        if (interviewerPublicId != null)
        {
            return userRepository.findByPublicId(interviewerPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", interviewerPublicId));
        }

        return SecurityUtilities.getCurrentCustomUser()
                .map(CustomUserDetails::getId)
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new AccessDeniedException("No authenticated user to own the slot"));
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
        Slot updatedSlot = slotRepository.save(existingSlot);
        return modelMapper.map(updatedSlot, SlotResponseDTO.class);
    }

    public SlotResponseDTO patchSlot(UUID publicId, SlotUpdateDTO slotUpdateDTO)
    {
        Slot existingSlot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", publicId));

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

        Slot patchedSlot = slotRepository.save(existingSlot);
        return modelMapper.map(patchedSlot, SlotResponseDTO.class);
    }

    public void deleteSlot(UUID publicId)
    {
        Slot slot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", publicId));

        if (slot.getInterview() != null)
        {
            throw new ResourceConflictException(
                    "Slot has an interview booked into it; cancel the interview first.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Slot"));
        }

        slotRepository.delete(slot);
    }
}
