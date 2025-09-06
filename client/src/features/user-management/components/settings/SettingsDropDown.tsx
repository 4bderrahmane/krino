import {NavLink} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import '../../styles/settings/SettingsDropDown.css';

interface SettingsDropDownProps {
    onClose?: () => void;
}

const SettingsDropdown: React.FC<SettingsDropDownProps> = ({ onClose }) => {
    const {t} = useTranslation();

    const handleClick = () => {
        if (onClose) onClose();
    };

    return (
        <div className="settings-nav-container">
            <div className="settings-nav">
                <NavLink to="/settings/profile" className="settings-nav-link" onClick={handleClick}>
                    {t('settings.modifyProfile')}
                </NavLink>
                <NavLink to="/settings/password" className="settings-nav-link" onClick={handleClick}>
                    {t('settings.password')}
                </NavLink>
                <NavLink to="/settings/delete" className="settings-nav-link settings-nav-link-delete" onClick={handleClick}>
                    {t('settings.deleteAccount')}
                </NavLink>
            </div>
        </div>
    );
};

export default SettingsDropdown;