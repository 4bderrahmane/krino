import { useContext } from 'react';
import { ToastContext } from '../contexts/toastContext';

export const useSuccessToast = () => {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error('useSuccessToast must be used within a ToastProvider');
    }

    return { showSuccessToast: context.showToast };
};
