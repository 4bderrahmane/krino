import {createContext} from 'react';

interface ToastContextType {
    showToast: (message: string, duration?: number) => void;
}

export const ToastContext = createContext<ToastContextType | undefined>(undefined);
