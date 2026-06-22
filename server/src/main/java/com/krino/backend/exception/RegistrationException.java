package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class RegistrationException extends BaseException {
    public RegistrationException(String message) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message, cause);
    }
}
