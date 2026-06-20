package com.krino.backend.service;

import com.krino.backend.entity.User;
import com.krino.backend.entity.UserRole;
import com.krino.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest
{
    @Test
    void loadUserByUsernameNormalizesEmail()
    {
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setId(1L);
        user.setPublicId(UUID.randomUUID());
        user.setEmail("candidate@test.local");
        user.setPassword("encoded");
        user.setApproved(true);
        user.setRoles(Set.of(UserRole.CANDIDATE));

        when(userRepository.findByEmail("candidate@test.local")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        UserDetails details = service.loadUserByUsername("Candidate@TEST.Local");

        assertThat(details.getUsername()).isEqualTo("candidate@test.local");
        verify(userRepository).findByEmail("candidate@test.local");
    }
}
