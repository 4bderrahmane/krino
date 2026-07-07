package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import lombok.Getter;

import java.util.Set;

/**
 * Raised when a paged request asks to sort by a property that is not on the endpoint's
 * {@code SortWhitelist}. It carries the rejected field and the allowed set so the 400
 * can tell the client exactly what it may sort by. Distinct from Spring Data's
 * {@code PropertyReferenceException}, which means "no such property exists" — here the
 * property may well exist, it is simply not permitted. Maps to a 400 through
 * {@link BaseException}'s {@link ErrorCode#VALIDATION_ERROR}.
 */
@Getter
public class InvalidSortFieldException extends BaseException {

    private final String field;
    private final transient Set<String> allowedFields;

    public InvalidSortFieldException(String field, Set<String> allowedFields) {
        // The allowed set is kept on the exception (getAllowedFields) for logging, but is
        // deliberately not spelled out to the client so the error stays terse.
        super(ErrorCode.VALIDATION_ERROR, "Cannot sort by '" + field + "'.");
        this.field = field;
        this.allowedFields = Set.copyOf(allowedFields);
    }
}
