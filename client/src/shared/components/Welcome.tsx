import {TypeAnimation} from 'react-type-animation';
import {useTranslation} from 'react-i18next';

function Welcome() {
    const {t, i18n} = useTranslation();

    return (
        <div className="text-xl font-serif">
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
export default Welcome;