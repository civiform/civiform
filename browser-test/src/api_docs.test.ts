import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  seedProgramsAndCategories,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'

test.describe('Viewing API docs', () => {
  test.beforeEach(async ({page}) => {
    await seedProgramsAndCategories(page)
    await enableFeatureFlag(page, 'api_generated_docs_enabled')
  })

  test('Views active API docs', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await page.goto('/')
    await loginAsAdmin(page)

    await test.step('Add additional option to checkbox to ensure all historical options are shown', async () => {
      await adminPrograms.publishAllDrafts()
      await adminQuestions.gotoQuestionEditPage('Sample Checkbox Question')
      await adminQuestions.deleteMultiOptionAnswer(0)

      await adminQuestions.addMultiOptionAnswer({
        adminName: 'spirograph',
        text: 'spirograph',
      })

      await adminQuestions.clickSubmitButtonAndNavigate('Update')
      await adminPrograms.publishAllDrafts()
    })

    await page.getByRole('link', {name: 'API docs'}).click()
    await waitForPageJsLoad(page)

    await test.step('Verify default comprehensive sample program', async () => {
      await expect(
        page.getByRole('complementary').getByRole('code'),
      ).toContainText('"program_name" : "comprehensive-sample-program"')

      await validateScreenshot(page, 'comprehensive-program-active-version')
    })

    await test.step('Select a different program and verify minimal sample program', async () => {
      await page
        .getByRole('combobox', {name: 'Select a program'})
        .selectOption('minimal-sample-program')

      await waitForPageJsLoad(page)

      await expect(
        page.getByRole('complementary').getByRole('code'),
      ).toContainText('"program_name" : "minimal-sample-program"')

      await validateScreenshot(page, 'minimal-program-active-version')
    })
  })

  test('Views active API docs without logging in', async ({
    page,
    adminPrograms,
    context,
  }) => {
    await page.goto('/')
    await loginAsAdmin(page)

    await adminPrograms.publishAllDrafts()

    const freshPage =
      await test.step('Log out and clear cookies before accessing API docs', async () => {
        const apiDocsUrl = await page.getByText('API docs').getAttribute('href')

        await logout(page)
        await context.clearCookies()
        const freshPage = await context.newPage()
        await freshPage.goto(apiDocsUrl!)
        await waitForPageJsLoad(freshPage)
        return freshPage
      })

    await test.step('Verify default comprehensive sample program', async () => {
      await expect(
        freshPage.getByRole('complementary').getByRole('code'),
      ).toContainText('"program_name" : "comprehensive-sample-program"')

      await validateScreenshot(
        freshPage,
        'comprehensive-program-active-version-logged-out',
      )
    })

    await test.step('Select a different program and verify minimal sample program', async () => {
      await freshPage.selectOption('#select-slug', {
        value: 'minimal-sample-program',
      })

      await waitForPageJsLoad(freshPage)

      expect(await freshPage.textContent('html')).toContain(
        '"program_name" : "minimal-sample-program"',
      )
      await validateScreenshot(
        freshPage,
        'minimal-program-active-version-logged-out',
      )
    })
  })

  test('Views draft API docs when available', async ({page}) => {
    await page.goto('/')
    await loginAsAdmin(page)

    await page.getByRole('link', {name: 'API docs'}).click()
    await waitForPageJsLoad(page)

    await test.step('Select a different program and verify minimal sample program', async () => {
      await page
        .getByRole('combobox', {name: 'Select a program'})
        .selectOption('minimal-sample-program')

      await waitForPageJsLoad(page)

      await page
        .getByRole('combobox', {name: 'Select version'})
        .selectOption('draft')

      await waitForPageJsLoad(page)

      await expect(
        page.getByRole('complementary').getByRole('code'),
      ).toContainText('"program_name" : "minimal-sample-program"')

      await validateScreenshot(page, 'draft-available')
    })
  })

  test('Shows error on draft API docs when no draft available', async ({
    page,
    adminPrograms,
  }) => {
    await page.goto('/')
    await loginAsAdmin(page)

    await adminPrograms.publishAllDrafts()

    await page.getByRole('link', {name: 'API docs'}).click()
    await waitForPageJsLoad(page)

    await test.step('Select a different program and verify minimal sample program', async () => {
      await page
        .getByRole('combobox', {name: 'Select a program'})
        .selectOption('minimal-sample-program')

      await waitForPageJsLoad(page)

      await page
        .getByRole('combobox', {name: 'Select version'})
        .selectOption('draft')

      await waitForPageJsLoad(page)

      await expect(
        page.getByRole('heading', {name: 'Program and version not found'}),
      ).toBeAttached()

      await validateScreenshot(page, 'draft-not-available')
    })
  })

  test('Opens help accordion with a click', async ({page, adminPrograms}) => {
    await page.goto('/')
    await loginAsAdmin(page)
    await adminPrograms.publishAllDrafts()

    await page.getByRole('link', {name: 'API docs'}).click()

    await waitForPageJsLoad(page)

    // Opening the accordion
    await page.getByRole('button', {name: 'How does this work?'}).click()

    await validateScreenshot(
      page.locator('.cf-accordion'),
      'api-docs-page-accordion-open',
    )
  })
})
