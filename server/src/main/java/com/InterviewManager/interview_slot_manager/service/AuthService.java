package com.InterviewManager.interview_slot_manager.service;

import com.InterviewManager.interview_slot_manager.DTO.Authentication.AuthenticationResponseDTO;
import com.InterviewManager.interview_slot_manager.DTO.Authentication.RegistrationResponseDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserLoginDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserRegistrationDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserResponseDTO;
import com.InterviewManager.interview_slot_manager.entity.User;
import com.InterviewManager.interview_slot_manager.entity.UserPrincipal;
import com.InterviewManager.interview_slot_manager.entity.UserRole;
import com.InterviewManager.interview_slot_manager.exception.EmailAlreadyExistsException;
import com.InterviewManager.interview_slot_manager.exception.UserAlreadyExistsException;
import com.InterviewManager.interview_slot_manager.repository.UserRepository;
import com.InterviewManager.interview_slot_manager.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    private final JwtBlacklistService jwtBlacklistService;

    @Transactional
    public RegistrationResponseDTO register(@NonNull final UserRegistrationDTO request)
    {
        if (userRepository.findByEmail(request.getEmail()).isPresent())
        {
            throw new EmailAlreadyExistsException("An account with this email already exists: " + request.getEmail());
        }
        if (userRepository.findByUsername(request.getUsername().toLowerCase()).isPresent())
        {
            throw new UserAlreadyExistsException("An account with this username already exists: " + request.getUsername());
        }

        User user = new User(
                request.getEmail(),
                request.getUsername().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName()
        );
        user.setPhoneNumber(request.getPhoneNumber());
        user.addRole(UserRole.CANDIDATE);
        userRepository.save(user);

        UserResponseDTO userResponse = modelMapper.map(user, UserResponseDTO.class);

        return new RegistrationResponseDTO(userResponse, "User registered successfully.");
    }

    @Transactional
    public AuthenticationResponseDTO login(@NonNull final UserLoginDTO request)
    {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + request.getEmail()));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = new UserPrincipal(user);
        String jwtToken = jwtUtil.generateToken(userDetails);


        UserResponseDTO userResponse = modelMapper.map(user, UserResponseDTO.class);

        return new AuthenticationResponseDTO(jwtToken, userResponse);
    }

    public String logout(@NonNull final String authHeader)
    {
        String token;
        if (authHeader.startsWith("Bearer "))
        {
            token = authHeader.substring(7);
        } else
        {
            token = authHeader;
        }

        try
        {
            String username = jwtUtil.extractUsername(token);

            if (username != null)
            {
                // Simply blacklist the token
                jwtBlacklistService.blacklistToken(token);

                log.info("User {} logged out successfully", username);
                return "Logged out successfully";
            } else
            {
                log.warn("Invalid token provided for logout");
                return "Invalid token";
            }
        } catch (Exception e)
        {
            log.error("Error during logout: {}", e.getMessage());
            return "Logout failed: " + e.getMessage();
        }
    }
}