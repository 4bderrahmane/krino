package com.jesa.interviewslotmanager.exception;

public class SlotNotFoundException extends RuntimeException
{
    public SlotNotFoundException(String message)
    {
        super(message);
    }

    public SlotNotFoundException(String field, Object value)
    {
        super(String.format("Slot with %s '%s' not found.", field.toLowerCase(), value));
    }
}