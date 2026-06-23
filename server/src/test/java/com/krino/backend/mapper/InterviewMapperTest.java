package com.krino.backend.mapper;

import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.InterviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewMapperTest
{
    private InterviewMapper interviewMapper;

    @BeforeEach
    void setUp()
    {
        interviewMapper = Mappers.getMapper(InterviewMapper.class);
    }

    @Test
    void toEntity_assignsAssociationsAndDefaultsStatusToScheduled()
    {
        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setCandidateId(UUID.randomUUID());
        dto.setJobId(UUID.randomUUID());
        dto.setSlotId(UUID.randomUUID());
        dto.setNotes("Bring a laptop");
        dto.setIsOnline(true);
        dto.setMeetingUrl("https://meet.example/abc");

        User interviewer = new User();
        User candidate = new User();
        Job job = new Job();
        Slot slot = new Slot();

        Interview interview = interviewMapper.toEntity(dto, interviewer, candidate, job, slot);

        assertThat(interview.getInterviewer()).isSameAs(interviewer);
        assertThat(interview.getCandidate()).isSameAs(candidate);
        assertThat(interview.getJob()).isSameAs(job);
        assertThat(interview.getSlot()).isSameAs(slot);
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(interview.getNotes()).isEqualTo("Bring a laptop");
        assertThat(interview.getIsOnline()).isTrue();
        assertThat(interview.getMeetingUrl()).isEqualTo("https://meet.example/abc");
        assertThat(interview.getId()).isNull();
        assertThat(interview.getPublicId()).isNull();
    }

    @Test
    void patchEntity_updatesNotesButIgnoresNullStatusAndAssociations()
    {
        Interview existing = new Interview();
        existing.setStatus(InterviewStatus.COMPLETED);
        existing.setNotes("Old notes");

        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setNotes("New notes");

        interviewMapper.patchEntity(dto, null, null, null, null, existing);

        assertThat(existing.getNotes()).isEqualTo("New notes");
        assertThat(existing.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
    }
}
