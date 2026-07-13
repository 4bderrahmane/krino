package com.krino.backend.service.email;

import com.krino.backend.exception.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;

/**
 * Delivers the email described by an {@link EmailRequestedEvent}, but only once the originating
 * transaction has committed ({@code AFTER_COMMIT}) and off the request thread ({@code @Async}).
 * Transport failures are retried a few times; if every attempt fails, the message is dropped
 * with an error log (never silently), so the delivery gap is at least visible to operations.
 *
 * <p>{@code fallbackExecution = true} means a request published without an active transaction is
 * still delivered (immediately) rather than being silently discarded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private static final int MAX_ATTEMPTS = 3;
    public static final Duration RETRY_BACKOFF_MILLIS = Duration.ofMillis(2000);

    private final EmailService emailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmailRequested(EmailRequestedEvent event) {
        for (int attempt = 1; true; attempt++) {
            try {
                event.action().accept(emailService);
                return;
            } catch (EmailDeliveryException ex) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("email delivery failed permanently after {} attempts template={} to={}",
                            MAX_ATTEMPTS, event.template(), event.recipient(), ex);
                    return;
                }
                log.warn("email delivery attempt {}/{} failed template={} to={}; retrying in {}ms",
                        attempt, MAX_ATTEMPTS, event.template(), event.recipient(), RETRY_BACKOFF_MILLIS, ex);

                if (!sleepBeforeRetry()) return;
            } catch (RuntimeException ex) {
                // Non-transport failure and retrying won't help, so we drop
                // it with context rather than letting it vanish on the async thread.
                log.error("email delivery failed (non-retryable) template={} to={}", event.template(), event.recipient(), ex);
                return;
            }
        }
    }

    private boolean sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS);
            return true;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
