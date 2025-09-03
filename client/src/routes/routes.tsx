import {createBrowserRouter} from 'react-router-dom';
import LoginForm from '../features/authentication/components/LoginForm';
import RegistrationForm from '../features/authentication/components/RegistrationForm';
import Dashboard from '../shared/components/Dashboard';
import NotFoundPage from '../shared/components/NotFoundPage';
import ProtectedRoute from '../shared/components/ProtectedRoute';
import RootRedirect from '../shared/components/RootRedirect';

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
        element: (
            <ProtectedRoute>
                <Dashboard/>
            </ProtectedRoute>
        )
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
