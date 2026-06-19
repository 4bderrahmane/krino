package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class InvalidRefreshTokenException extends BaseException
{
    public InvalidRefreshTokenException(String message)
    {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
