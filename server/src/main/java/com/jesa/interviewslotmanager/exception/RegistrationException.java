package com.jesa.interviewslotmanager.exception;

public class RegistrationException extends RuntimeException
{
    public RegistrationException(String message)
    {
        super(message);
    }

    public RegistrationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

