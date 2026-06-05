import { TypeAnimation } from 'react-type-animation';
import {useTranslation} from 'react-i18next';

export default function Welcome() {
    const {t, i18n} = useTranslation();

    return (
        <div className="text-xl font-mono">
            <TypeAnimation
                key={i18n.language}
                sequence={[
                    t('app.welcome'),
                    1000,
                ]}
                speed={40}
                repeat={0}
            />
        </div>
    );
}
