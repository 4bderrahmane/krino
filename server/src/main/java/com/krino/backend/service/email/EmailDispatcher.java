package com.krino.backend.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * The entry point application services call to send email. Rather than invoking the transport
 * directly — which would fire mid-transaction and, on a later rollback, leave a delivered email
 * pointing at data that was never committed — it publishes an {@link EmailRequestedEvent}.
 * {@link EmailEventListener} performs the real send after commit and off the request thread.
 */
@Service
@RequiredArgsConstructor
public class EmailDispatcher {

    private final ApplicationEventPublisher publisher;

    public void sendPasswordReset(String to, String resetLink) {
        publish("password-reset", to, svc -> svc.sendPasswordReset(to, resetLink));
    }

    public void sendInitialPassword(String to, String firstName, String password) {
        publish("account-created", to, svc -> svc.sendInitialPassword(to, firstName, password));
    }

    public void sendEmailVerification(String to, String firstName, String verificationLink) {
        publish("verify-email", to, svc -> svc.sendEmailVerification(to, firstName, verificationLink));
    }

    private void publish(String template, String to, Consumer<EmailService> action) {
        publisher.publishEvent(new EmailRequestedEvent(action, template, to));
    }
}
