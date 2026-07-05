package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

/**
 * Raised when a supplied password-reset token is unknown, expired or already used. Deliberately
 * carries no hint about which, so a caller can't probe token validity.
 */
public class InvalidPasswordResetTokenException extends BaseException {
    public InvalidPasswordResetTokenException(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }
}
