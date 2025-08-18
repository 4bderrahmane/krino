import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import './utils/i18n'
import LoadingSpinner from "./shared/components/LoadingSpinner.tsx";

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />

      <LoadingSpinner>
      </LoadingSpinner>
  </StrictMode>,
)
