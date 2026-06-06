package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
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