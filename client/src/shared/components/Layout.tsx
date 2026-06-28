import React from 'react';
import {Outlet} from 'react-router-dom';
import NavBar from './NavBar';
import PasswordReminderModal from './PasswordReminderModal';
import '@/shared/styles/Layout.css';

const Layout: React.FC = () => {
    return (
        <div className="app-layout">
            <NavBar/>
            <main className="main-content">
                <Outlet/>
            </main>
            <PasswordReminderModal/>
        </div>
    );
};

export default Layout;