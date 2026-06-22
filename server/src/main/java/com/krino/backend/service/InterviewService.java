package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.InterviewMapper;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class InterviewService
{

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final SlotRepository slotRepository;
    private final JobRepository jobRepository;
    private final InterviewMapper interviewMapper;

    public InterviewResponseDTO createInterview(InterviewRequestDTO interviewRequestDTO)
    {
        User candidate = resolveUser(interviewRequestDTO.getCandidateId());
        Job job = resolveJob(interviewRequestDTO.getJobId());
        Slot slot = resolveSlot(interviewRequestDTO.getSlotId());

        validateBooking(slot, null);

        Interview interview = interviewMapper.toEntity(interviewRequestDTO, slot.getInterviewer(), candidate, job, slot);

        Interview savedInterview = interviewRepository.save(interview);
        return interviewMapper.toResponse(savedInterview);
    }

    public InterviewResponseDTO getInterviewByPublicId(UUID publicId)
    {
        Interview interview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));
        return interviewMapper.toResponse(interview);
    }

    public PageResponse<InterviewResponseDTO> getAllInterviews(Pageable pageable)
    {
        return PageResponse.from(interviewRepository.findAll(pageable),
                interviewMapper::toResponse);
    }

    public InterviewResponseDTO updateInterview(UUID publicId, InterviewRequestDTO interviewRequestDTO)
    {
        Interview existingInterview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));
        User candidate = resolveUser(interviewRequestDTO.getCandidateId());
        Job job = resolveJob(interviewRequestDTO.getJobId());
        Slot slot = resolveSlot(interviewRequestDTO.getSlotId());

        validateBooking(slot, existingInterview);

        interviewMapper.updateEntity(interviewRequestDTO, slot.getInterviewer(), candidate, job, slot,
                existingInterview);

        Interview updatedInterview = interviewRepository.save(existingInterview);
        return interviewMapper.toResponse(updatedInterview);
    }

    public InterviewResponseDTO patchInterview(UUID publicId, InterviewRequestDTO interviewRequestDTO)
    {
        Interview existingInterview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));

        Slot slot = interviewRequestDTO.getSlotId() != null
                ? resolveSlot(interviewRequestDTO.getSlotId())
                : existingInterview.getSlot();

        validateBooking(slot, existingInterview);

        User candidate = interviewRequestDTO.getCandidateId() != null
                ? resolveUser(interviewRequestDTO.getCandidateId())
                : existingInterview.getCandidate();
        Job job = interviewRequestDTO.getJobId() != null
                ? resolveJob(interviewRequestDTO.getJobId())
                : existingInterview.getJob();
        interviewMapper.patchEntity(interviewRequestDTO, slot.getInterviewer(), candidate, job, slot,
                existingInterview);

        Interview patchedInterview = interviewRepository.save(existingInterview);
        return interviewMapper.toResponse(patchedInterview);
    }

    public void deleteInterview(UUID publicId)
    {
        Interview interview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));
        interview.setSlot(null); // free the slot
        interviewRepository.delete(interview);
    }

    /**
     * A slot can host at most one interview. The interviewer is always the slot's owner,
     * so no interviewer matching is needed. This is a friendly early check; the unique
     * constraint on {@code interviews.slot_id} is the real guard against concurrent bookings.
     */
    private void validateBooking(Slot slot, Interview existingInterview)
    {
        Interview bookedInterview = slot.getInterview();
        if (bookedInterview != null
                && (existingInterview == null || !bookedInterview.getId().equals(existingInterview.getId())))
        {
            throw new ResourceConflictException(
                    "Slot is already booked by another interview.",
                    ErrorCode.DATA_CONFLICT);
        }
    }

    private User resolveUser(UUID publicId)
    {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", publicId));
    }

    private Job resolveJob(UUID publicId)
    {
        return jobRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Job.class.getSimpleName(), "publicId", publicId));
    }

    private Slot resolveSlot(UUID publicId)
    {
        return slotRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", publicId));
    }
}
