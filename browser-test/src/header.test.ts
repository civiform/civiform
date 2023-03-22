import {
  createTestContext,
  loginAsGuest,
  loginAsTestUser,
  selectApplicantLanguage,
  validateScreenshot,
} from './support'

describe('Header', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  it('Not logged in, guest mode enabled', async () => {
    const {page} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await validateScreenshot(
      page.getByRole('navigation'),
      'not-logged-in-guest-mode-enabled',
    )
  })

  // TODO(#4360): add a "Not logged in, guest mode disabled" test once we
  // can get to the programs page without logging in, for an entity without
  // guest mode.

  it('Logged in', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')

    await validateScreenshot(page.getByRole('navigation'), 'logged-in')
  })
})
