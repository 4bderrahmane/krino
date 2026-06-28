// Shared CV/resume file constraints, mirroring the backend (CvStorageService):
// PDF only, 5MB max. Used by registration (base CV) and the apply flow.
export const CV_MAX_BYTES = 5 * 1024 * 1024;
export const CV_MIME = 'application/pdf';

export type CvFileError = 'required' | 'type' | 'size';

/** Returns an error code for a selected CV file, or null when it is valid. */
export const validateCvFile = (file: File | null): CvFileError | null => {
    if (!file) return 'required';
    if (file.type !== CV_MIME) return 'type';
    if (file.size > CV_MAX_BYTES) return 'size';
    return null;
};
