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
import com.krino.backend.mapper.SlotMapper;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SecurityUtilities;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SlotService {

    private static final String ADMIN = "ADMIN";
    private static final String HR_MANAGER = "HR_MANAGER";
    private static final String PUBLIC_ID = "publicId";
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final SlotMapper slotMapper;

    public SlotResponseDTO createSlot(SlotRequestDTO slotRequestDTO) {
        Slot slot = slotMapper.toEntity(slotRequestDTO, resolveInterviewer(slotRequestDTO.getInterviewerId()));

        Slot savedSlot = slotRepository.save(slot);
        return slotMapper.toResponse(savedSlot);
    }

    /**
     * The slot belongs to the user given in the request (HR booking on behalf of an
     * interviewer), or to the authenticated user when no interviewer is specified.
     */
    private User resolveInterviewer(UUID interviewerPublicId) {
        CustomUserDetails currentUser = SecurityUtilities.requireCurrentCustomUser();
        if (interviewerPublicId != null) {
            if (!currentUser.getPublicId().equals(interviewerPublicId)) {
                SecurityUtilities.requireAnyRole(ADMIN, HR_MANAGER);
            }
            return userRepository.findByPublicId(interviewerPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID,
                            interviewerPublicId));
        }

        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("No authenticated user to own the slot"));
    }

    public SlotResponseDTO getSlotByPublicId(UUID publicId) {
        Slot slot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), PUBLIC_ID, publicId));
        requireSlotOwnerOrStaff(slot);
        return slotMapper.toResponse(slot);
    }

    public PageResponse<SlotResponseDTO> getAllSlots(Pageable pageable) {
        SecurityUtilities.requireAnyRole(ADMIN, HR_MANAGER);
        return PageResponse.from(slotRepository.findAll(pageable),
                slotMapper::toResponse);
    }

    public SlotResponseDTO updateSlot(UUID publicId, SlotUpdateDTO slotUpdateDTO) {
        Slot existingSlot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), PUBLIC_ID, publicId));
        requireSlotOwnerOrStaff(existingSlot);

        slotMapper.updateEntity(slotUpdateDTO, existingSlot);
        Slot updatedSlot = slotRepository.save(existingSlot);
        return slotMapper.toResponse(updatedSlot);
    }

    public SlotResponseDTO patchSlot(UUID publicId, SlotUpdateDTO slotUpdateDTO) {
        Slot existingSlot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), PUBLIC_ID, publicId));
        requireSlotOwnerOrStaff(existingSlot);

        slotMapper.patchEntity(slotUpdateDTO, existingSlot);

        Slot patchedSlot = slotRepository.save(existingSlot);
        return slotMapper.toResponse(patchedSlot);
    }

    public void deleteSlot(UUID publicId) {
        Slot slot = slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), PUBLIC_ID, publicId));
        requireSlotOwnerOrStaff(slot);

        if (slot.getInterview() != null) {
            throw new ResourceConflictException(
                    "Slot has an interview booked into it; cancel the interview first.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Slot"));
        }

        slotRepository.delete(slot);
    }

    private void requireSlotOwnerOrStaff(Slot slot) {
        if (SecurityUtilities.hasAnyRole(ADMIN, HR_MANAGER)) {
            return;
        }
        User interviewer = slot.getInterviewer();
        if (interviewer == null || interviewer.getPublicId() == null) {
            throw new AccessDeniedException("You do not have permission to access this slot.");
        }
        SecurityUtilities.requireCurrentUser(interviewer.getPublicId());
    }
}
