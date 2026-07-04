package com.krino.backend.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Slf4j
@Configuration
public class SecureRandomConfiguration {

    @Bean
    public SecureRandom secureRandom() {
        try {
            SecureRandom random = SecureRandom.getInstance("NativePRNGBlocking");
            log.info("SecureRandom: using '{}' (backed by /dev/random)", random.getAlgorithm());
            return random;
        } catch (NoSuchAlgorithmException _) {
            try {
                SecureRandom strong = SecureRandom.getInstanceStrong();
                log.info("SecureRandom: NativePRNGBlocking unavailable, using strong '{}'", strong.getAlgorithm());
                return strong;
            } catch (NoSuchAlgorithmException _) {
                log.warn("SecureRandom: no strong algorithm available, using default SecureRandom");
                return new SecureRandom();
            }
        }
    }
}
