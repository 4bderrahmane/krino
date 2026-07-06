package com.krino.backend.dto.user;

import com.krino.backend.entity.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Set<UserRole> roles;
    private String resumeFilename;
    private LocalDateTime resumeUploadedAt;
    private boolean mustChangePassword;
    private boolean approved;
    private boolean emailVerified;
}
