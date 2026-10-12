import React from 'react';
import {Navigate, Outlet, useLocation} from 'react-router-dom';
import {useAuth} from '@/shared/hooks/useAuth';
import NavBar from './NavBar';
import '@/shared/styles/Layout.css';

const Layout: React.FC = () => {
    const {user} = useAuth();
    const {pathname} = useLocation();
    const needsPasswordChange = user?.mustChangePassword && pathname !== '/settings/password';

    return (
        <div className="app-layout">
            <NavBar/>
            <main className="main-content">
                {needsPasswordChange ? <Navigate to="/settings/password" replace/> : <Outlet/>}
            </main>
        </div>
    );
};

export default Layout;