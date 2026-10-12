import {useTranslation} from 'react-i18next';
import './PaginationControls.css';

interface PaginationControlsProps {
    label: string;
    page: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}

const PaginationControls = ({label, page, totalPages, onPageChange}: PaginationControlsProps) => {
    const {t} = useTranslation();
    if (totalPages <= 1) return null;

    return (
        <nav className="pagination-controls" aria-label={label}>
            <button type="button" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
                {t('common.previous')}
            </button>
            <span aria-live="polite">{t('common.pageIndicator', {current: page + 1, total: totalPages})}</span>
            <button type="button" disabled={page >= totalPages - 1}
                    onClick={() => onPageChange(page + 1)}>
                {t('common.next')}
            </button>
        </nav>
    );
};

export default PaginationControls;
