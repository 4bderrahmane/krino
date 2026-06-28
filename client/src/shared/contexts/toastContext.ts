import {createContext} from 'react';

export type ToastVariant = 'success' | 'error';

interface ToastContextType {
    // variant defaults to 'success' so existing callers keep working unchanged.
    showToast: (message: string, variant?: ToastVariant, duration?: number) => void;
}

export const ToastContext = createContext<ToastContextType | undefined>(undefined);
