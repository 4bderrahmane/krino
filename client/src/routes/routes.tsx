import {createBrowserRouter, Navigate} from 'react-router-dom';
import {Suspense, lazy, type JSX} from "react";

import LoginForm from '@/features/authentication/components/LoginForm';
import ProtectedRoute from '@/shared/components/ProtectedRoute';
import RootRedirect from '@/shared/components/RootRedirect';
import LoadingSpinner from "@/shared/components/LoadingSpinner.tsx";

const RegistrationForm = lazy(() => import("@/features/authentication/components/RegistrationForm"));
const ForgotPasswordForm = lazy(() => import("@/features/authentication/components/ForgotPasswordForm"));
const ResetPasswordForm = lazy(() => import("@/features/authentication/components/ResetPasswordForm"));
const VerifyEmailPage = lazy(() => import("@/features/authentication/components/VerifyEmailPage"));
const Dashboard = lazy(() => import("@/shared/components/Dashboard"));
const Layout = lazy(() => import("@/shared/components/Layout"));
const NotFoundPage = lazy(() => import("@/shared/components/NotFoundPage"));
const Profile = lazy(() => import("@/features/user-management/components/Profile"));
const ProfileSettings = lazy(() => import( "@/features/user-management/components/settings/ProfileSettings.tsx"));
const PasswordSettings = lazy(() => import( "@/features/user-management/components/settings/PasswordSettings.tsx"));
const DeleteAccount = lazy(() => import("@/features/user-management/components/settings/DeleteAccount.tsx"));
const OffersPage = lazy(() => import("@/features/offers/components/OffersPage.tsx"));
const CreateOfferPage = lazy(() => import("@/features/offers/components/CreateOfferPage.tsx"));
const EditOfferPage = lazy(() => import("@/features/offers/components/EditOfferPage.tsx"));
const ApplicationsPage = lazy(() => import("@/features/applications/components/ApplicationsPage.tsx"));
const ApplicationDetailPage = lazy(() => import("@/features/applications/components/ApplicationDetailPage.tsx"));
const OfferDetailPage = lazy(() => import("@/features/offers/components/OfferDetailPage.tsx"));
const InterviewsPage = lazy(() => import("@/features/interviews/components/InterviewsPage.tsx"));
const DepartmentsPage = lazy(() => import("@/features/departments/components/DepartmentsPage.tsx"));
const SlotsPage = lazy(() => import("@/features/slots/components/SlotsPage.tsx"));
const StaffManagementPage = lazy(() => import("@/features/administration/components/StaffManagementPage.tsx"));

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
        path: '/forgot-password',
        element: withSuspense(<ForgotPasswordForm/>),
    },
    {
        path: '/reset-password',
        element: withSuspense(<ResetPasswordForm/>),
    },
    {
        path: '/verify-email',
        element: withSuspense(<VerifyEmailPage/>),
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
                path: "offers",
                element: withSuspense(<OffersPage/>),
            },
            {
                path: "offers/new",
                element: withSuspense(<CreateOfferPage/>),
            },
            {
                path: "offers/:id",
                element: withSuspense(<OfferDetailPage/>),
            },
            {
                path: "offers/:id/edit",
                element: withSuspense(<EditOfferPage/>),
            },
            {
                path: "applications",
                element: withSuspense(<ApplicationsPage/>),
            },
            {
                path: "applications/:id",
                element: withSuspense(<ApplicationDetailPage/>),
            },
            {
                path: "interviews",
                element: withSuspense(<InterviewsPage/>),
            },
            {
                path: "departments",
                element: withSuspense(<DepartmentsPage/>),
            },
            {
                path: "slots",
                element: withSuspense(<SlotsPage/>),
            },
            {
                path: "admin/staff",
                element: withSuspense(<StaffManagementPage/>),
            },
            {
                path: "settings",
                children: [
                    {
                        index: true,
                        element: <Navigate to="profile" replace/>,
                    },
                    {
                        path: "profile",
                        element: withSuspense(<ProfileSettings/>),
                    },
                    {
                        path: "password",
                        element: withSuspense(<PasswordSettings/>),
                    },
                    {
                        path: "delete",
                        element: withSuspense(<DeleteAccount/>),
                    }
                ]
            },
            {
                path: '*',
                element: withSuspense(<NotFoundPage/>),
            }
        ]
    },
    {
        path: '*',
        element:
            withSuspense(<NotFoundPage/>),
    }
])


export default router;