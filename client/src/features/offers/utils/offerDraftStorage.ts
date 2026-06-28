// Tiny, type-agnostic localStorage helpers for persisting an in-progress form.
// Generic on purpose: the storage layer knows nothing about the offer form's
// shape, so it never has to change when the form's fields do. All access is
// guarded — localStorage can throw (private mode, quota, disabled storage), and
// a draft is a nice-to-have, never worth crashing the page over.

export const loadDraft = <T>(key: string | null): T | null => {
    if (!key) return null;
    try {
        const raw = window.localStorage.getItem(key);
        return raw ? (JSON.parse(raw) as T) : null;
    } catch {
        return null;
    }
};

export const saveDraft = <T>(key: string | null, value: T): void => {
    if (!key) return;
    try {
        window.localStorage.setItem(key, JSON.stringify(value));
    } catch {
        /* storage unavailable or full — drop the draft silently */
    }
};

export const clearDraft = (key: string | null): void => {
    if (!key) return;
    try {
        window.localStorage.removeItem(key);
    } catch {
        /* nothing to do */
    }
};
