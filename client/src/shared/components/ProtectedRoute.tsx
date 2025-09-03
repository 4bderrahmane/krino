import React from 'react';
import {Navigate} from 'react-router-dom';
import {useAuth} from "../hooks/useAuth.ts";
import LoadingSpinner from "./LoadingSpinner.tsx";

interface ProtectedRouteProps {
    children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({children}) => {
    const {isAuthenticated, isLoading} = useAuth();

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace/>;
};

export default ProtectedRoute;
