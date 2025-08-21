


return (
    <div className="login-container">
        <div className="login-card">
            <h2 className="page-title-animate">{translate('auth.login')}</h2>
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="username">{translate('auth.username')}</label>
                    <input
                        type="text"
                        id="username"
                        name="username"
                        value={credentials.username}
                        onChange={handleChange}
                        className={`form-input ${errors.username ? 'input-error' : ''}`}
                        placeholder={translate('auth.enterUsername')}
                        disabled={isLoading}
                    />
                    {errors.username && (
                        <div className="error-message">{errors.username}</div>
                    )}
                </div>

                <div className="form-group">
                    <label htmlFor="password">{translate('auth.password')}</label>
                    <input
                        type="password"
                        id="password"
                        name="password"
                        value={credentials.password}
                        onChange={handleChange}
                        className={`form-input ${errors.password ? 'input-error' : ''}`}
                        placeholder={translate('auth.enterPassword')}
                        disabled={isLoading}
                    />
                    {errors.password && (
                        <div className="error-message">{errors.password}</div>
                    )}
                </div>

                <button type="submit" className="login-button" disabled={isLoading}>
                    {isLoading ? translate('app.loading') : translate('auth.login')}
                </button>
            </form>

            <div className="login-footer">
                <Link to="/forgot-password">{translate('auth.forgotPassword')}</Link>
                <p>
                    {translate('auth.noAccount')}{' '}
                    <Link
                        to="/signup"
                        className="font-medium text-blue-600 hover:text-blue-500"
                    >
                        {translate('auth.createAccount')}
                    </Link>
                </p>
            </div>
        </div>
    </div>
);
};