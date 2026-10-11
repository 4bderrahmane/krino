import {expect, it} from 'vitest';
import {fireEvent, screen} from '@testing-library/react';
import {http, HttpResponse} from 'msw';
import i18n from '@/utils/i18n';
import ProfileSettings from './ProfileSettings.tsx';
import {apiPath, aUser, server, signedInSession} from '@/test/msw/server';
import {renderWithProviders} from '@/test/renderWithProviders';

const t = i18n.getFixedT('en');

it('submits the version loaded with the form and explains a stale-form conflict', async () => {
    const user = aUser(['CANDIDATE'], {version: 4});
    const sent: unknown[] = [];
    server.use(
        ...signedInSession(user),
        http.patch(apiPath('/users/me'), async ({request}) => {
            sent.push(await request.json());
            return HttpResponse.json({errorCode: 'STALE_PROFILE'}, {status: 409});
        }),
    );
    renderWithProviders(<ProfileSettings/>);

    const firstName = await screen.findByLabelText(t('profile.firstName'));
    fireEvent.change(firstName, {target: {value: 'Changed'}});
    fireEvent.click(screen.getByRole('button', {name: t('profile.saveChanges')}));

    expect(await screen.findByText(t('profile.staleFormError'))).toBeInTheDocument();
    expect(sent).toEqual([{firstName: 'Changed', version: 4}]);
    expect(firstName).toHaveValue('Changed');
});
