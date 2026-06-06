package com.krino.backend.service;

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
import org.springframework.stereotype.Service;

import java.util.List;

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
        User interviewer = userRepository.findById(interviewRequestDTO.getInterviewerId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", interviewRequestDTO.getInterviewerId()));
        User candidate = userRepository.findById(interviewRequestDTO.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", interviewRequestDTO.getCandidateId()));
        Slot slot = slotRepository.findById(interviewRequestDTO.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "id", interviewRequestDTO.getSlotId()));

        Interview interview = new Interview();
        interview.setInterviewer(interviewer);
        interview.setCandidate(candidate);
        interview.setSlot(slot);
        interview.setNotes(interviewRequestDTO.getNotes());
        interview.setIsOnline(interviewRequestDTO.getIsOnline());

        Interview savedInterview = interviewRepository.save(interview);
        return modelMapper.map(savedInterview, InterviewResponseDTO.class);
    }

    public InterviewResponseDTO getInterviewById(Long interviewId)
    {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "id", interviewId));
        return modelMapper.map(interview, InterviewResponseDTO.class);
    }

    public List<InterviewResponseDTO> getAllInterviews()
    {
        return interviewRepository.findAll().stream()
                .map(interview -> modelMapper.map(interview, InterviewResponseDTO.class))
                .toList();
    }

    public InterviewResponseDTO updateInterview(Long interviewId, InterviewRequestDTO interviewRequestDTO)
    {
        Interview existingInterview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "id", interviewId));
        var interviewer = userRepository.findById(interviewRequestDTO.getInterviewerId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", interviewRequestDTO.getInterviewerId()));
        var candidate = userRepository.findById(interviewRequestDTO.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", interviewRequestDTO.getCandidateId()));
        var slot = slotRepository.findById(interviewRequestDTO.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "id", interviewRequestDTO.getSlotId()));

        existingInterview.setInterviewer(interviewer);
        existingInterview.setCandidate(candidate);
        existingInterview.setSlot(slot);
        existingInterview.setNotes(interviewRequestDTO.getNotes());
        existingInterview.setIsOnline(interviewRequestDTO.getIsOnline());

        Interview updatedInterview = interviewRepository.save(existingInterview);
        return modelMapper.map(updatedInterview, InterviewResponseDTO.class);
    }

    public InterviewResponseDTO patchInterview(Long interviewId, InterviewRequestDTO interviewRequestDTO)
    {
        Interview existingInterview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(Interview.class.getSimpleName(), "id", interviewId));

        if (interviewRequestDTO.getInterviewerId() != null)
        {
            User interviewer = userRepository.findById(interviewRequestDTO.getInterviewerId())
                    .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", interviewRequestDTO.getInterviewerId()));
            existingInterview.setInterviewer(interviewer);
        }
        if (interviewRequestDTO.getCandidateId() != null)
        {
            User candidate = userRepository.findById(interviewRequestDTO.getCandidateId())
                    .orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), "id", interviewRequestDTO.getCandidateId()));
            existingInterview.setCandidate(candidate);
        }
        if (interviewRequestDTO.getSlotId() != null)
        {
            Slot slot = slotRepository.findById(interviewRequestDTO.getSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException(Slot.class.getSimpleName(), "id", interviewRequestDTO.getSlotId()));
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

    public void deleteInterview(Long interviewId)
    {
        if (!interviewRepository.existsById(interviewId))
        {
            throw new ResourceNotFoundException(Interview.class.getSimpleName(), "id", interviewId);
        }
        interviewRepository.deleteById(interviewId);
    }
}
