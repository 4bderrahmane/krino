import {type ReactNode} from "react";
import {RouterProvider} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {AuthProvider} from './shared/contexts/AuthContext';
import router from "./routes/routes.tsx";

const queryClient = new QueryClient();

function ErrorBoundary(props: { children: ReactNode }) {
    return <>{props.children}</>;
}

function App() {
    return (
        <ErrorBoundary>
            <QueryClientProvider client={queryClient}>
                <AuthProvider>
                    <RouterProvider router={router}/>
                </AuthProvider>
            </QueryClientProvider>
        </ErrorBoundary>
    );
}

export default App;