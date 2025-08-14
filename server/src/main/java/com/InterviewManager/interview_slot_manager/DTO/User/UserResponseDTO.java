package com.InterviewManager.interview_slot_manager.DTO.User;

import com.InterviewManager.interview_slot_manager.entity.UserRole;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class UserResponseDTO {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Set<UserRole> roles;
//    private Boolean isActive;
}
