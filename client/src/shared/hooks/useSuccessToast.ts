import { useState, useCallback, useRef, useEffect } from 'react';

// --- Hook for easy toast usage ---
// This hook provides a simple way to show success toasts using the context system
export const useSuccessToast = () => {
  const [isVisible, setIsVisible] = useState(false);
  const [message, setMessage] = useState('');
  const timerRef = useRef<number | null>(null);

  const hideSuccess = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    setIsVisible(false);
    // Clear message after animation completes
    setTimeout(() => setMessage(''), 300);
  }, []);

  const showSuccess = useCallback(
    (messageText: string, duration: number = 3000) => {
      // Clear any existing timer
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }

      setMessage(messageText);
      setIsVisible(true);

      // Auto-hide after duration
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
    hideSuccess,
  };
};
