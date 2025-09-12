package com.jesa.interviewslotmanager.exception;

public class JobNotFoundException extends RuntimeException
{
    public JobNotFoundException(String message)
    {
        super(message);
    }

    public JobNotFoundException(String field, Object value)
    {
        super(String.format("Job not found with %s: '%s'", field, value));
    }
}

