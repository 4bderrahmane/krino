package com.krino.backend.service.email;

import com.krino.backend.configuration.properties.MailProperties;
import com.krino.backend.exception.EmailDeliveryException;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.thymeleaf.context.Context;

@Service
@ConditionalOnProperty(name = "app.mail.log-only", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService implements EmailService {
    private final Resend resend;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties props;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void sendPasswordReset(String to, String resetLink) {
        send(to, "Reset your password", "mail/password-reset", Map.of("resetLink", resetLink));
    }

    @Override
    public void sendInitialPassword(String to, String firstName, String password) {
        send(to, "Your Krino account is ready", "mail/account-created",
                Map.of("firstName", firstName, "email", to, "password", password,
                        "loginUrl", frontendUrl + "/login"));
    }

    @Override
    public void sendEmailVerification(String to, String firstName, String verificationLink) {
        send(to, "Verify your Krino email address", "mail/verify-email",
                Map.of("firstName", firstName, "verificationLink", verificationLink));
    }

    private void send(String to, String subject, String template, Map<String, Object> model) {
        var ctx = new Context();
        ctx.setVariables(model);
        String html = templateEngine.process(template, ctx);

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(props.fromName() + " <" + props.from() + ">")
                .to(to)
                .subject(subject)
                .html(html)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(options);
            log.info("email sent template={} resendId={}", template, response.getId());
        } catch (ResendException e) {
            throw new EmailDeliveryException("send failed: " + template, e);
        }
    }


}
