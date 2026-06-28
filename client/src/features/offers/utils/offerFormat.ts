// Shared, locale-aware formatting for the offers feature.

export const formatNumber = (value: number, locale: string): string =>
    new Intl.NumberFormat(locale, {maximumFractionDigits: 0}).format(value);

/** A salary amount with its currency code, e.g. "140 000 MAD". */
export const formatAmount = (value: number, currency: string, locale: string): string =>
    `${formatNumber(value, locale)} ${currency}`;

// Deadlines arrive as plain ISO dates ("2026-06-30"). Build the Date from its
// parts so it is read in the local zone — `new Date("2026-06-30")` is UTC
// midnight and can render as the day before.
export const formatDate = (iso: string, locale: string): string => {
    const [y, m, d] = iso.split('-').map(Number);
    if (!y || !m || !d) return iso;
    return new Intl.DateTimeFormat(locale, {day: 'numeric', month: 'short', year: 'numeric'})
        .format(new Date(y, m - 1, d));
};
