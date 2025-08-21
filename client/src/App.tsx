import React, {useState} from "react";
import {useTranslation} from 'react-i18next';
import {Routes, Route, Navigate, useNavigate} from 'react-router-dom';
import type {UserLoginDTO} from "./features/authentication/types/api.types.ts";
import LoginComponent from "./features/authentication/components/LoginForm.tsx";
import "./features/authentication/styles/LoginForm.css";

const Dashboard: React.FC<{user: UserLoginDTO, onLogout: () => void}> = ({user, onLogout}) => {
    const {t} = useTranslation();

    return (
        <div className="login-page-container">
            <div className="login-card">
                <h2>{t('auth.welcome')}</h2>
                <p style={{textAlign: 'center', marginBottom: '1rem', color: '#6b7280'}}>
                    {t('auth.loggedInAs')} <strong style={{color: '#1e3a8a'}}>{user.email}</strong>
                </p>
                <button
                    onClick={onLogout}
                    className="login-button"
                >
                    {t('auth.logout')}
                </button>
            </div>
        </div>
    );
};

const LoginPage: React.FC<{onLoginSuccess: (credentials: UserLoginDTO) => void}> = ({onLoginSuccess}) => {
    return (
        <div className="login-page-container">
            <LoginComponent onLoginSuccess={onLoginSuccess}/>
        </div>
    );
};

const App: React.FC = () => {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [user, setUser] = useState<UserLoginDTO | null>(null);

    const handleLogin = (credentials: UserLoginDTO) => {
        console.log(t('auth.loginSuccess'), credentials);
        setUser(credentials);
        setIsLoggedIn(true);
        navigate('/dashboard');
    };

    const handleLogout = () => {
        setUser(null);
        setIsLoggedIn(false);
        navigate('/login');
    };

    return (
        <Routes>
            <Route
                path="/login"
                element={
                    isLoggedIn ?
                    <Navigate to="/dashboard" replace /> :
                    <LoginPage onLoginSuccess={handleLogin} />
                }
            />
            <Route
                path="/dashboard"
                element={
                    isLoggedIn && user ?
                    <Dashboard user={user} onLogout={handleLogout} /> :
                    <Navigate to="/login" replace />
                }
            />
            <Route
                path="/"
                element={<Navigate to="/login" replace />}
            />
            <Route
                path="*"
                element={<Navigate to="/login" replace />}
            />
        </Routes>
    );
};

export default App;