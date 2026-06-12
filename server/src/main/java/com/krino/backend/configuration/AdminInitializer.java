package com.krino.backend.configuration;

import com.krino.backend.entity.UserRole;
import com.krino.backend.entity.User;
import com.krino.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.Set;


@Component
@Profile("dev")
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner
{

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInitializer.class);

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String @NonNull ... args)
    {
        if (userRepository.findByEmail(adminEmail).isPresent())
        {
            LOGGER.info("Admin '{}' already exists, skipping seed.", adminEmail);
            return;
        }

        var admin = User.builder()
                .firstName("Abderrahmane")
                .lastName("Khbabez")
                .email(adminEmail)
                .phoneNumber("123456789")
                .isApproved(true)
                .password(passwordEncoder.encode(adminPassword))
                .roles(Set.of(UserRole.ADMIN))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(admin);
        LOGGER.info("Admin '{}' created.", adminEmail);
    }
}
