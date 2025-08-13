package com.InterviewManager.interview_slot_manager.DTO.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class UserUpdateDTO {

    @Size(min = 2, max = 100)
    private String username;

    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    @Email
    private String email;

    @Size(min = 9, max = 9)
    private String phoneNumber;
}
