import React from 'react';
import {useTranslation} from 'react-i18next';
import type {MoroccanCity, OfferFilters} from '@/features/offers/types/offer.types.ts';
import type {OfferFacets} from '@/features/offers/utils/filterOffers.ts';
import {formatAmount} from '@/features/offers/utils/offerFormat.ts';
import '@/features/offers/styles/OfferFilters.css';

interface OfferFiltersPanelProps {
    id: string;
    className?: string;
    facets: OfferFacets;
    filters: OfferFilters;
    activeCount: number;
    onChange: (next: OfferFilters) => void;
    onClear: () => void;
}

const toggle = <T,>(list: T[], value: T): T[] =>
    list.includes(value) ? list.filter((v) => v !== value) : [...list, value];

// A round step that keeps the slider usable across salary magnitudes.
const salaryStep = (max: number): number => {
    if (max <= 5000) return 250;
    if (max <= 20000) return 500;
    if (max <= 100000) return 1000;
    return 5000;
};

const OfferFiltersPanel: React.FC<OfferFiltersPanelProps> = ({
    id,
    className,
    facets,
    filters,
    activeCount,
    onChange,
    onClear,
}) => {
    const {t, i18n} = useTranslation();
    const locale = i18n.language;

    // A multi-select checkbox facet. `ns` is the i18n namespace for option labels
    // (e.g. "offers.remotePolicy"), so option text reuses the existing enum keys.
    const checkboxFacet = <T extends string>(
        legend: string,
        options: readonly T[],
        selected: T[],
        ns: string,
        apply: (next: T[]) => void,
    ) => (
        <fieldset className="filter-section filter-group">
            <legend className="filter-legend">{legend}</legend>
            <div className="filter-options">
                {options.map((value) => (
                    <label key={value} className="filter-check">
                        <input
                            type="checkbox"
                            checked={selected.includes(value)}
                            onChange={() => apply(toggle(selected, value))}
                        />
                        <span>{t(`${ns}.${value}`)}</span>
                    </label>
                ))}
            </div>
        </fieldset>
    );

    const sortedCities = [...facets.cities].sort((a, b) =>
        t(`offers.cities.${a}`).localeCompare(t(`offers.cities.${b}`), locale));

    return (
        <aside id={id} className={`offers-sidebar ${className ?? ''}`} aria-label={t('offers.filters.title')}>
            <form className="filter-panel" onSubmit={(e) => e.preventDefault()}>
                <div className="filter-panel-head">
                    <h2 className="filter-panel-title">{t('offers.filters.title')}</h2>
                    {activeCount > 0 && (
                        <button type="button" className="filter-clear" onClick={onClear}>
                            {t('offers.filters.clearAll')}
                        </button>
                    )}
                </div>

                <div className="filter-section">
                    <label className="filter-legend" htmlFor="offer-search">{t('offers.filters.search')}</label>
                    <input
                        id="offer-search"
                        type="search"
                        className="filter-search"
                        placeholder={t('offers.filters.searchPlaceholder')}
                        value={filters.search}
                        onChange={(e) => onChange({...filters, search: e.target.value})}
                    />
                </div>

                {facets.remotePolicies.length > 1 && checkboxFacet(
                    t('offers.filters.workArrangement'),
                    facets.remotePolicies,
                    filters.remotePolicies,
                    'offers.remotePolicy',
                    (next) => onChange({...filters, remotePolicies: next}),
                )}

                {facets.employmentTypes.length > 1 && checkboxFacet(
                    t('offers.filters.employmentType'),
                    facets.employmentTypes,
                    filters.employmentTypes,
                    'offers.employmentType',
                    (next) => onChange({...filters, employmentTypes: next}),
                )}

                {facets.contractTypes.length > 1 && checkboxFacet(
                    t('offers.filters.contractType'),
                    facets.contractTypes,
                    filters.contractTypes,
                    'offers.contractType',
                    (next) => onChange({...filters, contractTypes: next}),
                )}

                {facets.experienceLevels.length > 1 && checkboxFacet(
                    t('offers.filters.experienceLevel'),
                    facets.experienceLevels,
                    filters.experienceLevels,
                    'offers.experienceLevel',
                    (next) => onChange({...filters, experienceLevels: next}),
                )}

                {facets.departments.length > 1 && (
                    <div className="filter-section">
                        <label className="filter-legend" htmlFor="offer-department">
                            {t('offers.filterDepartment')}
                        </label>
                        <select
                            id="offer-department"
                            className="filter-select"
                            value={filters.department}
                            onChange={(e) => onChange({...filters, department: e.target.value})}
                        >
                            <option value="">{t('offers.allDepartments')}</option>
                            {facets.departments.map((name) => (
                                <option key={name} value={name}>{name}</option>
                            ))}
                        </select>
                    </div>
                )}

                {facets.cities.length > 1 && (
                    <div className="filter-section">
                        <label className="filter-legend" htmlFor="offer-city">{t('offers.filters.city')}</label>
                        <select
                            id="offer-city"
                            className="filter-select"
                            value={filters.city}
                            onChange={(e) => onChange({...filters, city: e.target.value as MoroccanCity | ''})}
                        >
                            <option value="">{t('offers.filters.allCities')}</option>
                            {sortedCities.map((city) => (
                                <option key={city} value={city}>{t(`offers.cities.${city}`)}</option>
                            ))}
                        </select>
                    </div>
                )}

                {facets.salary && (
                    <div className="filter-section">
                        <span className="filter-legend">{t('offers.filters.salary')}</span>
                        <label className="filter-check">
                            <input
                                type="checkbox"
                                checked={filters.disclosedSalaryOnly}
                                onChange={(e) => onChange({...filters, disclosedSalaryOnly: e.target.checked})}
                            />
                            <span>{t('offers.filters.disclosedOnly')}</span>
                        </label>
                        <div className="filter-salary">
                            <div className="filter-salary-value">
                                {filters.minSalary > 0
                                    ? t('offers.filters.minSalaryValue', {
                                        amount: formatAmount(filters.minSalary, facets.salary.currency, locale),
                                        period: t(`offers.salaryPeriod.${facets.salary.period}`),
                                    })
                                    : `${t('offers.filters.minSalary')}: ${t('offers.filters.minSalaryAny')}`}
                            </div>
                            <input
                                type="range"
                                className="filter-range"
                                min={0}
                                max={facets.salary.max}
                                step={salaryStep(facets.salary.max)}
                                value={Math.min(filters.minSalary, facets.salary.max)}
                                onChange={(e) => onChange({...filters, minSalary: Number(e.target.value)})}
                                aria-label={t('offers.filters.minSalary')}
                            />
                        </div>
                    </div>
                )}
            </form>
        </aside>
    );
};

export default OfferFiltersPanel;
