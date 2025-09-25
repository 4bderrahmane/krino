package com.jesa.interviewslotmanager.exception;

public class DepartmentNotFoundException extends RuntimeException
{
    public DepartmentNotFoundException(String message)
    {
        super(message);
    }

    public DepartmentNotFoundException(String field, Object value)
    {
        super(String.format("Department with %s '%s' not found.", field.toLowerCase(), value));
    }
}
