import { useContext } from 'react';
import { ToastContext } from '@/shared/contexts/toastContext';

// Errors linger a touch longer than successes so they aren't missed.
const ERROR_TOAST_DURATION = 4500;

export const useSuccessToast = () => {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error('useSuccessToast must be used within a ToastProvider');
    }

    return {
        showSuccessToast: (message: string, duration?: number) =>
            context.showToast(message, 'success', duration),
        showErrorToast: (message: string, duration: number = ERROR_TOAST_DURATION) =>
            context.showToast(message, 'error', duration),
    };
};
