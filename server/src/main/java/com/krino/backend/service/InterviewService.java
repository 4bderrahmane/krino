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
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;

    public InterviewResponseDTO createInterview(InterviewRequestDTO interviewRequestDTO)
    {
        User interviewer = resolveUser(interviewRequestDTO.getInterviewerId());
        User candidate = resolveUser(interviewRequestDTO.getCandidateId());
        Job job = resolveJob(interviewRequestDTO.getJobId());
        Slot slot = resolveSlot(interviewRequestDTO.getSlotId());

        validateBooking(slot, interviewer, null);

        Interview interview = new Interview();
        interview.setInterviewer(interviewer);
        interview.setCandidate(candidate);
        interview.setJob(job);
        interview.setSlot(slot);
        if (interviewRequestDTO.getStatus() != null)
        {
            interview.setStatus(interviewRequestDTO.getStatus());
        }
        interview.setNotes(interviewRequestDTO.getNotes());
        interview.setIsOnline(interviewRequestDTO.getIsOnline());
        interview.setMeetingUrl(interviewRequestDTO.getMeetingUrl());

        Interview savedInterview = interviewRepository.save(interview);
        return modelMapper.map(savedInterview, InterviewResponseDTO.class);
    }

    public InterviewResponseDTO getInterviewByPublicId(UUID publicId)
    {
        Interview interview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));
        return modelMapper.map(interview, InterviewResponseDTO.class);
    }

    public PageResponse<InterviewResponseDTO> getAllInterviews(Pageable pageable)
    {
        return PageResponse.from(interviewRepository.findAll(pageable),
                interview -> modelMapper.map(interview, InterviewResponseDTO.class));
    }

    public InterviewResponseDTO updateInterview(UUID publicId, InterviewRequestDTO interviewRequestDTO)
    {
        Interview existingInterview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));
        User interviewer = resolveUser(interviewRequestDTO.getInterviewerId());
        User candidate = resolveUser(interviewRequestDTO.getCandidateId());
        Job job = resolveJob(interviewRequestDTO.getJobId());
        Slot slot = resolveSlot(interviewRequestDTO.getSlotId());

        validateBooking(slot, interviewer, existingInterview);

        existingInterview.setInterviewer(interviewer);
        existingInterview.setCandidate(candidate);
        existingInterview.setJob(job);
        existingInterview.setSlot(slot);
        if (interviewRequestDTO.getStatus() != null)
        {
            existingInterview.setStatus(interviewRequestDTO.getStatus());
        }
        existingInterview.setNotes(interviewRequestDTO.getNotes());
        existingInterview.setIsOnline(interviewRequestDTO.getIsOnline());
        existingInterview.setMeetingUrl(interviewRequestDTO.getMeetingUrl());

        Interview updatedInterview = interviewRepository.save(existingInterview);
        return modelMapper.map(updatedInterview, InterviewResponseDTO.class);
    }

    public InterviewResponseDTO patchInterview(UUID publicId, InterviewRequestDTO interviewRequestDTO)
    {
        Interview existingInterview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));

        User interviewer = interviewRequestDTO.getInterviewerId() != null
                ? resolveUser(interviewRequestDTO.getInterviewerId())
                : existingInterview.getInterviewer();
        Slot slot = interviewRequestDTO.getSlotId() != null
                ? resolveSlot(interviewRequestDTO.getSlotId())
                : existingInterview.getSlot();

        validateBooking(slot, interviewer, existingInterview);

        existingInterview.setInterviewer(interviewer);
        if (interviewRequestDTO.getCandidateId() != null)
        {
            existingInterview.setCandidate(resolveUser(interviewRequestDTO.getCandidateId()));
        }
        if (interviewRequestDTO.getJobId() != null)
        {
            existingInterview.setJob(resolveJob(interviewRequestDTO.getJobId()));
        }
        if (interviewRequestDTO.getSlotId() != null)
        {
            existingInterview.setSlot(slot);
        }
        if (interviewRequestDTO.getStatus() != null)
        {
            existingInterview.setStatus(interviewRequestDTO.getStatus());
        }
        if (interviewRequestDTO.getNotes() != null)
        {
            existingInterview.setNotes(interviewRequestDTO.getNotes());
        }
        if (interviewRequestDTO.getIsOnline() != null)
        {
            existingInterview.setIsOnline(interviewRequestDTO.getIsOnline());
        }
        if (interviewRequestDTO.getMeetingUrl() != null)
        {
            existingInterview.setMeetingUrl(interviewRequestDTO.getMeetingUrl());
        }

        Interview patchedInterview = interviewRepository.save(existingInterview);
        return modelMapper.map(patchedInterview, InterviewResponseDTO.class);
    }

    public void deleteInterview(UUID publicId)
    {
        Interview interview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));
        interview.setSlot(null); // free the slot
        interviewRepository.delete(interview);
    }

    /**
     * A slot can only host one interview, and only for the interviewer whose
     * availability it represents.
     */
    private void validateBooking(Slot slot, User interviewer, Interview existingInterview)
    {
        Interview bookedInterview = slot.getInterview();
        if (bookedInterview != null
                && (existingInterview == null || !bookedInterview.getId().equals(existingInterview.getId())))
        {
            throw new ResourceConflictException(
                    "Slot is already booked by another interview.",
                    ErrorCode.DATA_CONFLICT);
        }

        if (!slot.getInterviewer().getId().equals(interviewer.getId()))
        {
            throw new ResourceConflictException(
                    "Slot does not belong to the specified interviewer.",
                    ErrorCode.OPERATION_NOT_ALLOWED);
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
