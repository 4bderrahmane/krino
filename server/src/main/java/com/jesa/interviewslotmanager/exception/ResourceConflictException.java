package com.jesa.interviewslotmanager.exception;

import com.jesa.interviewslotmanager.utility.ErrorCode;
import lombok.Getter;

@Getter
public class ResourceConflictException extends RuntimeException
{

    private final ErrorCode errorCode;

    public ResourceConflictException(String message, ErrorCode errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }

}