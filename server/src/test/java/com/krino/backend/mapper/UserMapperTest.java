package com.krino.backend.mapper;

import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest
{
    private UserMapper userMapper;

    @BeforeEach
    void setUp()
    {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void toResponse_exposesPublicIdAsId()
    {
        UUID publicId = UUID.randomUUID();
        User user = User.builder()
                .id(5L)
                .publicId(publicId)
                .email("candidate@test.local")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .roles(Set.of(UserRole.CANDIDATE))
                .build();

        UserResponseDTO response = userMapper.toResponse(user);

        assertThat(response.getId()).isEqualTo(publicId);
        assertThat(response.getEmail()).isEqualTo("candidate@test.local");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getRoles()).containsExactly(UserRole.CANDIDATE);
    }

    @Test
    void toEntity_trimsNamesAndAppliesNormalizedEmailAndPassword()
    {
        UserRegistrationDTO request = new UserRegistrationDTO();
        request.setFirstName("  John  ");
        request.setLastName("  Doe  ");
        request.setPhoneNumber("  123456789  ");

        User user = userMapper.toEntity(request, "candidate@test.local", "encoded-password");

        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getPhoneNumber()).isEqualTo("123456789");
        assertThat(user.getEmail()).isEqualTo("candidate@test.local");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getId()).isNull();
        assertThat(user.getPublicId()).isNull();
        assertThat(user.isApproved()).isFalse();
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void toEntity_nullNamesBecomeEmptyAndNullPhoneStaysNull()
    {
        UserRegistrationDTO request = new UserRegistrationDTO();

        User user = userMapper.toEntity(request, "candidate@test.local", "encoded-password");

        assertThat(user.getFirstName()).isEmpty();
        assertThat(user.getLastName()).isEmpty();
        assertThat(user.getPhoneNumber()).isNull();
    }

    @Test
    void updateEntity_overwritesFieldsIncludingNulls()
    {
        User user = User.builder()
                .firstName("Old")
                .lastName("Name")
                .phoneNumber("999999999")
                .build();

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setFirstName(null);
        dto.setLastName("New");

        userMapper.updateEntity(dto, "new@test.local", user);

        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isEqualTo("New");
        assertThat(user.getEmail()).isEqualTo("new@test.local");
    }

    @Test
    void patchEntity_ignoresNullFields()
    {
        User user = User.builder()
                .firstName("Old")
                .lastName("Name")
                .build();

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setLastName("New");

        userMapper.patchEntity(dto, "new@test.local", user);

        assertThat(user.getFirstName()).isEqualTo("Old");
        assertThat(user.getLastName()).isEqualTo("New");
        assertThat(user.getEmail()).isEqualTo("new@test.local");
    }
}
