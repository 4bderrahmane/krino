package com.krino.backend.exception;

public class InvalidRefreshTokenException extends RuntimeException
{
    public InvalidRefreshTokenException(String message)
    {
        super(message);
    }
}
