import {describe, expect, it} from 'vitest';
import {CV_MAX_BYTES, CV_MIME, validateCvFile} from '@/shared/utils/cvFile.ts';

const aFile = (bytes: number, type: string = CV_MIME, name = 'cv.pdf'): File =>
    new File([new Uint8Array(bytes)], name, {type});

describe('validateCvFile', () => {
    it('requires a file', () => {
        expect(validateCvFile(null)).toBe('required');
    });

    it('accepts a PDF within the limit', () => {
        expect(validateCvFile(aFile(1024))).toBeNull();
    });

    it('rejects anything that is not a PDF', () => {
        expect(validateCvFile(aFile(1024, 'image/png', 'cv.png'))).toBe('type');
        expect(validateCvFile(aFile(1024, 'application/msword', 'cv.doc'))).toBe('type');
        // A .pdf name with the wrong MIME type is still rejected: the browser's
        // type is what the backend will see.
        expect(validateCvFile(aFile(1024, 'text/plain', 'cv.pdf'))).toBe('type');
    });

    // The backend rejects above 5MB (APP_STORAGE_MAX_CV_SIZE), so the boundary
    // itself has to be allowed here or the client refuses files the server accepts.
    it('accepts a file of exactly the maximum size', () => {
        expect(validateCvFile(aFile(CV_MAX_BYTES))).toBeNull();
    });

    it('rejects a file one byte over the maximum', () => {
        expect(validateCvFile(aFile(CV_MAX_BYTES + 1))).toBe('size');
    });

    it('reports the type problem first when a file is both too big and the wrong type', () => {
        expect(validateCvFile(aFile(CV_MAX_BYTES + 1, 'image/png', 'huge.png'))).toBe('type');
    });
});
