package com.jesa.interviewslotmanager.exception;

public class InvalidRefreshTokenException extends RuntimeException
{
    public InvalidRefreshTokenException(String message)
    {
        super(message);
    }
}
