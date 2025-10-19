package com.jesa.interviewslotmanager.exception;

import com.jesa.interviewslotmanager.utility.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
@Getter
public class ResourceNotFoundException extends RuntimeException
{
    private final ErrorCode errorCode;

    public ResourceNotFoundException(String message)
    {
        super(message);
        this.errorCode = ErrorCode.RESOURCE_NOT_FOUND;
    }

    public ResourceNotFoundException(String message, ErrorCode errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public ResourceNotFoundException(String objectName, String field, Object value)
    {
        super(String.format("%s not found with %s: '%s'", objectName, field, value));
        this.errorCode = ErrorCode.RESOURCE_NOT_FOUND;
    }

}
