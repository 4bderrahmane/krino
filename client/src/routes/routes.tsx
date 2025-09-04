import {createBrowserRouter} from 'react-router-dom';
import {Suspense, lazy, type JSX} from "react";

import LoginForm from '../features/authentication/components/LoginForm';
import ProtectedRoute from '../shared/components/ProtectedRoute';
import RootRedirect from '../shared/components/RootRedirect';
import LoadingSpinner from "../shared/components/LoadingSpinner.tsx";

const RegistrationForm = lazy(() => import("../features/authentication/components/RegistrationForm"));
const Dashboard = lazy(() => import("../shared/components/Dashboard"));
const Layout = lazy(() => import("../shared/components/Layout"));
const NotFoundPage = lazy(() => import("../shared/components/NotFoundPage"));
const Profile = lazy(() => import("../features/user-management/components/Profile"));


const withSuspense = (element: JSX.Element) => (
    <Suspense fallback={<LoadingSpinner/>}>{element}</Suspense>
);

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
        element: withSuspense(<RegistrationForm/>),
    },
    {
        path: "/",
        element: (
            <ProtectedRoute>
                {withSuspense(<Layout/>)}
            </ProtectedRoute>
        ),
        children: [
            {
                path: "dashboard",
                element: withSuspense(<Dashboard/>),
            },
            {
                path: "me",
                element: withSuspense(<Profile/>),
            },
            {
                path: '*',
                element: withSuspense(<NotFoundPage/>),
            }
        ],
    },
    {
        path: '*',
        element: withSuspense(<NotFoundPage/>),
    }
])

export default router;