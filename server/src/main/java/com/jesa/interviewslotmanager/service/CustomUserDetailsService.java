package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.entity.User;
import com.jesa.interviewslotmanager.entity.CustomUserDetails;
import com.jesa.interviewslotmanager.exception.UserNotFoundException;
import com.jesa.interviewslotmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService
{

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(user);
    }


    public CustomUserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return new CustomUserDetails(user);
    }
}
