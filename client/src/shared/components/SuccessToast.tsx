import React, { useEffect, useState } from 'react';
// import { useTranslation } from 'react-i18next';
import '../styles/SuccessToast.css';
import type {SuccessToastProps} from "../types/types.ts";


const SuccessToast: React.FC<SuccessToastProps> = ({
    message,
    isVisible,
    onClose,
    duration = 3000
}) => {
    // const { t } = useTranslation();
    const [show, setShow] = useState(false);

    useEffect(() => {
        if (isVisible) {
            setShow(true);
            const timer = setTimeout(() => {
                setShow(false);
                setTimeout(onClose, 300);
            }, duration);

            return () => clearTimeout(timer);
        }
    }, [isVisible, duration, onClose]);

    if (!isVisible && !show) return null;

    return (
        <div className={`success-toast-overlay ${show ? 'show' : 'hide'}`}>
            <div className="success-toast-container">
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
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                        <polyline points="22,4 12,14.01 9,11.01"></polyline>
                    </svg>
                </div>
                <div className="success-message">
                    {message}
                </div>
            </div>
        </div>
    );
};

export default SuccessToast;
