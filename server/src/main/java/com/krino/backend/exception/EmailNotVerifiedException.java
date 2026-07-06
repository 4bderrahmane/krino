package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

/**
 * Raised when a user with correct credentials tries to log in before verifying their email
 * address. Distinct from {@link AccountNotApprovedException} so the client can offer a
 * "resend verification email" action instead of a dead end.
 */
public class EmailNotVerifiedException extends BaseException {
    public EmailNotVerifiedException(String message) {
        super(ErrorCode.EMAIL_NOT_VERIFIED, message);
    }
}
