import {test, expect} from './support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from './support'
import {ProgramVisibility} from './support/admin_programs'
import {ApiDocsPage} from './page/admin/docs/api_docs_page'

test.describe('Viewing API docs', () => {
  test.beforeEach(async ({page, seeding}) => {
    await enableFeatureFlag(page, 'ADMIN_UI_MIGRATION_SC_ENABLED')
    await seeding.seedProgramsAndCategories()
    await page.goto('/')
    await loginAsAdmin(page)
  })

  test('Views active API docs', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    const apiDocsPage = new ApiDocsPage(page)

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

    await apiDocsPage.gotoViaNav()

    await test.step('Verify default comprehensive sample program', async () => {
      await expect(apiDocsPage.getPageHeading()).toBeVisible()
      await expect(apiDocsPage.getJsonPreview()).toContainText(
        '"program_name" : "comprehensive-sample-program"',
      )

      await validateScreenshot(
        page,
        'comprehensive-program-active-version-with-multiple-upload',
      )
    })

    await test.step('Select a different program and verify minimal sample program', async () => {
      await apiDocsPage.selectProgram('minimal-sample-program')

      await expect(apiDocsPage.getJsonPreview()).toContainText(
        '"program_name" : "minimal-sample-program"',
      )
    })
  })

  test('Views draft API docs when available', async ({page}) => {
    const apiDocsPage = new ApiDocsPage(page)

    await apiDocsPage.gotoViaNav()

    await test.step('Select a different program and verify minimal sample program', async () => {
      await apiDocsPage.selectProgram('minimal-sample-program')
      await apiDocsPage.selectVersion('draft')

      await expect(apiDocsPage.getJsonPreview()).toContainText(
        '"program_name" : "minimal-sample-program"',
      )
    })
  })

  test('Shows error on draft API docs when no draft available', async ({
    page,
    adminPrograms,
  }) => {
    const apiDocsPage = new ApiDocsPage(page)
    await adminPrograms.publishAllDrafts()

    await apiDocsPage.gotoViaNav()

    await test.step('Select a different program with no draft version', async () => {
      await apiDocsPage.selectProgram('minimal-sample-program')
      await apiDocsPage.selectVersion('draft')

      await expect(apiDocsPage.getNotFoundMessage()).toBeVisible()
    })
  })

  test('Opens help accordion with a click', async ({page, adminPrograms}) => {
    const apiDocsPage = new ApiDocsPage(page)
    await adminPrograms.publishAllDrafts()

    await apiDocsPage.gotoViaNav()

    await expect(apiDocsPage.getAccordionButton()).toHaveAttribute(
      'aria-expanded',
      'false',
    )

    await apiDocsPage.clickAccordion()

    await expect(apiDocsPage.getAccordionButton()).toHaveAttribute(
      'aria-expanded',
      'true',
    )
  })

  test('External programs are not shown in program options', async ({
    page,
    adminPrograms,
  }) => {
    const apiDocsPage = new ApiDocsPage(page)

    await adminPrograms.addExternalProgram(
      'External Program Name',
      'short program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
    )

    await apiDocsPage.gotoViaNav()

    await expect(apiDocsPage.getProgramSelect()).not.toContainText(
      'external-program-name',
    )
  })
})
