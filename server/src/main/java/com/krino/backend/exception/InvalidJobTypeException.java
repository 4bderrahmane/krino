package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class InvalidJobTypeException extends BaseException {
    public InvalidJobTypeException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public InvalidJobTypeException(String field, Object value) {
        super(ErrorCode.VALIDATION_ERROR, String.format("Invalid job type with %s: '%s'", field, value));
    }
}
