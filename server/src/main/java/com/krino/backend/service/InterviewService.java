package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
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
    private final ModelMapper modelMapper;

    public InterviewResponseDTO createInterview(InterviewRequestDTO interviewRequestDTO)
    {
        User interviewer = userRepository.findByPublicId(interviewRequestDTO.getInterviewerId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", interviewRequestDTO.getInterviewerId()));
        User candidate = userRepository.findByPublicId(interviewRequestDTO.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", interviewRequestDTO.getCandidateId()));
        Slot slot = slotRepository.findByPublicId(interviewRequestDTO.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", interviewRequestDTO.getSlotId()));

        Interview interview = new Interview();
        interview.setInterviewer(interviewer);
        interview.setCandidate(candidate);
        interview.setSlot(slot);
        interview.setNotes(interviewRequestDTO.getNotes());
        interview.setIsOnline(interviewRequestDTO.getIsOnline());

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
        var interviewer = userRepository.findByPublicId(interviewRequestDTO.getInterviewerId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", interviewRequestDTO.getInterviewerId()));
        var candidate = userRepository.findByPublicId(interviewRequestDTO.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", interviewRequestDTO.getCandidateId()));
        var slot = slotRepository.findByPublicId(interviewRequestDTO.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", interviewRequestDTO.getSlotId()));

        existingInterview.setInterviewer(interviewer);
        existingInterview.setCandidate(candidate);
        existingInterview.setSlot(slot);
        existingInterview.setNotes(interviewRequestDTO.getNotes());
        existingInterview.setIsOnline(interviewRequestDTO.getIsOnline());

        Interview updatedInterview = interviewRepository.save(existingInterview);
        return modelMapper.map(updatedInterview, InterviewResponseDTO.class);
    }

    public InterviewResponseDTO patchInterview(UUID publicId, InterviewRequestDTO interviewRequestDTO)
    {
        Interview existingInterview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));

        if (interviewRequestDTO.getInterviewerId() != null)
        {
            User interviewer = userRepository.findByPublicId(interviewRequestDTO.getInterviewerId())
                    .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", interviewRequestDTO.getInterviewerId()));
            existingInterview.setInterviewer(interviewer);
        }
        if (interviewRequestDTO.getCandidateId() != null)
        {
            User candidate = userRepository.findByPublicId(interviewRequestDTO.getCandidateId())
                    .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "publicId", interviewRequestDTO.getCandidateId()));
            existingInterview.setCandidate(candidate);
        }
        if (interviewRequestDTO.getSlotId() != null)
        {
            Slot slot = slotRepository.findByPublicId(interviewRequestDTO.getSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "publicId", interviewRequestDTO.getSlotId()));
            existingInterview.setSlot(slot);
        }
        if (interviewRequestDTO.getNotes() != null)
        {
            existingInterview.setNotes(interviewRequestDTO.getNotes());
        }
        if (interviewRequestDTO.getIsOnline() != null)
        {
            existingInterview.setIsOnline(interviewRequestDTO.getIsOnline());
        }

        Interview patchedInterview = interviewRepository.save(existingInterview);
        return modelMapper.map(patchedInterview, InterviewResponseDTO.class);
    }

    public void deleteInterview(UUID publicId)
    {
        Interview interview = interviewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "publicId", publicId));
        interviewRepository.delete(interview);
    }
}
