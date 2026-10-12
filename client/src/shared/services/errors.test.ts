import {describe, expect, it} from 'vitest';
import {AxiosError, AxiosHeaders} from 'axios';
import i18n from '@/utils/i18n';
import {
    getServerErrorCode,
    resolveServerError,
    resolveServerErrorCode,
    type ServerErrorCode,
} from '@/shared/services/errors.ts';

const t = i18n.getFixedT('en');

/** An axios error carrying the RFC 9457 body the backend actually returns. */
const problemDetail = (errorCode: string, status = 400): AxiosError => {
    const error = new AxiosError('Request failed', 'ERR_BAD_REQUEST');
    error.response = {
        status,
        statusText: '',
        headers: new AxiosHeaders(),
        config: {headers: new AxiosHeaders()},
        data: {errorCode, detail: 'server detail text'},
    };
    return error;
};

describe('getServerErrorCode', () => {
    it('reads the code off a ProblemDetail response', () => {
        expect(getServerErrorCode(problemDetail('EMAIL_NOT_VERIFIED'))).toBe('EMAIL_NOT_VERIFIED');
    });

    it('reads the code off an error object that carries one directly', () => {
        expect(getServerErrorCode({errorCode: 'RATE_LIMITED'})).toBe('RATE_LIMITED');
    });

    it('returns null when there is no code to read', () => {
        expect(getServerErrorCode(new Error('network down'))).toBeNull();
        expect(getServerErrorCode(undefined)).toBeNull();
        expect(getServerErrorCode({})).toBeNull();
    });
});

describe('resolveServerErrorCode', () => {
    it('resolves a known code to its message', () => {
        expect(resolveServerErrorCode(t, 'ACCOUNT_NOT_APPROVED')).toBe(t('codes.ACCOUNT_NOT_APPROVED', {ns: 'errors'}));
    });

    it('falls back to the generic message for an unknown code', () => {
        expect(resolveServerErrorCode(t, 'NOT_A_REAL_CODE' as ServerErrorCode))
            .toBe(t('default', {ns: 'errors'}));
    });

    it('falls back to the generic message when there is no code', () => {
        expect(resolveServerErrorCode(t, null)).toBe(t('default', {ns: 'errors'}));
    });

    it('prefers a context-specific message over the generic one', () => {
        const generic = resolveServerErrorCode(t, 'INVALID_CREDENTIALS');
        const inLogin = resolveServerErrorCode(t, 'INVALID_CREDENTIALS', {context: 'login'});

        expect(inLogin).not.toBe(t('default', {ns: 'errors'}));
        // Falls back to the generic message when this context has nothing more specific.
        expect(resolveServerErrorCode(t, 'INVALID_CREDENTIALS', {context: 'nosuchcontext'})).toBe(generic);
    });

    it('never leaks a raw code to the user', () => {
        for (const code of Object.keys(ALL_CODES) as ServerErrorCode[]) {
            expect(resolveServerErrorCode(t, code)).not.toContain(code);
        }
    });
});

describe('resolveServerError', () => {
    it('translates a thrown ProblemDetail rather than showing the server text', () => {
        const message = resolveServerError(t, problemDetail('PAYLOAD_TOO_LARGE', 413));

        expect(message).toBe(t('codes.PAYLOAD_TOO_LARGE', {ns: 'errors'}));
        expect(message).not.toContain('server detail text');
    });

    it('shows the generic message for a network failure with no response', () => {
        expect(resolveServerError(t, new Error('Network Error'))).toBe(t('default', {ns: 'errors'}));
    });
});

/**
 * Every code the backend can return, as a Record so the compiler rejects this
 * table the moment ServerErrorCode gains a member that is not listed here.
 *
 * This pairs with the test below: the union has to stay in step with the backend's
 * ErrorCode enum by hand, and a code with no translation silently degrades to
 * "Something went wrong". That is exactly how PASSWORD_CHANGE_REQUIRED shipped
 * without a message.
 */
const ALL_CODES: Record<ServerErrorCode, true> = {
    INTERNAL_SERVER_ERROR: true,
    VALIDATION_ERROR: true,
    RESOURCE_NOT_FOUND: true,
    UNAUTHORIZED: true,
    INVALID_CREDENTIALS: true,
    TOKEN_EXPIRED: true,
    INVALID_TOKEN: true,
    ACCESS_DENIED: true,
    ACCOUNT_LOCKED: true,
    ACCOUNT_NOT_APPROVED: true,
    EMAIL_NOT_VERIFIED: true,
    PASSWORD_CHANGE_REQUIRED: true,
    DATA_CONFLICT: true,
    STALE_PROFILE: true,
    OPERATION_NOT_ALLOWED: true,
    METHOD_NOT_ALLOWED: true,
    NOT_ACCEPTABLE: true,
    UNSUPPORTED_MEDIA_TYPE: true,
    PAYLOAD_TOO_LARGE: true,
    RATE_LIMITED: true,
    EXTERNAL_SERVICE_FAILURE: true,
    TIMEOUT_OCCURRED: true,
};

describe('error code coverage', () => {
    const languages = ['en', 'fr'] as const;
    const codes = Object.keys(ALL_CODES) as ServerErrorCode[];

    it.each(languages)('has a %s message for every code the backend can return', (language) => {
        const translate = i18n.getFixedT(language);
        const fallback = translate('default', {ns: 'errors'});

        const untranslated = codes.filter((code) => resolveServerErrorCode(translate, code) === fallback);

        expect(untranslated).toEqual([]);
    });
});
