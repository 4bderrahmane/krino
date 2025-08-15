package com.InterviewManager.interview_slot_manager.config;

import com.InterviewManager.interview_slot_manager.entity.UserRole;
import com.InterviewManager.interview_slot_manager.entity.User;
import com.InterviewManager.interview_slot_manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner
{
    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    @Override
    public void run(String... args) throws Exception
    {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            var adminUser = User.builder()
                    .username("Admin")
                    .firstName("Abderrahmane")
                    .lastName("Khbabez")
                    .email(adminEmail)
                    .phoneNumber("767572988")
                    .isApproved(true)
                    .createdAt(LocalDateTime.now())
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(Set.of(UserRole.ADMIN))
                    .build();

            userRepository.save(adminUser);
            logger.info("✅ Default ADMIN user created successfully.");
        }
    }

}
