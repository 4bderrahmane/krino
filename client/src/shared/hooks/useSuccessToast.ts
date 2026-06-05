import { useContext } from 'react';
import { ToastContext } from '../contexts/ToastContext.tsx';

export const useSuccessToast = () => {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error('useSuccessToast must be used within a ToastProvider');
    }

    return { showSuccessToast: context.showToast };
};
