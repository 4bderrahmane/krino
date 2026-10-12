package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class RefreshTokenAuthenticationException extends BaseException {
    public RefreshTokenAuthenticationException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
