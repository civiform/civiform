import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {ProgramVisibility} from './support/admin_programs'

test.describe('Viewing API docs', () => {
  test.beforeEach(async ({seeding}) => {
    await seeding.seedProgramsAndCategories()
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

    await page.getByRole('button', {name: 'API'}).click()
    await page.getByRole('link', {name: 'Documentation'}).click()

    await waitForPageJsLoad(page)

    await test.step('Verify default comprehensive sample program', async () => {
      await expect(
        page.getByRole('complementary').getByRole('code'),
      ).toContainText('"program_name" : "comprehensive-sample-program"')

      await validateScreenshot(
        page,
        'comprehensive-program-active-version-with-multiple-upload',
      )
    })

    await test.step('Select a different program and verify minimal sample program', async () => {
      await page
        .getByRole('combobox', {name: 'Select a program'})
        .selectOption('minimal-sample-program')

      await waitForPageJsLoad(page)

      await expect(
        page.getByRole('complementary').getByRole('code'),
      ).toContainText('"program_name" : "minimal-sample-program"')

      await validateScreenshot(
        page,
        'minimal-program-active-version-with-multiple-upload',
      )
    })
  })

  test('Views draft API docs when available', async ({page}) => {
    await page.goto('/')
    await loginAsAdmin(page)

    await page.getByRole('button', {name: 'API'}).click()
    await page.getByRole('link', {name: 'Documentation'}).click()

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

    await page.getByRole('button', {name: 'API'}).click()
    await page.getByRole('link', {name: 'Documentation'}).click()

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

    await page.getByRole('button', {name: 'API'}).click()
    await page.getByRole('link', {name: 'Documentation'}).click()

    await waitForPageJsLoad(page)

    // Opening the accordion
    await page.getByRole('button', {name: 'How does this work?'}).click()

    await validateScreenshot(
      page.locator('.cf-accordion'),
      'api-docs-page-accordion-open',
    )
  })

  test('External programs are not shown in program options', async ({
    page,
    adminPrograms,
  }) => {
    await page.goto('/')
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'external_program_cards_enabled')

    await adminPrograms.addExternalProgram(
      'External Program Name',
      'short program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
    )

    await page.getByRole('button', {name: 'API'}).click()
    await page.getByRole('link', {name: 'Documentation'}).click()

    await waitForPageJsLoad(page)

    await expect(
      page.getByRole('combobox', {name: 'Program'}),
    ).not.toContainText('external-program-name')
  })
})
