package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class EmailDeliveryException extends BaseException {

    public EmailDeliveryException(String message) {
        super(ErrorCode.EXTERNAL_SERVICE_FAILURE, message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_FAILURE, message, cause);
    }
}
