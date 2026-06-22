package com.krino.backend.mapper;

import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfiguration.class, uses = {UserMapper.class, JobMapper.class, SlotMapper.class})
public interface InterviewMapper {
    @Mapping(target = "id", source = "publicId")
    InterviewResponseDTO toResponse(Interview interview);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "interviewer", source = "interviewer")
    @Mapping(target = "candidate", source = "candidate")
    @Mapping(target = "job", source = "job")
    @Mapping(target = "slot", source = "slot")
    @Mapping(target = "status", source = "dto.status", defaultValue = "SCHEDULED")
    @Mapping(target = "notes", source = "dto.notes")
    @Mapping(target = "isOnline", source = "dto.isOnline")
    @Mapping(target = "meetingUrl", source = "dto.meetingUrl")
    Interview toEntity(InterviewRequestDTO dto, User interviewer, User candidate, Job job, Slot slot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "interviewer", source = "interviewer")
    @Mapping(target = "candidate", source = "candidate")
    @Mapping(target = "job", source = "job")
    @Mapping(target = "slot", source = "slot")
    @Mapping(target = "status", source = "dto.status",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "notes", source = "dto.notes")
    @Mapping(target = "isOnline", source = "dto.isOnline")
    @Mapping(target = "meetingUrl", source = "dto.meetingUrl")
    void updateEntity(InterviewRequestDTO dto, User interviewer, User candidate, Job job, Slot slot,
                      @MappingTarget Interview interview);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "interviewer", source = "interviewer")
    @Mapping(target = "candidate", source = "candidate")
    @Mapping(target = "job", source = "job")
    @Mapping(target = "slot", source = "slot")
    @Mapping(target = "status", source = "dto.status")
    @Mapping(target = "notes", source = "dto.notes")
    @Mapping(target = "isOnline", source = "dto.isOnline")
    @Mapping(target = "meetingUrl", source = "dto.meetingUrl")
    void patchEntity(InterviewRequestDTO dto, User interviewer, User candidate, Job job, Slot slot,
                     @MappingTarget Interview interview);
}
