import {test, expect} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('footer', () => {
  test('renders footer', async ({page}) => {
    await validateScreenshot(page.locator('footer'), 'footer')
    await validateAccessibility(page)
  })

  test('Updating the support email updated the footer', async ({
    page,
    adminSettings,
  }) => {
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()
    await adminSettings.setStringSetting(
      'SUPPORT_EMAIL_ADDRESS',
      'test@email.com',
    )
    await adminSettings.saveChanges()
    await logout(page)

    await expect(
      page.getByRole('link', {
        name: 'test@email.com',
      }),
    ).toBeAttached()
  })
})
