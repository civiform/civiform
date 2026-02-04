import {test, expect} from './support/civiform_fixtures'
import {
  loginAsAdmin,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'

test.describe('Error pages', {tag: ['@parallel-candidate']}, () => {
  test('404 page', async ({page}) => {
    await test.step('404 page is shown', async () => {
      await page.goto('/bad/path/ezbezzdebashiboozook')
      await validateScreenshot(page, '404-page')
      await validateAccessibility(page)
    })

    await test.step('Has heading in English', async () => {
      await expect(
        page.getByRole('heading', {
          name: 'We were unable to find the page you tried to visit',
        }),
      ).toBeAttached()
    })

    await test.step('Change applicant language to Spanish', async () => {
      await page.goto('/')
      await selectApplicantLanguage(page, 'es-US')
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

    await test.step('Updating the support email address updates the error page', async () => {
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
  })

  test('disabled program page', async ({page, adminPrograms}) => {
    await test.step('Create and disable a program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addDisabledProgram('Test Program')
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('Disabled program page is shown', async () => {
      await page.goto('/programs/test-program/disabled')
      await validateScreenshot(page, 'disabled-program-page')
      await validateAccessibility(page)
    })

    await test.step('Has heading in English', async () => {
      await page.goto('/programs/test-program/disabled')
      await expect(
        page.getByRole('heading', {
          name: 'This program is no longer available',
        }),
      ).toBeAttached()
    })

    await test.step('Home button takes you to the homepage', async () => {
      await page
        .getByRole('link', {
          name: 'Back to homepage',
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

    await test.step('Change applicant language to Spanish', async () => {
      await page.goto('/')
      await selectApplicantLanguage(page, 'es-US')
    })

    await test.step('Has heading in Spanish', async () => {
      await page.goto('/programs/test-program/disabled')
      await expect(
        page.getByRole('heading', {
          name: 'Este programa ya no está disponible',
        }),
      ).toBeAttached()
    })
  })
})
