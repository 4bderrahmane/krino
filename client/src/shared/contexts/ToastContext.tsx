import React, {useState, useCallback, useRef} from 'react';
import Toast from '@/shared/components/Toast';
import type {ReactNode} from 'react';
import {ToastContext, type ToastVariant} from './toastContext';

interface ToastProviderProps {
    children: ReactNode;
}

export const ToastProvider: React.FC<ToastProviderProps> = ({children}) => {
    const [toastMessage, setToastMessage] = useState<string | null>(null);
    const [variant, setVariant] = useState<ToastVariant>('success');
    const [isVisible, setIsVisible] = useState(false);
    const hideTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

    const showToast = useCallback((message: string, toastVariant: ToastVariant = 'success', duration = 3000) => {
        if (hideTimer.current) clearTimeout(hideTimer.current);
        setToastMessage(message);
        setVariant(toastVariant);
        setIsVisible(true);

        hideTimer.current = setTimeout(() => {
            setIsVisible(false);
            setTimeout(() => setToastMessage(null), 300);
        }, duration);
    }, []);

    const closeToast = useCallback(() => {
        if (hideTimer.current) clearTimeout(hideTimer.current);
        setIsVisible(false);
        setTimeout(() => setToastMessage(null), 300);
    }, []);

    return (
        <ToastContext.Provider value={{showToast}}>
            {children}
            {toastMessage && (
                <Toast
                    message={toastMessage}
                    variant={variant}
                    isVisible={isVisible}
                    onClose={closeToast}
                />
            )}
        </ToastContext.Provider>
    );
};
