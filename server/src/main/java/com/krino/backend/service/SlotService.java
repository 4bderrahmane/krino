package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.SlotMapper;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SecurityUtilities;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SlotService {

    private static final String PUBLIC_ID = "publicId";
    private static final String RESOURCE = "resource";
    private static final String FIELD = "field";
    private static final String SLOT = "Slot";
    private static final String INTERVIEWER_ID = "interviewerId";
    private static final String BOOKED_SLOT_MESSAGE = "Slot has an interview booked into it; cancel the interview first.";
    private static final String OVERLAP_MESSAGE = "The interviewer already has a slot overlapping that window.";
    private static final String OVERLAP_CONSTRAINT = "ex_slots_interviewer_no_overlap";

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final SlotMapper slotMapper;
    private final Clock clock;

    public SlotResponseDTO createSlot(SlotRequestDTO slotRequestDTO) {
        User interviewer = resolveInterviewer(slotRequestDTO.getInterviewerId());

        Slot slot = slotMapper.toEntity(slotRequestDTO, interviewer);
        return slotMapper.toResponse(persist(slot));
    }

    private User resolveInterviewer(UUID interviewerPublicId) {
        CustomUserDetails currentUser = SecurityUtilities.requireCurrentCustomUser();
        User interviewer;

        if (interviewerPublicId != null
                && !currentUser.getPublicId().equals(interviewerPublicId)) {
            SecurityUtilities.requireAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER);

            interviewer = userRepository.findByPublicId(interviewerPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), PUBLIC_ID, interviewerPublicId));
        } else {
            interviewer = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("The authenticated user's account no longer exists."));
        }

        return requireEligibleInterviewer(interviewer);
    }

    private User requireEligibleInterviewer(User interviewer) {
        if (!interviewer.hasRole(UserRole.INTERVIEWER)) {
            throw new ResourceConflictException(
                    "Slots can only be owned by a user with the INTERVIEWER role.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(FIELD, INTERVIEWER_ID));
        }
        if (!interviewer.isApproved()) {
            throw new ResourceConflictException(
                    "The interviewer's account is deactivated and cannot take new slots.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(FIELD, INTERVIEWER_ID));
        }
        return interviewer;
    }

    public SlotResponseDTO getSlotByPublicId(UUID publicId) {
        Slot slot = findSlot(publicId);
        requireSlotOwnerOrStaff(slot);
        return slotMapper.toResponse(slot);
    }

    public PageResponse<SlotResponseDTO> getAllSlots(Pageable pageable) {
        SecurityUtilities.requireAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER);
        return PageResponse.from(slotRepository.findAll(pageable), slotMapper::toResponse);
    }

    public SlotResponseDTO updateSlot(UUID publicId, SlotUpdateDTO slotUpdateDTO) {
        Slot existingSlot = findSlot(publicId);
        requireSlotOwnerOrStaff(existingSlot);
        requireEditableWindow(existingSlot, slotUpdateDTO);

        slotMapper.updateEntity(slotUpdateDTO, existingSlot);
        return slotMapper.toResponse(persist(existingSlot));
    }

    public SlotResponseDTO patchSlot(UUID publicId, SlotUpdateDTO slotUpdateDTO) {
        Slot existingSlot = findSlot(publicId);
        requireSlotOwnerOrStaff(existingSlot);
        requireEditableWindow(existingSlot, slotUpdateDTO);

        slotMapper.patchEntity(slotUpdateDTO, existingSlot);
        return slotMapper.toResponse(persist(existingSlot));
    }

    public void deleteSlot(UUID publicId) {
        Slot slot = findSlot(publicId);
        requireSlotOwnerOrStaff(slot);
        requireUnbooked(slot);

        slotRepository.delete(slot);
    }

    private Slot findSlot(UUID publicId) {
        return slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(SLOT, PUBLIC_ID, publicId));
    }

    private void requireEditableWindow(Slot slot, SlotUpdateDTO update) {
        requireUnbooked(slot);

        Instant startAt = update.getStartAt() != null ? update.getStartAt() : slot.getStartAt();
        Instant endAt = update.getEndAt() != null ? update.getEndAt() : slot.getEndAt();

        requireForwardWindow(startAt, endAt);
        if (update.getStartAt() == null) {
            requireFutureWindow(startAt);
        }
    }

    private void requireUnbooked(Slot slot) {
        if (slot.getInterview() != null) {
            throw new ResourceConflictException(
                    BOOKED_SLOT_MESSAGE,
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(RESOURCE, SLOT));
        }
    }

    private void requireForwardWindow(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("A slot must have both a start and an end time.");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("A slot's end time must be after its start time.");
        }
    }

    private void requireFutureWindow(Instant startAt) {
        if (!startAt.isAfter(clock.instant())) {
            throw new IllegalArgumentException("A slot must start in the future.");
        }
    }

    /**
     * Flushes inside the service so the database's overlap rejection is translated into
     * an application conflict instead of surfacing as a raw integrity error at commit time.
     */
    private Slot persist(Slot slot) {
        try {
            return slotRepository.saveAndFlush(slot);
        } catch (DataIntegrityViolationException ex) {
            if (violatesOverlapConstraint(ex)) {
                throw new ResourceConflictException(OVERLAP_MESSAGE, ErrorCode.DATA_CONFLICT, Map.of(RESOURCE, SLOT));
            }
            throw ex;
        }
    }

    private static boolean violatesOverlapConstraint(Throwable failure) {
        for (Throwable cause = failure; cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains(OVERLAP_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }

    private void requireSlotOwnerOrStaff(Slot slot) {
        if (SecurityUtilities.hasAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER)) {
            return;
        }
        User interviewer = slot.getInterviewer();
        if (interviewer == null || interviewer.getPublicId() == null) {
            throw new AccessDeniedException("You do not have permission to access this slot.");
        }
        SecurityUtilities.requireCurrentUser(interviewer.getPublicId());
    }
}
