package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class FileStorageException extends BaseException {

    public FileStorageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public FileStorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
