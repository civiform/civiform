import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  selectApplicantLanguageNorthstar,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'

test.describe(
  'Error pages',
  {tag: ['@parallel-candidate', '@northstar']},
  () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('404 page', async ({page}) => {
      await test.step('Has heading in English', async () => {
        await page.goto('/bad/path/ezbezzdebashiboozook')
        await expect(
          page.getByRole('heading', {
            name: 'We were unable to find the page you tried to visit',
          }),
        ).toBeAttached()
      })

      await test.step('Change applicant language to Spanish', async () => {
        await page.goto('/')
        await selectApplicantLanguageNorthstar(page, 'es-US')
      })

      await test.step('Has heading in Spanish', async () => {
        await page.goto('/bad/path/ezbezzdebashiboozook')
        await expect(
          page.getByRole('heading', {
            name: 'No Pudimos encontrar la página que intentó visitar',
          }),
        ).toBeAttached()
      })
    })

    test('server error page', async ({page, adminSettings}) => {
      await test.step('Server error page is shown', async () => {
        await page.goto('/error?exceptionId=1')
        await validateScreenshot(page, 'server-error-page')
        await validateAccessibility(page)
      })

      await test.step('Home button takes you to the homepage', async () => {
        await page
          .getByRole('link', {
            name: 'Visit homepage',
          })
          .click()
        await waitForPageJsLoad(page)
        await expect(page).toHaveURL(/.*programs/)
        await expect(
          page.getByRole('heading', {
            name: 'Apply for government programs online',
          }),
        ).toBeAttached()
      })

      await test.step('Updating the support email address updates the error page when IT email is unset', async () => {
        await loginAsAdmin(page)
        await adminSettings.gotoAdminSettings()
        await adminSettings.setStringSetting(
          'SUPPORT_EMAIL_ADDRESS',
          'support@email.com',
        )
        await adminSettings.saveChanges()
        await page.goto('/error?exceptionId=1')
        await expect(
          page.getByRole('link', {
            name: 'support@email.com',
          }),
        ).toBeAttached()
      })

      await test.step('Updating the IT email address updates the error page when both are set', async () => {
        await adminSettings.gotoAdminSettings()
        await adminSettings.setStringSetting('IT_EMAIL_ADDRESS', 'it@email.com')
        await adminSettings.saveChanges()
        await page.goto('/error?exceptionId=1')
        await expect(
          page.getByRole('link', {
            name: 'it@email.com',
          }),
        ).toBeAttached()
      })

      await test.step('Removing the IT email address updates the error page with fallback to support', async () => {
        await adminSettings.gotoAdminSettings()
        await adminSettings.setStringSetting('IT_EMAIL_ADDRESS', '')
        await adminSettings.saveChanges()
        await page.goto('/error?exceptionId=1')
        await expect(
          page.getByRole('link', {
            name: 'support@email.com',
          }),
        ).toBeAttached()
      })
    })
  },
)
