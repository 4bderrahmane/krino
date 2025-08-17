import React, {useState, type FormEvent} from 'react';
import {Link} from 'react-router-dom';
import type {User, UserLoginDTO} from '../types/api.types';
import '../styles/LoginForm.css';

interface LoginFormProps {
    onLogin: (user: Partial<User>) => void;
}

const LoginForm: React.FC<LoginFormProps> = ({onLogin}) => {
    const [credentials, setCredentials] = useState<UserLoginDTO>({
        email: '',
        password: '',
    });
    const [errors, setErrors] = useState<Partial<UserLoginDTO>>({});
    const [isLoading, setIsLoading] = useState<boolean>(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setCredentials({
            ...credentials,
            [name]: value,
        });

        // Clear error when user types
        if (errors[name as keyof UserLoginDTO]) {
            setErrors({
                ...errors,
                [name]: '',
            });
        }
    };

    const validate = (): boolean => {
        const newErrors: Partial<UserLoginDTO> = {};

        if (!credentials.email) {
            newErrors.email = 'Email is required';
        }

        if (!credentials.password) {
            newErrors.password = 'Password is required';
        } else if (credentials.password.length < 6) {
            newErrors.password = 'Password must be at least 6 characters';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();

        if (!validate()) {
            return;
        }

        setIsLoading(true);

        try {
            console.log('Logging in with:', credentials);

            // Simulate API call
            await new Promise((resolve) => setTimeout(resolve, 1000));

            // Create a mock user for demonstration
            const mockUser: Partial<User> = {
                name: credentials.email.split('@')[0],
                email: credentials.email,
            };

            onLogin(mockUser);
        } catch (error) {
            console.error('Login failed:', error);
            alert('Login failed. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h2>Login</h2>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="email">Email</label>
                        <input
                            type="email"
                            id="email"
                            name="email"
                            value={credentials.email}
                            onChange={handleChange}
                            className={`form-input ${errors.email ? 'input-error' : ''}`}
                            placeholder="Enter your email"
                            disabled={isLoading}
                        />
                        {errors.email && (
                            <div className="error-message">{errors.email}</div>
                        )}
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            value={credentials.password}
                            onChange={handleChange}
                            className={`form-input ${errors.password ? 'input-error' : ''}`}
                            placeholder="Enter your password"
                            disabled={isLoading}
                        />
                        {errors.password && (
                            <div className="error-message">{errors.password}</div>
                        )}
                    </div>

                    <button type="submit" className="login-button" disabled={isLoading}>
                        {isLoading ? 'Loading...' : 'Login'}
                    </button>
                </form>

                <div className="login-footer">
                    <Link to="/forgot-password">Forgot password?</Link>
                    <p>
                        Don't have an account?{' '}
                        <Link to="/signup" className="signup-link">
                            Sign up
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default LoginForm;
