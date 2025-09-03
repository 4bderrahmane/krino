import React from 'react';
import {Navigate} from 'react-router-dom';
import {useAuth} from '../hooks/useAuth';

const RootRedirect: React.FC = () => {
    const {isAuthenticated} = useAuth();

    return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace/>;
};

export default RootRedirect;