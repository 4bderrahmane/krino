import React from 'react';
import {Navigate, Outlet, useLocation} from 'react-router-dom';
import LoadingSpinner from './LoadingSpinner';
import {useAuth} from '@/shared/hooks/useAuth';

/**
 * Guard layer for the routes that need a session.
 *
 * Used as a pathless layout route inside the main layout, so the public pages (the
 * job catalogue) and the private ones share one NavBar and one shell, while only the
 * private ones sit behind this check. Where the visitor was headed is handed to the
 * login screen so they land back there instead of on the dashboard.
 */
const ProtectedRoute: React.FC = () => {
    const {user, isLoading} = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (user) {
        return <Outlet/>;
    }

    const target = `${location.pathname}${location.search}`;
    return <Navigate to={`/login?redirect=${encodeURIComponent(target)}`} replace/>;
};

export default ProtectedRoute;
