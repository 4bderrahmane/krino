import React, {useState, useEffect} from 'react';
import type {ToastState} from "../types/types.ts";

interface SuccessToastProps {
    message: string;
    duration: number;
    onClose: () => void;
}

export const SuccessToastTest: React.FC<SuccessToastProps> = ({message, duration, onClose}) => {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        setVisible(true);
    }, []);

    useEffect(() => {
        const timer = setTimeout(() => {
            setVisible(false);

            setTimeout(onClose, 500);
        }, duration);

        return () => clearTimeout(timer);
    }, [duration, onClose]);

    return (
        <div
            className={`fixed bottom-5 right-5 flex items-center bg-green-500 text-white py-3 px-5 rounded-lg shadow-lg transform transition-all duration-500 ease-in-out ${visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-5'}`}
        >
            <svg className="w-6 h-6 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                 xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span>{message}</span>
        </div>
    );
};




export default function App() {

    const [toast, setToast] = useState<ToastState | null>(null);

    const showSuccessToast = () => {
        setToast({
            key: Date.now(),
            message: 'Action completed successfully!',
            duration: 3000
        });
    };

    return (
        <div className="bg-slate-900 min-h-screen flex flex-col items-center justify-center font-sans text-white p-4">
            <div className="text-center">
                <h1 className="text-4xl font-bold mb-4">Success Toast Example</h1>
                <p className="text-slate-400 mb-8">Click the button to show a success notification.</p>
                <button
                    onClick={showSuccessToast}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 px-6 rounded-lg shadow-md transition-transform transform hover:scale-105 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-opacity-50"
                >
                    Show Success Toast
                </button>
            </div>

            {/* Conditionally render the toast when its state is not null */}
            {toast && (
                <SuccessToastTest
                    key={toast.key}
                    message={toast.message}
                    duration={toast.duration}
                    onClose={() => setToast(null)}
                />
            )}
        </div>
    );
}
