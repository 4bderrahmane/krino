import {describe, expect, it} from 'vitest';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {http, HttpResponse} from 'msw';
import {AuthProvider} from './AuthContext.tsx';
import {useAuth} from '@/shared/hooks/useAuth';
import {useApplications} from '@/features/applications/hooks/useApplications.ts';
import {useInterviews} from '@/features/interviews/hooks/useInterviews.ts';
import api, {SESSION_ENDED_EVENT} from '@/shared/services/api.ts';
import {anApplicationResponse} from '@/test/fixtures.ts';
import {aUser, apiPath, server} from '@/test/msw/server.ts';
import type {UserResponseDTO} from '@/features/authentication/types/api.types.ts';

const first = aUser(['CANDIDATE'], {id: 'candidate-a', email: 'a@test.local'});
const second = aUser(['CANDIDATE'], {id: 'candidate-b', email: 'b@test.local'});

const page = (content: unknown[]) => ({
    content,
    page: {number: 0, size: 20, totalElements: content.length, totalPages: 1},
});

const applicationFor = (user: UserResponseDTO) =>
    anApplicationResponse({id: `application-${user.id}`, jobTitle: `Offer for ${user.id}`});

const Probe = ({onSwitch}: {onSwitch: () => void}) => {
    const {user, login, logout} = useAuth();
    const applications = useApplications('mine');
    const interviews = useInterviews('mine');

    return <>
        <output data-testid="account">{user?.id ?? 'guest'}</output>
        <output data-testid="applications">{applications.data?.applications.map((application) => application.offerTitle).join(', ') ?? ''}</output>
        <output data-testid="interviews">{interviews.data?.interviews.map((interview) => interview.id).join(', ') ?? ''}</output>
        <button onClick={logout}>Log out locally</button>
        <button onClick={() => { onSwitch(); login(second); }}>Switch account</button>
    </>;
};

const mount = (onSwitch: () => void) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
    render(<QueryClientProvider client={queryClient}>
        <AuthProvider><Probe onSwitch={onSwitch}/></AuthProvider>
    </QueryClientProvider>);
    return queryClient;
};

const useSessionHandlers = (current: () => UserResponseDTO) => {
    server.use(
        http.get(apiPath('/users/me'), () => HttpResponse.json(first)),
        http.get(apiPath('/applications/me'), () => HttpResponse.json(page([applicationFor(current())]))),
        http.get(apiPath('/interviews/me'), () => HttpResponse.json(page([{id: `interview-${current().id}`}])))
    );
};

describe('private query data across sessions', () => {
    it('removes the previous account cache before rendering the next account', async () => {
        let current = first;
        useSessionHandlers(() => current);
        const queryClient = mount(() => { current = second; });

        expect(await screen.findByText('Offer for candidate-a')).toBeInTheDocument();
        expect(await screen.findByText('interview-candidate-a')).toBeInTheDocument();
        expect(queryClient.getQueryData(['applications', 'mine', first.id, 0, 20])).toBeDefined();
        expect(queryClient.getQueryData(['interviews', 'mine', first.id, 0, 20])).toBeDefined();

        fireEvent.click(screen.getByRole('button', {name: 'Switch account'}));

        expect(queryClient.getQueryData(['applications', 'mine', first.id, 0, 20])).toBeUndefined();
        expect(queryClient.getQueryData(['interviews', 'mine', first.id, 0, 20])).toBeUndefined();
        expect(screen.queryByText('Offer for candidate-a')).not.toBeInTheDocument();
        expect(screen.queryByText('interview-candidate-a')).not.toBeInTheDocument();
        expect(await screen.findByText('Offer for candidate-b')).toBeInTheDocument();
        expect(await screen.findByText('interview-candidate-b')).toBeInTheDocument();
    });

    it('discards a private response that was in flight at logout', async () => {
        let current = first;
        let releaseFirst!: () => void;
        let startedFirst!: () => void;
        const held = new Promise<void>((resolve) => { releaseFirst = resolve; });
        const started = new Promise<void>((resolve) => { startedFirst = resolve; });
        let requestSignal: {aborted: boolean} | undefined;
        const interceptorId = api.interceptors.request.use((config) => {
            if (config.url === '/applications/me' && current.id === first.id) {
                requestSignal = config.signal;
            }
            return config;
        });
        try {
            server.use(
                http.get(apiPath('/users/me'), () => HttpResponse.json(first)),
                http.get(apiPath('/applications/me'), async () => {
                    const requestedBy = current;
                    if (requestedBy.id === first.id) {
                        startedFirst();
                        await held;
                    }
                    return HttpResponse.json(page([applicationFor(requestedBy)]));
                }),
                http.get(apiPath('/interviews/me'), () => HttpResponse.json(page([]))),
            );
            const queryClient = mount(() => { current = second; });

            await started;
            fireEvent.click(screen.getByRole('button', {name: 'Log out locally'}));
            expect(screen.getByTestId('account')).toHaveTextContent('guest');
            expect(requestSignal?.aborted).toBe(true);
            expect(queryClient.getQueryData(['applications', 'mine', first.id, 0, 20])).toBeUndefined();

            fireEvent.click(screen.getByRole('button', {name: 'Switch account'}));
            expect(await screen.findByText('Offer for candidate-b')).toBeInTheDocument();
            await act(async () => { releaseFirst(); });

            expect(queryClient.getQueryData(['applications', 'mine', first.id, 0, 20])).toBeUndefined();
            expect(screen.queryByText('Offer for candidate-a')).not.toBeInTheDocument();
            expect(screen.getByText('Offer for candidate-b')).toBeInTheDocument();
        } finally {
            releaseFirst();
            api.interceptors.request.eject(interceptorId);
        }
    });

    it('clears private data when the session expires on a public page', async () => {
        useSessionHandlers(() => first);
        const queryClient = mount(() => undefined);
        expect(await screen.findByText('Offer for candidate-a')).toBeInTheDocument();

        act(() => window.dispatchEvent(new Event(SESSION_ENDED_EVENT)));

        expect(screen.getByTestId('account')).toHaveTextContent('guest');
        expect(queryClient.getQueryData(['applications', 'mine', first.id, 0, 20])).toBeUndefined();
        expect(screen.queryByText('Offer for candidate-a')).not.toBeInTheDocument();
    });
});
