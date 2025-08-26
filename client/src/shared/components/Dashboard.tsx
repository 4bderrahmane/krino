import React from "react";
import {useTranslation} from "react-i18next";
import Navbar from "./NavBar.tsx";
import type {User} from "../types/types.ts";

const Dashboard: React.FC<{ user: User, onLogout: () => void }> = ({user, onLogout}) => {
    const {t} = useTranslation();

    const navbarProps = {
        username: user.username,
        onLogout: onLogout,
    };

    return (
        <div className="app-container">
            {user && <Navbar {...navbarProps}/>}
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
        </div>
    );
};

export default Dashboard;