import React from 'react';
import {Navigate} from 'react-router-dom';
import {useAuth} from '@/shared/hooks/useAuth';
import LoadingSpinner from './LoadingSpinner';

/**
 * The front door. Signed-in users go to their dashboard; everyone else lands on the
 * published catalogue rather than a login form, since browsing openings needs no
 * account, and asking a visitor to sign in before they see anything is how a careers
 * site loses them.
 *
 * Waits for the session probe to settle, otherwise a returning user is bounced to the
 * catalogue for a frame before their session is restored.
 */
const RootRedirect: React.FC = () => {
    const {user, isLoading} = useAuth();

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    return <Navigate to={user ? '/dashboard' : '/offers'} replace/>;
};

export default RootRedirect;
