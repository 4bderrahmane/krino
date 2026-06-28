import React from 'react';

interface ErrorBoundaryProps {
    children: React.ReactNode;
}

interface ErrorBoundaryState {
    hasError: boolean;
}

/**
 * App-level error boundary. Catches render-time errors anywhere in the tree and
 * shows a recoverable fallback instead of a blank white screen. Intentionally
 * does not use i18n/router hooks, since the failure may be in those very layers.
 */
class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
    state: ErrorBoundaryState = {hasError: false};

    static getDerivedStateFromError(): ErrorBoundaryState {
        return {hasError: true};
    }

    componentDidCatch(error: unknown, info: React.ErrorInfo) {
        // Last-resort logging hook; wire to a real reporter (e.g. Sentry) later.
        console.error('Unhandled UI error:', error, info);
    }

    private readonly handleReload = () => {
        window.location.assign('/');
    };

    render() {
        if (!this.state.hasError) {
            return this.props.children;
        }

        return (
            <div
                role="alert"
                style={{
                    minHeight: '100vh',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '12px',
                    padding: '24px',
                    textAlign: 'center',
                    fontFamily: 'system-ui, sans-serif',
                }}
            >
                <h1 style={{fontSize: '1.5rem', margin: 0}}>Something went wrong</h1>
                <p style={{color: '#6b675f', margin: 0}}>
                    An unexpected error occurred. Please try reloading the page.
                </p>
                <button
                    onClick={this.handleReload}
                    style={{
                        marginTop: '8px',
                        padding: '10px 22px',
                        borderRadius: '9999px',
                        border: 'none',
                        background: '#1f1d1a',
                        color: '#fff',
                        fontWeight: 600,
                        cursor: 'pointer',
                    }}
                >
                    Reload
                </button>
            </div>
        );
    }
}

export default ErrorBoundary;
