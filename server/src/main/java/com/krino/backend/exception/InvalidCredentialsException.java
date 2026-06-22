package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}
