import {test, expect} from '@playwright/test'
import {
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {ProgramVisibility} from './support/admin_programs'

test.describe('program settings', () => {
  const ctx = createTestContext()

  test('program settings page', async () => {
    const {page, adminPrograms} = ctx

    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsAdmin(page)

    const programName = 'A Program'
    await adminPrograms.addProgram(programName)

    await adminPrograms.gotoProgramSettingsPage(programName)

    await validateScreenshot(page, 'gating-eligibility')

    const responsePromise = page.waitForResponse((response) => {
      return response.url().includes('settings/edit')
    })
    await page.click('#eligibility-toggle')
    await responsePromise

    // Get the mouse out of the way so that it's not hovering over the toggle during the screenshot.
    await page.mouse.move(0, 0)
    await validateScreenshot(page, 'nongating-eligibility')
  })

  test('program index shows settings in dropdown', async () => {
    const {page, adminPrograms} = ctx

    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsAdmin(page)

    const programName = 'A Program'
    await adminPrograms.addProgram(programName)

    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.expectDraftProgram(programName)

    await page.click(
      adminPrograms.withinProgramCardSelector(
        programName,
        'Draft',
        '.cf-with-dropdown',
      ),
    )

    await validateScreenshot(page, 'dropdown-with-settings')

    expect(
      await page
        .locator(
          adminPrograms.withinProgramCardSelector(
            programName,
            'Draft',
            ':text("Settings")',
          ),
        )
        .isVisible(),
    ).toBe(true)
  })

  test('back button on program settings page navigates correctly', async () => {
    const {page, adminPrograms} = ctx

    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsAdmin(page)

    const programName = 'A Program'
    await adminPrograms.addProgram(programName)

    await adminPrograms.gotoAdminProgramsPage()

    await adminPrograms.gotoProgramSettingsPage(programName)

    await page.click(`a:has-text("Back")`)
    await waitForPageJsLoad(page)
    await adminPrograms.expectAdminProgramsPage()

    await adminPrograms.gotoEditDraftProgramPage(programName)
    await page.click(`a:has-text("program settings")`)
    await waitForPageJsLoad(page)
    await adminPrograms.expectProgramSettingsPage()

    await page.click(`a:has-text("Back")`)
    await waitForPageJsLoad(page)
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  test('program index hides settings in dropdown for common intake form', async () => {
    const {page, adminPrograms} = ctx

    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsAdmin(page)

    const programName = 'Common Intake Form'
    await adminPrograms.addProgram(
      programName,
      'description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ true,
    )

    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.expectDraftProgram(programName)

    await page.click(
      adminPrograms.withinProgramCardSelector(
        programName,
        'Draft',
        '.cf-with-dropdown',
      ),
    )

    expect(
      await page
        .locator(
          adminPrograms.withinProgramCardSelector(
            programName,
            'Draft',
            ':text("Settings")',
          ),
        )
        .isVisible(),
    ).toBe(false)
  })
})
