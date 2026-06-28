import React from 'react';
import type {ToastVariant} from '@/shared/contexts/toastContext';
import '@/shared/styles/Toast.css';

interface ToastProps {
    message: string | null;
    variant: ToastVariant;
    isVisible: boolean;
    onClose: () => void;
}

// Check-circle for success, alert-circle for error.
const ICON_PATHS: Record<ToastVariant, string> = {
    success: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
    error: 'M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
};

const Toast: React.FC<ToastProps> = ({message, variant, isVisible, onClose}) => {
    if (!message) return null;

    return (
        <div className={`toast toast--${variant} ${isVisible ? 'visible' : 'hidden'}`} role="status" aria-live="polite">
            <svg
                className="toast-icon"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                xmlns="http://www.w3.org/2000/svg"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d={ICON_PATHS[variant]}
                ></path>
            </svg>
            <span>{message}</span>
            <button onClick={onClose} className="toast-close" aria-label="Close">
                <svg xmlns="http://www.w3.org/2000/svg" className="toast-close-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd"
                          d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                          clipRule="evenodd"/>
                </svg>
            </button>
        </div>
    );
};

export default Toast;
