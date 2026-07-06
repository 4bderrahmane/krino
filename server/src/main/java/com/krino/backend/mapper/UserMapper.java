package com.krino.backend.mapper;

import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.entity.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.HashSet;

@Mapper(config = MapperConfiguration.class)
public interface UserMapper {
    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "resumeFilename", source = "resumeOriginalFilename")
    UserResponseDTO toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "firstName", expression = "java(request.getFirstName() == null ? \"\" : request.getFirstName()" +
            ".trim())")
    @Mapping(target = "lastName", expression = "java(request.getLastName() == null ? \"\" : request.getLastName()" +
            ".trim())")
    @Mapping(target = "phoneNumber", expression = "java(request.getPhoneNumber() == null ? null : request" +
            ".getPhoneNumber().trim())")
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "mustChangePassword", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "resumeObjectKey", ignore = true)
    @Mapping(target = "resumeOriginalFilename", ignore = true)
    @Mapping(target = "resumeContentType", ignore = true)
    @Mapping(target = "resumeSizeBytes", ignore = true)
    @Mapping(target = "resumeUploadedAt", ignore = true)
    User toEntity(UserRegistrationDTO request, String email, String password);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "mustChangePassword", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "resumeObjectKey", ignore = true)
    @Mapping(target = "resumeOriginalFilename", ignore = true)
    @Mapping(target = "resumeContentType", ignore = true)
    @Mapping(target = "resumeSizeBytes", ignore = true)
    @Mapping(target = "resumeUploadedAt", ignore = true)
    void updateEntity(UserUpdateDTO dto, String email, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "mustChangePassword", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "resumeObjectKey", ignore = true)
    @Mapping(target = "resumeOriginalFilename", ignore = true)
    @Mapping(target = "resumeContentType", ignore = true)
    @Mapping(target = "resumeSizeBytes", ignore = true)
    @Mapping(target = "resumeUploadedAt", ignore = true)
    void patchEntity(UserUpdateDTO dto, String email, @MappingTarget User user);

    @AfterMapping
    default void initializeRoles(@MappingTarget User user) {
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
    }
}
