package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceNotFoundExceptionTest
{

    @Test
    void departmentSimpleNameMapsToDepartmentNotFound()
    {
        ResourceNotFoundException ex = new ResourceNotFoundException("Department", "id", 1L);
        assertEquals(ErrorCode.DEPARTMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void departmentFullyQualifiedNameMapsToDepartmentNotFound()
    {
        ResourceNotFoundException ex = new ResourceNotFoundException("com.jesa.interviewslotmanager.entity.Department", "id", 1L);
        assertEquals(ErrorCode.DEPARTMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void userSimpleNameMapsToUserNotFound()
    {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", 1L);
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void jobSimpleNameMapsToJobNotFound()
    {
        ResourceNotFoundException ex = new ResourceNotFoundException("Job", "id", 1L);
        assertEquals(ErrorCode.JOB_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void unknownNameFallsBackToResourceNotFound()
    {
        ResourceNotFoundException ex = new ResourceNotFoundException("Application", "id", 1L);
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }
}

