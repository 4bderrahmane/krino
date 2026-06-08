package com.krino.backend.dto.user;

import com.krino.backend.entity.UserRole;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class UserResponseDTO
{

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Set<UserRole> roles;
//    private Boolean isActive;
}
