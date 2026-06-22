package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class IncorrectPasswordException extends BaseException {
    public IncorrectPasswordException() {
        super(ErrorCode.INVALID_CREDENTIALS, "The password provided is incorrect.");
    }

    public IncorrectPasswordException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}
