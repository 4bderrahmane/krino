import i18n from "i18next";
import {initReactI18next} from "react-i18next";
import enLanguage from './en/en.json';
import frLanguage from './fr/fr.json';
import enErrors from './en/errors.json';
import frErrors from './fr/errors.json';

// The 'translation' namespace holds UI copy; the 'errors' namespace holds
// messages keyed by the server's ErrorCode enum (see shared/services/errors.ts).
// Keeping server error vocabulary in its own namespace lets every feature share
// one source of truth without bloating the UI copy file.
const resources = {
    en: {
        translation: enLanguage,
        errors: enErrors,
    },
    fr: {
        translation: frLanguage,
        errors: frErrors,
    }
};

i18n
    .use(initReactI18next) // passes i18n down to react-i18next
    .init({
        resources,
        fallbackLng: "en",
        lng: "en", // language to use, more information here: https://www.i18next.com/overview/configuration-options#languages-namespaces-resources
        // you can use the i18n.changeLanguage function to change the language manually: https://www.i18next.com/overview/api#changelanguage
        // if you're using a language detector, do not define the lng option
        ns: ['translation', 'errors'],
        defaultNS: 'translation',

        interpolation: {
            escapeValue: false // react already safes from xss
        }
    });

export default i18n;