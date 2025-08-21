import React, { useState } from 'react';

const RegistrationForm: React.FC = () => {

    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
    });

    // 3. A single handler to update the state object dynamically
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prevState => ({
            ...prevState, // Keep the existing values
            [name]: value,  // Update the specific field that changed
        }));
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        // The complete form data is available here for submission
        console.log('Submitting Registration:', formData);
        // Example: send formData to your API endpoint
    };

    return (
        <form onSubmit={handleSubmit}>
            <h2>Register</h2>
            <input
                type="text"
                name="username" // The 'name' attribute must match the state property
                placeholder="Username"
                // 2. Link the input value to the state
                value={formData.username}
                onChange={handleChange}
            />
            <input
                type="email"
                name="email"
                placeholder="Email"
                value={formData.email}
                onChange={handleChange}
            />
            <input
                type="password"
                name="password"
                placeholder="Password"
                value={formData.password}
                onChange={handleChange}
            />
            <button type="submit">Register</button>
        </form>
    );
};

export default RegistrationForm;