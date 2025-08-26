import {createBrowserRouter, Navigate} from 'react-router-dom';
import LoginForm from '../features/authentication/components/LoginForm';
import RegistrationForm from '../features/authentication/components/RegistrationForm';
import DashboardPage from '../shared/components/DashboardPage';
import NotFoundPage from '../shared/components/NotFoundPage';
import {useAuth} from '../shared/contexts/AuthContext';

// eslint-disable-next-line react-refresh/only-export-components
const RootRedirect = () => {
    const {isAuthenticated} = useAuth();
    return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />;
};

// eslint-disable-next-line react-refresh/only-export-components
const ProtectedDashboard = () => {
    const {isAuthenticated} = useAuth();
    return isAuthenticated ? <DashboardPage /> : <Navigate to="/login" replace />;
};

const router = createBrowserRouter([
    {
        path: '/',
        element: <RootRedirect/>
    },
    {
        path: '/login',
        element: <LoginForm/>
    },
    {
        path: '/register',
        element: <RegistrationForm/>
    },
    {
        path: '/dashboard',
        element: <ProtectedDashboard/>
    },
    {
        path: '*',
        element: <NotFoundPage/>
    }
])

export default router;

// children: [
//     {
//         index: true,
//         element: <Home/>
//     },
//     {
//         path: 'about',
//         element: <About/>
//     }
// ]
