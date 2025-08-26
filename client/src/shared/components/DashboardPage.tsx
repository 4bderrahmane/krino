import React from 'react';
import {useNavigate} from 'react-router-dom';
import Dashboard from './Dashboard';
import type {User} from '../types/types';
import {useAuth} from '../contexts/AuthContext';

const DashboardPage: React.FC = () => {
    const navigate = useNavigate();
    const {user: authUser, logout: authLogout, isAuthenticated} = useAuth();

    if (!isAuthenticated || !authUser) {
        navigate('/login');
        return null;
    }

    const dashboardUser: User = {
        username: authUser.username,
        email: authUser.email,
        firstName: authUser.firstName,
        lastName: authUser.lastName,
        phoneNumber: parseInt(authUser.phoneNumber) || 0,
        roles: new Set(authUser.roles),
    };

    const handleLogout = () => {
        authLogout();
        navigate('/login');
    };

    return <Dashboard user={dashboardUser} onLogout={handleLogout}/>;
};

export default DashboardPage;
