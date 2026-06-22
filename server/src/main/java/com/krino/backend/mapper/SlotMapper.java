package com.krino.backend.mapper;

import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfiguration.class, uses = UserMapper.class)
public interface SlotMapper {
    @Mapping(target = "id", source = "publicId")
    SlotResponseDTO toResponse(Slot slot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "interviewer", source = "interviewer")
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "interview", ignore = true)
    Slot toEntity(SlotRequestDTO dto, User interviewer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "interviewer", ignore = true)
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "interview", ignore = true)
    void updateEntity(SlotUpdateDTO dto, @MappingTarget Slot slot);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "interviewer", ignore = true)
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "interview", ignore = true)
    void patchEntity(SlotUpdateDTO dto, @MappingTarget Slot slot);
}
