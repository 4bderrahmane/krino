package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResourceNotFoundExceptionTest
{

    @Test
    void resourceFieldValueConstructorAlwaysMapsToResourceNotFound()
    {
        ResourceNotFoundException ex = new ResourceNotFoundException("Department", "id", 1L);

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertEquals("Department", ex.getResource());
        assertEquals("id", ex.getField());
        assertEquals(1L, ex.getValue());
    }

    @Test
    void resourceFieldValueConstructorBuildsReadableMessage()
    {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "publicId", "abc");

        assertEquals("User not found with publicId: 'abc'", ex.getMessage());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void messageOnlyConstructorMapsToResourceNotFoundWithoutDetails()
    {
        ResourceNotFoundException ex = new ResourceNotFoundException("Job not found");

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertEquals("Job not found", ex.getMessage());
        assertNull(ex.getResource());
        assertNull(ex.getField());
        assertNull(ex.getValue());
    }
}
