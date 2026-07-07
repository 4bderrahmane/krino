package com.krino.backend.service.email;

public interface EmailService {
    void sendPasswordReset(String to, String resetLink);

    void sendInitialPassword(String to, String firstName, String password);

    void sendEmailVerification(String to, String firstName, String verificationLink);
}
