import '@/shared/styles/BrandLogo.css';

type BrandLogoVariant = 'navbar' | 'auth';

interface BrandLogoProps {
    variant?: BrandLogoVariant;
    className?: string;
}

const BrandLogo = ({variant = 'navbar', className = ''}: BrandLogoProps) => {
    const classes = ['brand-logo', `brand-logo--${variant}`, className]
        .filter(Boolean)
        .join(' ');

    return (
        <span className={classes} role="img" aria-label="KRINO">
            <img className="brand-logo-mark" src="/krino-logo.png" alt="" aria-hidden="true"/>
            <span className="brand-logo-wordmark" aria-hidden="true">RINO</span>
        </span>
    );
};

export default BrandLogo;
