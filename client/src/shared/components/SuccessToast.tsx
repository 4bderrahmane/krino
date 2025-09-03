import React, { useEffect } from 'react';
import '../styles/SuccessToast.css';

interface SuccessToastProps {
    message: string;
    isVisible: boolean;
    onClose: () => void;
    duration?: number;
}

const SuccessToast: React.FC<SuccessToastProps> = ({
    message,
    isVisible,
    onClose,
    duration = 2000
}) => {
    useEffect(() => {
        if (isVisible && duration > 0) {
            const timer = setTimeout(() => {
                onClose();
            }, duration);

            return () => clearTimeout(timer);
        }
    }, [isVisible, duration, onClose]);

    if (!isVisible) {
        return null;
    }

    return (
        <div className="success-toast show">
            <div className="success-toast-content">
                <div className="success-icon">
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <polyline points="20,6 9,17 4,12"></polyline>
                    </svg>
                </div>
                <span className="success-message">{message}</span>
                <button
                    className="close-button"
                    onClick={onClose}
                    aria-label="Close"
                >
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>
        </div>
    );
};

export default SuccessToast;
