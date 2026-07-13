import React, {useMemo, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {useOffers} from '@/features/offers/hooks/useOffers.ts';
import OfferCard from '@/features/offers/components/OfferCard.tsx';
import CreateOfferCard from '@/features/offers/components/CreateOfferCard.tsx';
import OfferFiltersPanel from '@/features/offers/components/OfferFilters.tsx';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {
    applyOfferFilters,
    countActiveFilters,
    defaultOfferFilters,
    deriveOfferFacets,
} from '@/features/offers/utils/filterOffers.ts';
import {formatAmount} from '@/features/offers/utils/offerFormat.ts';
import type {OfferFilters} from '@/features/offers/types/offer.types.ts';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import '@/features/offers/styles/Offers.css';

const PAGE_SIZE = 12;

const OffersPage: React.FC = () => {
    const {t, i18n} = useTranslation();
    const {data, isLoading, isError, refetch} = useOffers();
    const {isStaff} = usePermissions();

    const [filters, setFilters] = useState<OfferFilters>(defaultOfferFilters);
    const [page, setPage] = useState(0);
    const [showFilters, setShowFilters] = useState(false); // mobile: panel collapsed by default

    const offers = useMemo(() => data?.offers ?? [], [data]);
    const facets = useMemo(() => deriveOfferFacets(offers), [offers]);
    const filtered = useMemo(
        () => applyOfferFilters(offers, filters, facets.salary),
        [offers, filters, facets.salary],
    );

    const activeCount = countActiveFilters(filters);

    // Masthead figures: all read off the currently filtered set so the "index"
    // always describes what the visitor is actually looking at.
    const dateline = new Date().toLocaleDateString(i18n.language, {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    });
    const departmentCount = useMemo(
        () => new Set(filtered.map((o) => o.department.name)).size,
        [filtered],
    );
    const openCount = useMemo(
        () => filtered.filter((o) => o.status === 'OPEN').length,
        [filtered],
    );

    const updateFilters = (next: OfferFilters) => {
        setFilters(next);
        setPage(0); // any filter change returns to the first page
    };
    const clearFilters = () => updateFilters(defaultOfferFilters);

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (isError) {
        return (
            <div className="offers-container">
                <div className="offers-state offers-error">
                    <p>{t('offers.loadError')}</p>
                    <button className="offers-retry" onClick={() => refetch()}>
                        {t('common.tryAgain')}
                    </button>
                </div>
            </div>
        );
    }

    // No offers exist at all — distinct from "none match your filters" below.
    if (offers.length === 0) {
        return (
            <div className="offers-container">
                <header className="offers-masthead offers-masthead--bare">
                    <div className="offers-masthead-head">
                        <p className="offers-dateline">{dateline}</p>
                        <h1 className="offers-title">{t('offers.title')}</h1>
                        <p className="offers-tagline">
                            {isStaff ? t('offers.tagline.staff') : t('offers.tagline.candidate')}
                        </p>
                    </div>
                </header>
                <div className="offers-rule" aria-hidden="true"/>
                {isStaff ? (
                    <ul className="offers-grid">
                        <li className="offers-grid-item">
                            <CreateOfferCard/>
                        </li>
                    </ul>
                ) : (
                    <div className="offers-state offers-empty">
                        <p>{t('offers.empty')}</p>
                    </div>
                )}
            </div>
        );
    }

    const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    const currentPage = Math.min(page, pageCount - 1);
    const visible = filtered.slice(currentPage * PAGE_SIZE, currentPage * PAGE_SIZE + PAGE_SIZE);

    // Active selections as removable chips — each clears just its own value.
    // Especially useful on mobile, where the panel is collapsed.
    const without = (patch: Partial<OfferFilters>) => updateFilters({...filters, ...patch});
    const chips: {key: string; label: string; clear: () => void}[] = [];
    if (filters.search.trim()) {
        chips.push({key: 'search', label: `“${filters.search.trim()}”`, clear: () => without({search: ''})});
    }
    if (filters.department) {
        chips.push({key: 'dept', label: filters.department, clear: () => without({department: ''})});
    }
    if (filters.city) {
        chips.push({key: 'city', label: t(`offers.cities.${filters.city}`), clear: () => without({city: ''})});
    }
    filters.remotePolicies.forEach((v) => chips.push({
        key: `rp-${v}`,
        label: t(`offers.remotePolicy.${v}`),
        clear: () => without({remotePolicies: filters.remotePolicies.filter((x) => x !== v)}),
    }));
    filters.employmentTypes.forEach((v) => chips.push({
        key: `et-${v}`,
        label: t(`offers.employmentType.${v}`),
        clear: () => without({employmentTypes: filters.employmentTypes.filter((x) => x !== v)}),
    }));
    filters.contractTypes.forEach((v) => chips.push({
        key: `ct-${v}`,
        label: t(`offers.contractType.${v}`),
        clear: () => without({contractTypes: filters.contractTypes.filter((x) => x !== v)}),
    }));
    filters.experienceLevels.forEach((v) => chips.push({
        key: `el-${v}`,
        label: t(`offers.experienceLevel.${v}`),
        clear: () => without({experienceLevels: filters.experienceLevels.filter((x) => x !== v)}),
    }));
    if (filters.disclosedSalaryOnly) {
        chips.push({key: 'disc', label: t('offers.filters.disclosedOnly'), clear: () => without({disclosedSalaryOnly: false})});
    }
    if (filters.minSalary > 0 && facets.salary) {
        chips.push({
            key: 'minsal',
            label: `≥ ${formatAmount(filters.minSalary, facets.salary.currency, i18n.language)}`,
            clear: () => without({minSalary: 0}),
        });
    }

    return (
        <div className="offers-container">
            <header className="offers-masthead">
                <div className="offers-masthead-head">
                    <p className="offers-dateline">{dateline}</p>
                    <h1 className="offers-title">{t('offers.title')}</h1>
                    <p className="offers-tagline">
                        {isStaff ? t('offers.tagline.staff') : t('offers.tagline.candidate')}
                    </p>
                </div>
                <dl className="offers-index" aria-label={t('offers.count', {count: filtered.length})}>
                    <div className="offers-index-item">
                        <dt className="offers-index-label">{t('offers.index.offers')}</dt>
                        <dd className="offers-index-value">{filtered.length}</dd>
                    </div>
                    <div className="offers-index-item">
                        <dt className="offers-index-label">{t('offers.index.departments')}</dt>
                        <dd className="offers-index-value">{departmentCount}</dd>
                    </div>
                    <div className="offers-index-item offers-index-open">
                        <dt className="offers-index-label">{t('offers.index.open')}</dt>
                        <dd className="offers-index-value">
                            <span className="offers-live-dot" aria-hidden="true"/>
                            {openCount}
                        </dd>
                    </div>
                </dl>
            </header>
            <div className="offers-rule" aria-hidden="true"/>

            <button
                type="button"
                className="offers-filter-toggle"
                aria-expanded={showFilters}
                aria-controls="offers-filter-panel"
                onClick={() => setShowFilters((v) => !v)}
            >
                {t('offers.filters.toggle')}
                {activeCount > 0 && <span className="filter-count-badge">{activeCount}</span>}
            </button>

            <div className="offers-layout">
                <OfferFiltersPanel
                    id="offers-filter-panel"
                    className={showFilters ? 'is-open' : ''}
                    facets={facets}
                    filters={filters}
                    activeCount={activeCount}
                    onChange={updateFilters}
                    onClear={clearFilters}
                />

                <section className="offers-results">
                    {chips.length > 0 && (
                        <div className="offers-active-bar">
                            <ul className="offers-active" aria-label={t('offers.filters.activeLabel')}>
                                {chips.map((chip) => (
                                    <li key={chip.key}>
                                        <button
                                            type="button"
                                            className="offers-chip"
                                            onClick={chip.clear}
                                            aria-label={t('offers.filters.removeFilter', {name: chip.label})}
                                        >
                                            <span className="offers-chip-label">{chip.label}</span>
                                            <span className="offers-chip-remove" aria-hidden="true">×</span>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                            <button type="button" className="offers-clear" onClick={clearFilters}>
                                {t('offers.filters.clearAll')}
                            </button>
                        </div>
                    )}

                    {filtered.length === 0 ? (
                        <div className="offers-state offers-empty">
                            <p>{t('offers.filters.noMatches')}</p>
                            <button className="offers-retry" onClick={clearFilters}>
                                {t('offers.filters.clearAll')}
                            </button>
                        </div>
                    ) : (
                        <>
                            <ul className="offers-grid">
                                {isStaff && currentPage === 0 && (
                                    <li className="offers-grid-item">
                                        <CreateOfferCard/>
                                    </li>
                                )}
                                {visible.map((offer) => (
                                    <li key={offer.id} className="offers-grid-item">
                                        <OfferCard offer={offer}/>
                                    </li>
                                ))}
                            </ul>

                            {pageCount > 1 && (
                                <nav className="offers-pagination" aria-label={t('offers.pagination')}>
                                    <button
                                        className="offers-page-button"
                                        onClick={() => setPage(Math.max(0, currentPage - 1))}
                                        disabled={currentPage === 0}
                                    >
                                        {t('common.previous')}
                                    </button>
                                    <span className="offers-page-indicator">
                                        {t('offers.pageIndicator', {current: currentPage + 1, total: pageCount})}
                                    </span>
                                    <button
                                        className="offers-page-button"
                                        onClick={() => setPage(Math.min(pageCount - 1, currentPage + 1))}
                                        disabled={currentPage >= pageCount - 1}
                                    >
                                        {t('common.next')}
                                    </button>
                                </nav>
                            )}
                        </>
                    )}
                </section>
            </div>
        </div>
    );
};

export default OffersPage;
