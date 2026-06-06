package com.jesa.interviewslotmanager.exception;

public class InvalidJobTypeException extends RuntimeException
{
    public InvalidJobTypeException(String message)
    {
        super(message);
    }

    public InvalidJobTypeException(String field, Object value)
    {
        super(String.format("Job not found with %s: '%s'", field, value));
    }
}