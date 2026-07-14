package com.krino.backend.configuration;

import com.krino.backend.configuration.properties.MailProperties;
import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfiguration {

    // Only needed by ResendEmailService, so it follows the same toggle — a dev running with
    // log-only mail doesn't need a Resend API key at all.
    @Bean
    @ConditionalOnProperty(name = "app.mail.log-only", havingValue = "false", matchIfMissing = true)
    Resend resend(@Value("${resend.api-key}") String apiKey) {
        return new Resend(apiKey);
    }
}
