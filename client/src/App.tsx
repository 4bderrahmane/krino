import {RouterProvider} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import router from "./routes/routes.tsx";
import {AuthProvider} from "./shared/contexts/AuthContext.tsx";
import {ToastProvider} from "./shared/contexts/ToastContext.tsx";
import ErrorBoundary from "./shared/components/ErrorBoundary.tsx";

const queryClient = new QueryClient();

function App() {
    return (
        <ErrorBoundary>
            <QueryClientProvider client={queryClient}>
                <AuthProvider>
                    <ToastProvider>
                        <RouterProvider router={router}/>
                    </ToastProvider>
                </AuthProvider>
            </QueryClientProvider>
        </ErrorBoundary>
    );
}

export default App;