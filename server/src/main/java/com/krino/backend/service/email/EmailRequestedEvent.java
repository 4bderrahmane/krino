package com.krino.backend.service.email;

import java.util.function.Consumer;

public record EmailRequestedEvent(Consumer<EmailService> action, String template, String recipient) {
}
