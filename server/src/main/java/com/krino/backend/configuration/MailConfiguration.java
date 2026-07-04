package com.krino.backend.configuration;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfiguration {

    @Bean
    Resend resend(@Value("${resend.api-key}") String apiKey) {
        return new Resend(apiKey);
    }
}
