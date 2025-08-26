import {useState, useCallback, useRef, useEffect} from 'react';

interface UseSuccessToastReturn {
    isVisible: boolean;
    message: string;
    showSuccess: (message: string, duration?: number) => void;
    hideSuccess: () => void;
}

const DEFAULT_SUCCESS_DURATION = 3000;

export const useSuccessToast = (): UseSuccessToastReturn => {
    const [isVisible, setIsVisible] = useState(false);
    const [message, setMessage] = useState('');
    const timerRef = useRef<number | null>(null);

    const hideSuccess = useCallback(() => {
        if (timerRef.current) {
            clearTimeout(timerRef.current);
            timerRef.current = null;
        }
        setIsVisible(false);
        setMessage('');
    }, []);

    const showSuccess = useCallback(
        (messageText: string, duration: number = DEFAULT_SUCCESS_DURATION) => {
            setMessage(messageText);
            setIsVisible(true);

            if (timerRef.current) {
                clearTimeout(timerRef.current);
            }
            timerRef.current = window.setTimeout(() => {
                hideSuccess();
            }, duration);
        },
        [hideSuccess]
    );

    useEffect(() => {
        return () => {
            if (timerRef.current) {
                clearTimeout(timerRef.current);
            }
        };
    }, []);

    return {
        isVisible,
        message,
        showSuccess,
        hideSuccess
    };
};
