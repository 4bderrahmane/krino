package com.InterviewManager.interview_slot_manager.service;

import com.InterviewManager.interview_slot_manager.DTO.Authentication.AuthenticationResponseDTO;
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
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;

    public AuthenticationResponseDTO register(UserRegistrationDTO request)
    {
        if (userRepository.findByEmail(request.getEmail()).isPresent())
        {
            throw new EmailAlreadyExistsException("An account with this email already exists: " + request.getEmail());
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent())
        {
            throw new UserAlreadyExistsException("An account with this username already exists: " + request.getUsername());
        }

        User user = new User(
                request.getEmail(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName()
        );
        user.setPhoneNumber(request.getPhoneNumber());
        user.addRole(UserRole.CANDIDATE);
        userRepository.save(user);

        UserDetails userDetails = new UserPrincipal(user);
        String jwtToken = jwtUtil.generateToken(userDetails);
        UserResponseDTO userResponse = modelMapper.map(user, UserResponseDTO.class);

        return new AuthenticationResponseDTO(jwtToken, userResponse);
    }

    public AuthenticationResponseDTO login(UserLoginDTO request)
    {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found after authentication"));

        UserDetails userDetails = new UserPrincipal(user);
        String jwtToken = jwtUtil.generateToken(userDetails);
        UserResponseDTO userResponse = modelMapper.map(user, UserResponseDTO.class);

        return new AuthenticationResponseDTO(jwtToken, userResponse);
    }

}