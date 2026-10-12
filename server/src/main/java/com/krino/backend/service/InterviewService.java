package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.dto.interview.InterviewView;
import com.krino.backend.entity.*;
import com.krino.backend.entity.enums.ApplicationStatus;
import com.krino.backend.entity.enums.InterviewStatus;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.InterviewMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SecurityUtilities;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.time.Clock;

@Service
@Transactional
@RequiredArgsConstructor
public class InterviewService {

    public static final String FIELD = "field";
    private static final String PUBLIC_ID = "publicId";
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final SlotRepository slotRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewMapper interviewMapper;
    private final Clock clock;
    private final EntityManager entityManager;

    public InterviewResponseDTO createInterview(InterviewRequestDTO interviewRequestDTO) {
        SecurityUtilities.requireAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER);
        Application application = resolveApplication(interviewRequestDTO.getApplicationId());
        lockApplication(application);
        requireSchedulableApplication(application);

        Slot slot = resolveSlot(interviewRequestDTO.getSlotId());
        validateBooking(slot, null, interviewRequestDTO.getStatus());

        Interview interview = interviewMapper.toEntity(interviewRequestDTO, slot.getInterviewer(), application, slot);
        requireMeetingConsistency(interview);
        requireRecommendationConsistency(interview);

        Interview savedInterview = interviewRepository.save(interview);
        if (savedInterview.getStatus() == InterviewStatus.CANCELLED) {
            savedInterview.setSlot(null);
        } else if (savedInterview.getStatus() == InterviewStatus.SCHEDULED) {
            markApplicationScheduled(application);
        }
        return interviewMapper.toResponse(savedInterview);
    }

    public InterviewView getInterviewByPublicId(UUID publicId) {
        Interview interview = findInterview(publicId);
        requireInterviewParticipantOrStaff(interview);
        return toPermittedView(interview);
    }

    public PageResponse<InterviewResponseDTO> getAllInterviews(Pageable pageable) {
        SecurityUtilities.requireAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER);
        return PageResponse.from(interviewRepository.findAll(pageable),
                interviewMapper::toResponse);
    }

    /** The query is the gate here: it returns only the interviews the caller takes part in. */
    public PageResponse<InterviewView> getMyInterviews(Pageable pageable) {
        User currentUser = getCurrentUser();
        return PageResponse.from(
                interviewRepository.findByApplication_CandidateOrInterviewer(currentUser, currentUser, pageable),
                this::toPermittedView);
    }

    /**
     * Picks the representation the caller is entitled to, having already been cleared to read the
     * interview at all. Staff and interviewers get the full record; a candidate gets logistics only,
     * since the notes and the recommendation are the interviewer's working record and the other
     * party's contact details are not theirs to read.
     *
     * <p>A role check is enough because the caller has already been narrowed to a participant or to
     * staff: an interviewer with no part in this interview is turned away by
     * {@link #requireInterviewParticipantOrStaff}, and never appears in {@link #getMyInterviews}.
     * It also takes an account to hold a single role, so that nobody is staff on one interview and
     * a candidate on another.
     */
    private InterviewView toPermittedView(Interview interview) {
        if (SecurityUtilities.hasAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER, UserRole.INTERVIEWER)) {
            return interviewMapper.toResponse(interview);
        }

        return interviewMapper.toCandidateResponse(interview);
    }

    private User getCurrentUser() {
        var currentUser = SecurityUtilities.requireCurrentCustomUser();
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("No authenticated user."));
    }

    public InterviewResponseDTO updateInterview(UUID publicId, InterviewRequestDTO interviewRequestDTO) {
        Interview existingInterview = findInterview(publicId);
        requireInterviewInterviewerOrStaff(existingInterview);
        requireApplicationUnchanged(existingInterview, interviewRequestDTO.getApplicationId());
        lockApplication(existingInterview.getApplication());
        InterviewStatus targetStatus = targetStatus(existingInterview, interviewRequestDTO);
        Slot slot = targetStatus == InterviewStatus.CANCELLED
                ? existingInterview.getSlot() : resolveSlot(interviewRequestDTO.getSlotId());
        requireSlotInterviewerOrStaff(slot);

        validateBooking(slot, existingInterview, targetStatus);

        User interviewer = slot != null ? slot.getInterviewer() : existingInterview.getInterviewer();
        interviewMapper.updateEntity(interviewRequestDTO, interviewer, slot, existingInterview);
        requireMeetingConsistency(existingInterview);
        requireRecommendationConsistency(existingInterview);
        reconcileBooking(existingInterview);

        Interview updatedInterview = interviewRepository.save(existingInterview);
        return interviewMapper.toResponse(updatedInterview);
    }

    public InterviewResponseDTO patchInterview(UUID publicId, InterviewRequestDTO interviewRequestDTO) {
        Interview existingInterview = findInterview(publicId);
        requireInterviewInterviewerOrStaff(existingInterview);
        requireApplicationUnchanged(existingInterview, interviewRequestDTO.getApplicationId());
        lockApplication(existingInterview.getApplication());
        InterviewStatus targetStatus = targetStatus(existingInterview, interviewRequestDTO);

        Slot slot = targetStatus != InterviewStatus.CANCELLED && interviewRequestDTO.getSlotId() != null
                ? resolveSlot(interviewRequestDTO.getSlotId())
                : existingInterview.getSlot();
        requireSlotInterviewerOrStaff(slot);

        validateBooking(slot, existingInterview, targetStatus);

        User interviewer = slot != null ? slot.getInterviewer() : existingInterview.getInterviewer();
        interviewMapper.patchEntity(interviewRequestDTO, interviewer, slot, existingInterview);
        requireMeetingConsistency(existingInterview);
        requireRecommendationConsistency(existingInterview);
        reconcileBooking(existingInterview);

        Interview patchedInterview = interviewRepository.save(existingInterview);
        return interviewMapper.toResponse(patchedInterview);
    }

    public void deleteInterview(UUID publicId) {
        SecurityUtilities.requireAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER);
        Interview interview = findInterview(publicId);
        lockApplication(interview.getApplication());
        interview.setSlot(null); // free the slot
        interviewRepository.delete(interview);
        returnApplicationToReviewIfUnscheduled(interview);
    }

    private InterviewStatus targetStatus(Interview interview, InterviewRequestDTO request) {
        return request.getStatus() != null ? request.getStatus() : interview.getStatus();
    }

    private void lockApplication(Application application) {
        // Serialize bookings/cancellations for one application. Two concurrent cancellations
        // must not both see the other's interview and leave the application scheduled forever.
        // Refresh also discards an application snapshot loaded before waiting for the lock.
        entityManager.refresh(application, LockModeType.PESSIMISTIC_WRITE);
    }

    private void reconcileBooking(Interview interview) {
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            interview.setSlot(null);
            returnApplicationToReviewIfUnscheduled(interview);
        } else if (interview.getStatus() == InterviewStatus.SCHEDULED) {
            markApplicationScheduled(interview.getApplication());
        }
    }

    private void returnApplicationToReviewIfUnscheduled(Interview interview) {
        Application application = interview.getApplication();
        if (application.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED
                && !interviewRepository.existsByApplicationAndStatusAndIdNot(
                        application, InterviewStatus.SCHEDULED, interview.getId())) {
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
        }
    }

    private Interview findInterview(UUID publicId) {
        return interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), PUBLIC_ID, publicId));
    }

    /**
     * An interview can only be scheduled for an application that is still in play. Once a
     * decision has been made (accepted or rejected) the candidate is no longer in the
     * pipeline, so booking an interview against it makes no sense and is rejected.
     */
    private void requireSchedulableApplication(Application application) {
        ApplicationStatus status = application.getStatus();
        if (status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED) {
            throw new ResourceConflictException(
                    "Cannot schedule an interview for an application that has been "
                            + status.name().toLowerCase() + ".",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("status", status.name()));
        }
    }

    /**
     * Booking an interview advances the application into the INTERVIEW_SCHEDULED stage.
     * Applications that are already there (e.g. a second interview round) keep that status.
     */
    private void markApplicationScheduled(Application application) {
        ApplicationStatus status = application.getStatus();
        if (status == ApplicationStatus.PENDING || status == ApplicationStatus.UNDER_REVIEW) {
            application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        }
    }

    /**
     * An interview belongs permanently to the application it was created for. Allowing the
     * application to change would move the interview onto a different candidate/job and skip
     * the schedulability checks done at creation, so any mismatch is rejected.
     */
    private void requireApplicationUnchanged(Interview interview, UUID requestedApplicationId) {
        if (requestedApplicationId == null) {
            return;
        }
        Application current = interview.getApplication();
        if (current == null || !requestedApplicationId.equals(current.getPublicId())) {
            throw new ResourceConflictException(
                    "An interview's application cannot be changed.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Interview"));
        }
    }

    /**
     * A slot can host at most one interview. The interviewer is always the slot's owner,
     * so no interviewer matching is needed. This is a friendly early check; the unique
     * constraint on {@code interviews.slot_id} is the real guard against concurrent bookings.
     */
    private void validateBooking(Slot slot, Interview existingInterview, InterviewStatus targetStatus) {
        if (existingInterview != null && targetStatus == InterviewStatus.CANCELLED) {
            return;
        }
        if (slot == null) {
            throw new IllegalArgumentException("A non-cancelled interview requires a slot.");
        }

        Interview bookedInterview = slot.getInterview();
        if (bookedInterview != null && (existingInterview == null || !bookedInterview.getId().equals(existingInterview.getId()))) {
            throw new ResourceConflictException("Slot is already booked by another interview.",
                    ErrorCode.DATA_CONFLICT);
        }

        boolean newBooking = existingInterview == null
                || existingInterview.getSlot() == null
                || !slot.getPublicId().equals(existingInterview.getSlot().getPublicId())
                || (targetStatus == InterviewStatus.SCHEDULED && existingInterview.getStatus() != InterviewStatus.SCHEDULED);
        if (!newBooking) {
            return; // Editing an existing appointment's notes/outcome must still work after it starts.
        }
        if (slot.getStartAt() == null || !slot.getStartAt().isAfter(clock.instant())) {
            throw new ResourceConflictException("Cannot book a slot that has already started.",
                    ErrorCode.OPERATION_NOT_ALLOWED);
        }
        User interviewer = slot.getInterviewer();
        // Hold the current account state stable until booking commits. Deactivation cannot
        // slip between the eligibility check and the write; historical edits need no such lock.
        entityManager.refresh(interviewer, LockModeType.PESSIMISTIC_READ);
        if (!interviewer.isApproved() || !interviewer.hasRole(UserRole.INTERVIEWER)) {
            throw new ResourceConflictException("The slot's owner must be an active interviewer.",
                    ErrorCode.OPERATION_NOT_ALLOWED);
        }
        if (existingInterview != null && targetStatus == InterviewStatus.SCHEDULED) {
            requireSchedulableApplication(existingInterview.getApplication());
        }
    }

    /**
     * An interview's mode and its meeting URL must agree: an online interview needs a meeting
     * URL, and an in-person one must not carry one. Checked against the entity's final state so
     * it holds uniformly across create, full update and patch (where the merged state matters).
     */
    private void requireMeetingConsistency(Interview interview) {
        boolean online = Boolean.TRUE.equals(interview.getIsOnline());
        boolean hasMeetingUrl = interview.getMeetingUrl() != null && !interview.getMeetingUrl().isBlank();
        if (online && !hasMeetingUrl) {
            throw new ResourceConflictException(
                    "An online interview requires a meeting URL.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(FIELD, "meetingUrl"));
        }
        if (!online && hasMeetingUrl) {
            throw new ResourceConflictException(
                    "A meeting URL can only be set for an online interview.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(FIELD, "meetingUrl"));
        }
    }

    /**
     * An interview's recommendation is the hiring signal it produces, so it exists only for an
     * interview that actually took place. A COMPLETED interview must carry a recommendation, and
     * an interview in any other state (still scheduled, cancelled, or a no-show) must not. Checked
     * against the entity's final state so it holds uniformly across create, full update and patch.
     */
    private void requireRecommendationConsistency(Interview interview) {
        boolean completed = interview.getStatus() == InterviewStatus.COMPLETED;
        boolean hasRecommendation = interview.getRecommendation() != null;
        if (completed && !hasRecommendation) {
            throw new ResourceConflictException(
                    "A completed interview requires a recommendation.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(FIELD, "recommendation"));
        }
        if (!completed && hasRecommendation) {
            throw new ResourceConflictException(
                    "A recommendation can only be set on a completed interview.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(FIELD, "recommendation"));
        }
    }

    private Application resolveApplication(UUID publicId) {
        return applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID,
                        publicId));
    }

    private Slot resolveSlot(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("Slot ID cannot be null.");
        }
        return slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), PUBLIC_ID, publicId));
    }

    private void requireInterviewParticipantOrStaff(Interview interview) {
        if (SecurityUtilities.hasAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER)) {
            return;
        }
        User candidate = interview.getCandidate();
        User interviewer = interview.getInterviewer();
        var currentUser = SecurityUtilities.requireCurrentCustomUser();
        if (isUser(currentUser.getPublicId(), candidate) || isUser(currentUser.getPublicId(), interviewer)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to access this interview.");
    }

    private void requireInterviewInterviewerOrStaff(Interview interview) {
        if (SecurityUtilities.hasAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER)) {
            return;
        }
        User interviewer = interview.getInterviewer();
        if (interviewer == null || interviewer.getPublicId() == null) {
            throw new AccessDeniedException("You do not have permission to update this interview.");
        }
        SecurityUtilities.requireCurrentUser(interviewer.getPublicId());
    }

    private void requireSlotInterviewerOrStaff(Slot slot) {
        if (SecurityUtilities.hasAnyRole(UserRole.ADMIN, UserRole.HR_MANAGER)) {
            return;
        }
        if (slot == null) {
            return;
        }
        User interviewer = slot.getInterviewer();
        if (interviewer == null || interviewer.getPublicId() == null) {
            throw new AccessDeniedException("You do not have permission to use this slot.");
        }
        SecurityUtilities.requireCurrentUser(interviewer.getPublicId());
    }

    private boolean isUser(UUID currentUserPublicId, User user) {
        return user != null && currentUserPublicId.equals(user.getPublicId());
    }
}
