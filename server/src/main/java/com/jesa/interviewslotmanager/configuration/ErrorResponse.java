package com.jesa.interviewslotmanager.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jesa.interviewslotmanager.utility.ErrorCode;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String path,
        ErrorCode errorCode,
        Map<String, Object> details
)
{

}