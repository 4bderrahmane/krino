package com.krino.backend.mapper;

import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SlotMapperTest
{
    private static final Instant START = Instant.parse("2099-01-01T10:00:00Z");
    private static final Instant END = Instant.parse("2099-01-01T10:30:00Z");

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
        dto.setStartAt(START);
        dto.setEndAt(END);

        Slot slot = slotMapper.toEntity(dto, interviewer);

        assertThat(slot.getInterviewer()).isSameAs(interviewer);
        assertThat(slot.getStartAt()).isEqualTo(START);
        assertThat(slot.getEndAt()).isEqualTo(END);
        assertThat(slot.getId()).isNull();
        // publicId is auto-assigned at construction (stable identity for transient
        // entities); only the DB surrogate id stays unset until persist.
        assertThat(slot.getPublicId()).isNotNull();
        assertThat(slot.getInterview()).isNull();
    }

    @Test
    void patchEntity_ignoresNullFields()
    {
        Slot existing = new Slot();
        existing.setStartAt(START);
        existing.setEndAt(END);

        SlotUpdateDTO dto = new SlotUpdateDTO();
        dto.setEndAt(Instant.parse("2099-01-01T11:00:00Z"));

        slotMapper.patchEntity(dto, existing);

        assertThat(existing.getStartAt()).isEqualTo(START);
        assertThat(existing.getEndAt()).isEqualTo(Instant.parse("2099-01-01T11:00:00Z"));
    }
}
