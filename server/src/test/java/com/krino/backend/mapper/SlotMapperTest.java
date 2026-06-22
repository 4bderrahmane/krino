package com.krino.backend.mapper;

import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class SlotMapperTest
{
    private SlotMapper slotMapper;

    @BeforeEach
    void setUp()
    {
        slotMapper = Mappers.getMapper(SlotMapper.class);
    }

    @Test
    void toEntity_assignsInterviewerAndLeavesIdentitiesUnset()
    {
        User interviewer = new User();
        SlotRequestDTO dto = new SlotRequestDTO();
        dto.setInterviewDate(LocalDate.of(2099, 1, 1));
        dto.setStartTime(LocalTime.of(10, 0));
        dto.setEndTime(LocalTime.of(10, 30));

        Slot slot = slotMapper.toEntity(dto, interviewer);

        assertThat(slot.getInterviewer()).isSameAs(interviewer);
        assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(slot.getId()).isNull();
        assertThat(slot.getPublicId()).isNull();
        assertThat(slot.getInterview()).isNull();
    }

    @Test
    void patchEntity_ignoresNullFields()
    {
        Slot existing = new Slot();
        existing.setInterviewDate(LocalDate.of(2099, 1, 1));
        existing.setStartTime(LocalTime.of(9, 0));
        existing.setEndTime(LocalTime.of(9, 30));

        SlotUpdateDTO dto = new SlotUpdateDTO();
        dto.setEndTime(LocalTime.of(10, 0));

        slotMapper.patchEntity(dto, existing);

        assertThat(existing.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(existing.getInterviewDate()).isEqualTo(LocalDate.of(2099, 1, 1));
        assertThat(existing.getEndTime()).isEqualTo(LocalTime.of(10, 0));
    }
}
