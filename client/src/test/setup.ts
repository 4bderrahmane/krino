import '@testing-library/jest-dom/vitest';
import {cleanup} from '@testing-library/react';
import {afterAll, afterEach, beforeAll} from 'vitest';
import {server} from './msw/server';

// jsdom implements no media queries, and ThemeProvider asks for the OS colour
// scheme on mount. Report "light" so themed components render deterministically.
if (!window.matchMedia) {
    window.matchMedia = (query: string): MediaQueryList => ({
        matches: false,
        media: query,
        onchange: null,
        addEventListener: () => undefined,
        removeEventListener: () => undefined,
        dispatchEvent: () => false,
        addListener: () => undefined,
        removeListener: () => undefined,
    });
}

// Every request goes through MSW. 'error' rather than 'warn' on an unhandled
// request: a call nobody stubbed is a test asserting against a failure it did not
// mean to cause, which is worse than a noisy failure here.
beforeAll(() => {
    server.listen({onUnhandledRequest: 'error'});
});

// Testing Library renders into a container it does not tear down on its own when
// globals are off, so unmount between tests; otherwise queries start matching
// leftovers from the previous test and failures point at the wrong thing.
afterEach(() => {
    cleanup();
    server.resetHandlers();
});

afterAll(() => {
    server.close();
});
