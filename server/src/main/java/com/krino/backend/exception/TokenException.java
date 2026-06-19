package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class TokenException extends BaseException
{
    public TokenException(String message)
    {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public TokenException(String message, Throwable cause)
    {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}
