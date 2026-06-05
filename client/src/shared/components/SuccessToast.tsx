import React from 'react';
import '../styles/SuccessToast.css';

interface SuccessToastProps {
    message: string | null;
    isVisible: boolean;
    onClose: () => void;
    duration?: number;
}

const SuccessToast: React.FC<SuccessToastProps> = ({message, isVisible, onClose}) => {
    if (!message) return null;

    return (
        <div className={`success-notification ${isVisible ? 'visible' : 'hidden'}`}>
            {/* Success Icon SVG */}
            <svg
                className="success-icon"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                xmlns="http://www.w3.org/2000/svg"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                ></path>
            </svg>
            <span>{message}</span>
            {/* Close Button */}
            <button onClick={onClose} className="close-button">
                <svg xmlns="http://www.w3.org/2000/svg" className="close-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd"
                          d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                          clipRule="evenodd"/>
                </svg>
            </button>
        </div>
    );
};


export default SuccessToast;