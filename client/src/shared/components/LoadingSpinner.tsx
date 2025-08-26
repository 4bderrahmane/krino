import React from 'react';
import '../styles/LoadingSpinner.css';

const LoadingSpinner: React.FC = () => {
    return (
        <div className="loading-spinner">
            <div className="spinner"></div>
        </div>
    );
};


export default LoadingSpinner;

// export default function LoadingSpinner() {
//     return (
//         <div className="spinner-wrapper">
//             <div className="spinner"></div>
//             <p className="loading-text">Loading, please wait...</p>
//         </div>
//     );
// }