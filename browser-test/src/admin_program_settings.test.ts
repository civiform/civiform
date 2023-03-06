import {
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'

describe('program settings', () => {
  const ctx = createTestContext()

  it('program settings page', async () => {
    const {page, adminPrograms} = ctx

    await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')
    await enableFeatureFlag(page, 'nongated_eligibility_enabled')

    await loginAsAdmin(page)

    const programName = 'A Program'
    await adminPrograms.addProgram(programName)

    await adminPrograms.gotoProgramSettingsPage(programName)

    await validateScreenshot(page, 'gating-eligibility')

    await page.click('#eligibility-toggle')
    await validateScreenshot(page, 'nongating-eligibility')
  })

  it('program index shows settings in dropdown', async () => {
    const {page, adminPrograms} = ctx

    await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')
    await enableFeatureFlag(page, 'nongated_eligibility_enabled')

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
  })

  it('program index hides settings in dropdown when flags are disabled', async () => {
    const {page, adminPrograms} = ctx

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

    await validateScreenshot(page, 'dropdown-without-settings')
  })
})
