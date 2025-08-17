// import {useState} from 'react'
import './App.css'
// import LoginForm from './features/authentication/components/LoginForm'
// import type {User} from './features/authentication/types/api.types'
import {useTranslation} from "react-i18next";

function App() {
    const {t} = useTranslation();
    return (
        // <div className="app">
        //     <LoginForm onLogin={handleLogin}/>
        // </div>
        <div>{t('welcomeMessage')}</div>
    )
}

// function App() {
//     const [user, setUser] = useState<User | null>(null)
//
//     const handleLogin = (userData: Partial<User>) => {
//         console.log('User logged in:', userData)
//         setUser(userData as User)
//     }
//
//     if (user) {
//         return (
//             <div className="app">
//                 <div style={{
//                     textAlign: 'center',
//                     background: 'white',
//                     padding: '2rem',
//                     borderRadius: '8px',
//                     boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
//                 }}>
//                     <h1>Welcome, {user.name}!</h1>
//                     <p>Email: {user.email}</p>
//                     <button onClick={() => setUser(null)} style={{
//                         marginTop: '1rem',
//                         padding: '0.5rem 1rem',
//                         backgroundColor: '#ef4444',
//                         color: 'white',
//                         border: 'none',
//                         borderRadius: '4px',
//                         cursor: 'pointer'
//                     }}>
//                         Logout
//                     </button>
//                 </div>
//             </div>
//         )
//     }

// }

export default App