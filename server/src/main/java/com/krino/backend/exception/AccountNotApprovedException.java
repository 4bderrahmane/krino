package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

public class AccountNotApprovedException extends BaseException {
    public AccountNotApprovedException(String message) {
        super(ErrorCode.ACCOUNT_NOT_APPROVED, message);
    }
}
