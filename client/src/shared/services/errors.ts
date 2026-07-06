import axios from 'axios';
import type {TFunction} from 'i18next';

// The full set of machine-readable codes the backend can return on an RFC 9457
// ProblemDetail (mirrors the server's ErrorCode enum). This is the single source
// of truth for translating server errors; keep it in lockstep with the backend.
export type ServerErrorCode =
    | 'INTERNAL_SERVER_ERROR'
    | 'VALIDATION_ERROR'
    | 'RESOURCE_NOT_FOUND'
    | 'UNAUTHORIZED'
    | 'INVALID_CREDENTIALS'
    | 'TOKEN_EXPIRED'
    | 'INVALID_TOKEN'
    | 'ACCESS_DENIED'
    | 'ACCOUNT_LOCKED'
    | 'ACCOUNT_NOT_APPROVED'
    | 'EMAIL_NOT_VERIFIED'
    | 'DATA_CONFLICT'
    | 'OPERATION_NOT_ALLOWED'
    | 'METHOD_NOT_ALLOWED'
    | 'NOT_ACCEPTABLE'
    | 'UNSUPPORTED_MEDIA_TYPE'
    | 'PAYLOAD_TOO_LARGE'
    | 'RATE_LIMITED'
    | 'EXTERNAL_SERVICE_FAILURE'
    | 'TIMEOUT_OCCURRED';

const ERRORS_NS = 'errors';

// Reads the stable errorCode the backend puts on every ProblemDetail response.
// Also handles thrown errors that carry the code directly (our EnhancedError
// wrapper). Returns null when no code is present (network blip, non-API error),
// so callers fall back to the generic message.
export const getServerErrorCode = (error: unknown): ServerErrorCode | null => {
    if (axios.isAxiosError(error) && error.response?.data) {
        const code = (error.response.data as {errorCode?: string}).errorCode;
        if (code) return code as ServerErrorCode;
    }

    if (error && typeof error === 'object' && 'errorCode' in error) {
        const code = (error as {errorCode?: string}).errorCode;
        if (code) return code as ServerErrorCode;
    }

    return null;
};

export interface ResolveServerErrorOptions {
    // Flow-specific bucket (e.g. 'login', 'register', 'changePassword') used to
    // pick a more specific message for a coarse code before the generic one.
    context?: string;
}

// Resolves a known code to a localized message: context override -> generic
// code message -> default. Never surfaces a raw code to the user.
export const resolveServerErrorCode = (
    t: TFunction,
    code: ServerErrorCode | null | undefined,
    options: ResolveServerErrorOptions = {},
): string => {
    const {context} = options;

    if (code && context) {
        const contextual = t(`context.${context}.${code}`, {ns: ERRORS_NS, defaultValue: ''});
        if (contextual) return contextual;
    }

    if (code) {
        const generic = t(`codes.${code}`, {ns: ERRORS_NS, defaultValue: ''});
        if (generic) return generic;
    }

    return t('default', {ns: ERRORS_NS});
};

// Convenience wrapper: pull the code off a thrown error and resolve it.
export const resolveServerError = (
    t: TFunction,
    error: unknown,
    options: ResolveServerErrorOptions = {},
): string => resolveServerErrorCode(t, getServerErrorCode(error), options);
