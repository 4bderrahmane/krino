import React, {useState, createContext, useCallback} from 'react';
import SuccessToast from '../components/SuccessToast';
import type {ReactNode} from 'react';

interface ToastContextType {
    showToast: (message: string, duration?: number) => void;
}

interface ToastProviderProps {
    children: ReactNode;
}

export const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const ToastProvider: React.FC<ToastProviderProps> = ({children}) => {
    const [toastMessage, setToastMessage] = useState<string | null>(null);
    const [isVisible, setIsVisible] = useState(false);

    const showToast = useCallback((message: string, duration: number = 3000) => {
        setToastMessage(message);
        setIsVisible(true);

        setTimeout(() => {
            setIsVisible(false);
            setTimeout(() => setToastMessage(null), 300);
        }, duration);
    }, []);

    const closeToast = useCallback(() => {
        setIsVisible(false);
        setTimeout(() => setToastMessage(null), 300);
    }, []);

    return (
        <ToastContext.Provider value={{showToast}}>
            {children}
            {toastMessage && (
                <SuccessToast
                    message={toastMessage}
                    isVisible={isVisible}
                    onClose={closeToast}
                />
            )}
        </ToastContext.Provider>
    );
};
