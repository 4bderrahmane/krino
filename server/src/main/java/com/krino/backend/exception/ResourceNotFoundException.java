package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
@Getter
public class ResourceNotFoundException extends RuntimeException
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNotFoundException.class);
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
        this.errorCode = mapErrorCodeForObject(objectName);
    }

    private static ErrorCode mapErrorCodeForObject(String objectName)
    {
        if (objectName == null || objectName.isBlank())
        {
            return ErrorCode.RESOURCE_NOT_FOUND;
        }

        String simpleName = objectName.contains(".") ? objectName.substring(objectName.lastIndexOf('.') + 1) : objectName;
        String normalized = simpleName.trim().toUpperCase();

        try
        {
            return ErrorCode.valueOf(normalized + "_NOT_FOUND");
        } catch (IllegalArgumentException ignored)
        {
            LOGGER.debug("No exact ErrorCode for '{}_NOT_FOUND', will try relaxed matching", normalized, ignored);
        }

        for (ErrorCode code : ErrorCode.values())
        {
            if (code.name().contains(normalized))
            {
                return code;
            }
        }

        return ErrorCode.RESOURCE_NOT_FOUND;
    }

}
