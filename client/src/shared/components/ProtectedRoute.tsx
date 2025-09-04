import React from 'react';
import {Navigate} from 'react-router-dom';
import LoadingSpinner from './LoadingSpinner';
import {useAuth} from '../hooks/useAuth';

interface ProtectedRouteProps {
    children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({children}) => {
    const {user, isLoading} = useAuth();

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    return user ? <>{children}</> : <Navigate to="/login" replace/>;
};

export default ProtectedRoute;
