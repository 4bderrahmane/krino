import React from 'react';
import {Navigate} from 'react-router-dom';
import {useAuth} from '../hooks/useAuth';

const RootRedirect: React.FC = () => {
    const {user} = useAuth();

    return <Navigate to={user ? '/dashboard' : '/login'} replace/>;
};

export default RootRedirect;