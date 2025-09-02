package com.jesa.interviewslotmanager.DTO.User;

import com.jesa.interviewslotmanager.entity.UserRole;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class UserResponseDTO
{

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Set<UserRole> roles;
//    private Boolean isActive;
}
