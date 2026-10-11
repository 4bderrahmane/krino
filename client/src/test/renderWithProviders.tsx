import type {ReactElement} from 'react';
import {render} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {RouterProvider, createMemoryRouter} from 'react-router-dom';
import {I18nextProvider} from 'react-i18next';
import i18n from '@/utils/i18n';
import {AuthProvider} from '@/shared/contexts/AuthContext.tsx';
import {ToastProvider} from '@/shared/contexts/ToastContext.tsx';
import {ThemeProvider} from '@/shared/contexts/ThemeContext.tsx';
import {routes} from '@/routes/routes.tsx';

/**
 * The provider stack App.tsx builds, minus the browser router.
 *
 * retry is off because the default backoff turns "assert this failed" into a test
 * that hangs for seconds before agreeing. i18n is the real instance rather than a
 * stub that echoes keys, so a missing translation fails a test instead of quietly
 * rendering a key name at the user.
 */
const testQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {retry: false},
            mutations: {retry: false},
        },
    });

/** Mounts the real route tree at a given URL. */
export const renderApp = (initialPath: string) => {
    const router = createMemoryRouter(routes, {initialEntries: [initialPath]});

    const view = render(
        <ThemeProvider>
            <QueryClientProvider client={testQueryClient()}>
                <AuthProvider>
                    <ToastProvider>
                        <RouterProvider router={router}/>
                    </ToastProvider>
                </AuthProvider>
            </QueryClientProvider>
        </ThemeProvider>,
    );

    return {...view, router, currentPath: () => router.state.location.pathname};
};

interface RenderOptions {
    /** Route pattern the component is mounted at, so useParams resolves. */
    path?: string;
    /** URL to start on, which must match `path`. */
    at?: string;
}

/**
 * Mounts one component inside the same providers, on a throwaway route.
 *
 * Pass `path`/`at` for components that read route params. Deliberately without the
 * app Layout: a component test should not have to disambiguate its own buttons from
 * the NavBar's.
 */
export const renderWithProviders = (ui: ReactElement, {path = '/', at = '/'}: RenderOptions = {}) => {
    const router = createMemoryRouter([{path, element: ui}], {initialEntries: [at]});

    return render(
        <I18nextProvider i18n={i18n}>
            <ThemeProvider>
                <QueryClientProvider client={testQueryClient()}>
                    <AuthProvider>
                        <ToastProvider>
                            <RouterProvider router={router}/>
                        </ToastProvider>
                    </AuthProvider>
                </QueryClientProvider>
            </ThemeProvider>
        </I18nextProvider>,
    );
};
