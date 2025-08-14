package com.InterviewManager.interview_slot_manager.DTO.Authentication;

import com.InterviewManager.interview_slot_manager.DTO.User.UserResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponseDTO {
    private String token;
    private UserResponseDTO user;
}
