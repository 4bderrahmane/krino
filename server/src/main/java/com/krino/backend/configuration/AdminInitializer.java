package com.krino.backend.configuration;

import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.entity.User;
import com.krino.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.util.Set;


@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String @NonNull ... args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.warn("No admin credentials configured (APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD); "
                    + "skipping admin bootstrap. Set both to seed the first ADMIN account.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.info("Admin '{}' already exists, skipping seed.", normalizedEmail);
            return;
        }

        var admin = User.builder()
                .firstName("System")
                .lastName("Administrator")
                .email(normalizedEmail)
                .isApproved(true)
                .password(passwordEncoder.encode(adminPassword))
                .roles(Set.of(UserRole.ADMIN))
                .build();

        userRepository.save(admin);
        log.info("Admin '{}' created.", normalizedEmail);
    }
}
