package com.krino.backend.mapper;

import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.support.TestJobs;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.InterviewRecommendation;
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
        dto.setApplicationId(UUID.randomUUID());
        dto.setSlotId(UUID.randomUUID());
        dto.setNotes("Bring a laptop");
        dto.setIsOnline(true);
        dto.setMeetingUrl("https://meet.example/abc");

        User interviewer = new User();
        User candidate = new User();
        Job job = TestJobs.draft("Backend Engineer");
        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        Slot slot = new Slot();

        Interview interview = interviewMapper.toEntity(dto, interviewer, application, slot);

        assertThat(interview.getInterviewer()).isSameAs(interviewer);
        assertThat(interview.getApplication()).isSameAs(application);
        assertThat(interview.getCandidate()).isSameAs(candidate);
        assertThat(interview.getJob()).isSameAs(job);
        assertThat(interview.getSlot()).isSameAs(slot);
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(interview.getNotes()).isEqualTo("Bring a laptop");
        assertThat(interview.getIsOnline()).isTrue();
        assertThat(interview.getMeetingUrl()).isEqualTo("https://meet.example/abc");
        assertThat(interview.getId()).isNull();
        // publicId is auto-assigned at construction (stable identity for transient
        // entities); only the DB surrogate id stays unset until persist.
        assertThat(interview.getPublicId()).isNotNull();
    }

    @Test
    void patchEntity_updatesNotesButIgnoresNullStatusAndAssociations()
    {
        Interview existing = new Interview();
        existing.setStatus(InterviewStatus.COMPLETED);
        existing.setNotes("Old notes");

        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setNotes("New notes");

        interviewMapper.patchEntity(dto, null, null, existing);

        assertThat(existing.getNotes()).isEqualTo("New notes");
        assertThat(existing.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
    }

    @Test
    void toEntity_mapsRecommendation()
    {
        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setApplicationId(UUID.randomUUID());
        dto.setSlotId(UUID.randomUUID());
        dto.setStatus(InterviewStatus.COMPLETED);
        dto.setRecommendation(InterviewRecommendation.STRONG_YES);

        Application application = new Application();
        application.setCandidate(new User());
        application.setJob(TestJobs.draft("Backend Engineer"));

        Interview interview = interviewMapper.toEntity(dto, new User(), application, new Slot());

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(interview.getRecommendation()).isEqualTo(InterviewRecommendation.STRONG_YES);
    }

    @Test
    void patchEntity_appliesRecommendationButKeepsItWhenOmitted()
    {
        Interview existing = new Interview();
        existing.setStatus(InterviewStatus.COMPLETED);
        existing.setRecommendation(InterviewRecommendation.YES);

        InterviewRequestDTO change = new InterviewRequestDTO();
        change.setRecommendation(InterviewRecommendation.STRONG_NO);
        interviewMapper.patchEntity(change, null, null, existing);
        assertThat(existing.getRecommendation()).isEqualTo(InterviewRecommendation.STRONG_NO);

        // a patch that omits the recommendation must preserve the stored value
        interviewMapper.patchEntity(new InterviewRequestDTO(), null, null, existing);
        assertThat(existing.getRecommendation()).isEqualTo(InterviewRecommendation.STRONG_NO);
    }
}
