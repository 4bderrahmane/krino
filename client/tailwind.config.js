/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",          // include index.html
        "./src/**/*.{js,ts,jsx,tsx}", // include all React + TS files
    ],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Lora', 'Georgia', 'Times New Roman', 'serif'],
                serif: ['Lora', 'Georgia', 'Times New Roman', 'serif'],
            },
        },
    },
    plugins: [],
}
