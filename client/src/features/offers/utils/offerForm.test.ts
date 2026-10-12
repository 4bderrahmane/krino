import {describe, expect, it} from 'vitest';
import i18n from '@/utils/i18n';
import {
    formToCreateInput,
    formToEditInput,
    initialState,
    isoToDatetimeLocal,
    offerToFormState,
    toIntOrNull,
    validateOfferForm,
    type FormState,
} from '@/features/offers/utils/offerForm.ts';
import {anOffer} from '@/test/fixtures.ts';

const t = i18n.getFixedT('en');

const aValidForm = (overrides: Partial<FormState> = {}): FormState => ({
    ...initialState,
    department: 'Engineering',
    title: 'Backend Engineer',
    remotePolicy: 'REMOTE',
    applyingDeadline: '2099-12-31T23:59',
    ...overrides,
});

describe('toIntOrNull', () => {
    it('maps an empty or blank string to null', () => {
        expect(toIntOrNull('')).toBeNull();
        expect(toIntOrNull('   ')).toBeNull();
    });

    it('parses an integer', () => {
        expect(toIntOrNull('42')).toBe(42);
    });

    it('truncates rather than rounding', () => {
        expect(toIntOrNull('2.9')).toBe(2);
    });

    it('maps a non-numeric string to null', () => {
        expect(toIntOrNull('abc')).toBeNull();
    });
});

describe('isoToDatetimeLocal', () => {
    it('returns an empty string for null or an unparseable value', () => {
        expect(isoToDatetimeLocal(null)).toBe('');
        expect(isoToDatetimeLocal('not a date')).toBe('');
    });

    it('renders the shape a datetime-local input accepts', () => {
        expect(isoToDatetimeLocal('2099-06-30T14:05:00.000Z')).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
    });

    // Whatever the timezone, seeding the edit form and submitting it unchanged
    // must not move the deadline.
    it('round-trips through the create mapping without shifting the instant', () => {
        const original = '2099-06-30T14:05:00.000Z';
        const form = aValidForm({applyingDeadline: isoToDatetimeLocal(original)});

        expect(formToCreateInput(form, []).applyingDeadline).toBe(original);
    });
});

describe('validateOfferForm', () => {
    it('accepts a complete form', () => {
        expect(validateOfferForm(aValidForm(), t)).toEqual({});
    });

    it('requires a department and a title', () => {
        const errors = validateOfferForm(aValidForm({department: '', title: '   '}), t);

        expect(Object.keys(errors)).toEqual(expect.arrayContaining(['department', 'title']));
    });

    it('requires a city for on-site and hybrid offers, but not remote offers', () => {
        expect(validateOfferForm(aValidForm({remotePolicy: 'ON_SITE', location: ''}), t))
            .toHaveProperty('location');
        expect(validateOfferForm(aValidForm({remotePolicy: 'HYBRID', location: ''}), t))
            .toHaveProperty('location');
        expect(validateOfferForm(aValidForm({remotePolicy: 'ON_SITE', location: 'RABAT'}), t))
            .not.toHaveProperty('location');
        expect(validateOfferForm(aValidForm({remotePolicy: 'REMOTE', location: ''}), t))
            .not.toHaveProperty('location');
    });

    it('rejects a title over 100 characters', () => {
        expect(validateOfferForm(aValidForm({title: 'x'.repeat(101)}), t)).toHaveProperty('title');
        expect(validateOfferForm(aValidForm({title: 'x'.repeat(100)}), t)).not.toHaveProperty('title');
    });

    it('requires a deadline, and requires it to be in the future', () => {
        expect(validateOfferForm(aValidForm({applyingDeadline: ''}), t)).toHaveProperty('applyingDeadline');
        expect(validateOfferForm(aValidForm({applyingDeadline: '2000-01-01T00:00'}), t))
            .toHaveProperty('applyingDeadline');
    });

    it('rejects a salary range that runs backwards', () => {
        expect(validateOfferForm(aValidForm({salaryMin: '30000', salaryMax: '20000'}), t))
            .toHaveProperty('salaryMax');
        expect(validateOfferForm(aValidForm({salaryMin: '20000', salaryMax: '30000'}), t))
            .not.toHaveProperty('salaryMax');
    });

    it('requires at least one open position', () => {
        expect(validateOfferForm(aValidForm({openPositions: '0'}), t)).toHaveProperty('openPositions');
        expect(validateOfferForm(aValidForm({openPositions: ''}), t)).toHaveProperty('openPositions');
        expect(validateOfferForm(aValidForm({openPositions: '1'}), t)).not.toHaveProperty('openPositions');
    });

    it('produces real messages rather than raw translation keys', () => {
        const errors = validateOfferForm(aValidForm({department: '', title: ''}), t);

        for (const message of Object.values(errors)) {
            expect(message).not.toMatch(/^offers\./);
        }
    });
});

describe('formToCreateInput', () => {
    it('sends unset optional fields as null rather than empty strings', () => {
        const input = formToCreateInput(aValidForm(), []);

        expect(input).toMatchObject({
            description: null,
            experienceLevel: null,
            location: null,
            salaryMin: null,
            salaryMax: null,
            salaryCurrency: null,
            salaryPeriod: null,
            minimumExperienceYears: null,
            plannedStartDate: null,
        });
    });

    it('trims the title and description', () => {
        const input = formToCreateInput(aValidForm({title: '  Backend Engineer  ', description: '  Build it.  '}), []);

        expect(input.title).toBe('Backend Engineer');
        expect(input.description).toBe('Build it.');
    });

    it('defaults open positions to 1 when the field cannot be parsed', () => {
        expect(formToCreateInput(aValidForm({openPositions: ''}), []).openPositions).toBe(1);
    });

    it('drops blank skill rows and trims the rest', () => {
        const input = formToCreateInput(aValidForm(), [
            {name: '  Java  ', importance: 'REQUIRED'},
            {name: '   ', importance: 'PREFERRED'},
            {name: 'Spring', importance: 'PREFERRED'},
        ]);

        expect(input.skills).toEqual([
            {name: 'Java', importance: 'REQUIRED'},
            {name: 'Spring', importance: 'PREFERRED'},
        ]);
    });
});

describe('formToEditInput', () => {
    // The read model cannot show these three, so a PATCH must not carry them:
    // sending them would overwrite stored values with form defaults.
    it('omits the fields the edit form cannot show', () => {
        const input = formToEditInput(aValidForm(), []);

        expect(input).not.toHaveProperty('minimumExperienceYears');
        expect(input).not.toHaveProperty('plannedStartDate');
        expect(input).not.toHaveProperty('salaryNegotiable');
    });
});

describe('offerToFormState', () => {
    it('seeds every visible field from the offer', () => {
        const offer = anOffer({
            title: 'Platform Engineer',
            description: 'Run the platform.',
            openPositions: 3,
            skills: [{name: 'Kubernetes', slug: 'kubernetes', importance: 'REQUIRED'}],
        });

        const {form, skills} = offerToFormState(offer);

        expect(form).toMatchObject({
            department: offer.department.name,
            title: 'Platform Engineer',
            description: 'Run the platform.',
            openPositions: '3',
            salaryMin: '20000',
            salaryMax: '30000',
        });
        expect(skills).toEqual([{name: 'Kubernetes', importance: 'REQUIRED'}]);
    });

    it('renders absent values as empty strings, not "null"', () => {
        const {form} = offerToFormState(anOffer({
            description: null,
            experienceLevel: null,
            location: null,
            salaryMin: null,
            salaryMax: null,
            salaryCurrency: null,
            salaryPeriod: null,
            applyingDeadline: null,
        }));

        expect(form).toMatchObject({
            description: '',
            experienceLevel: '',
            location: '',
            salaryMin: '',
            salaryMax: '',
            applyingDeadline: '',
        });
    });

    it('gives the form one blank skill row when the offer lists none', () => {
        const {skills} = offerToFormState(anOffer({skills: []}));

        expect(skills).toEqual([{name: '', importance: 'REQUIRED'}]);
    });

    it('survives a round trip back into an edit payload', () => {
        const offer = anOffer({title: 'Platform Engineer', openPositions: 2});
        const {form, skills} = offerToFormState(offer);

        expect(formToEditInput(form, skills)).toMatchObject({
            title: 'Platform Engineer',
            openPositions: 2,
            department: offer.department.name,
            salaryMin: 20000,
            salaryMax: 30000,
        });
    });
});
