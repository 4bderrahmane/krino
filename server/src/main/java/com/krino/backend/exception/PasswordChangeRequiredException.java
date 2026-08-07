package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;

/**
 * Raised when a session cannot be renewed because the account is still on the temporary
 * password it was created with.
 *
 * <p>Login deliberately still succeeds in that state: the user needs a session to reach the
 * change-password endpoint at all. Renewal is where the line is drawn, so a temporary password
 * buys one access-token lifetime rather than an indefinitely extendable session. Distinct from
 * {@link AccountNotApprovedException} so the client can send the user to the change-password
 * screen instead of showing a dead end.
 */
public class PasswordChangeRequiredException extends BaseException {
    public PasswordChangeRequiredException(String message) {
        super(ErrorCode.PASSWORD_CHANGE_REQUIRED, message);
    }
}
