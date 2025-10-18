package com.jesa.interviewslotmanager.exception;

public class UserNotFoundException extends RuntimeException
{
    public UserNotFoundException(String message)
    {
        super(message);
    }

    public UserNotFoundException(String field, Object value)
    {
        super(String.format("User not found with %s: '%s'", field, value));
    }
}