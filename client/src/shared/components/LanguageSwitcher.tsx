import React, {useState} from 'react';
import {useTranslation} from 'react-i18next';
import type {Language} from "@/shared/types/types";
import enFlag from '@/assets/us.svg';
import frFlag from '@/assets/fr.svg';
import '@/shared/styles/LanguageSwitcher.css';

const LanguageSwitcher: React.FC = () => {
    const {i18n} = useTranslation();

    const languages: Language[] = [
        {code: "en", name: "English", flag: enFlag},
        {code: "fr", name: "Français", flag: frFlag},
    ];

    const [selectedLang, setSelectedLang] = useState<Language>(
        languages.find(lang => lang.code === i18n.language) || languages[0]
    );
    const [open, setOpen] = useState(false);

    const handleSelect = (lang: Language) => {
        setSelectedLang(lang);
        i18n.changeLanguage(lang.code);
        setOpen(false);
    };

    return (
        <div className="language-switcher">
            <button
                className="language-switcher__button"
                onClick={() => setOpen(!open)}
            >
                <img
                    src={selectedLang.flag}
                    alt={selectedLang.name}
                    className="language-switcher__flag"
                />
                {selectedLang.name}
            </button>

            {open && (
                <ul className="language-switcher__dropdown">
                    {languages.map(lang => (
                        <li key={lang.code} className="language-switcher__item">
                            <button onClick={() => handleSelect(lang)}>
                                <img
                                    src={lang.flag}
                                    alt={lang.name}
                                    className="language-switcher__flag"
                                />
                                {lang.name}
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default LanguageSwitcher;