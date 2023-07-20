import {
  AdminPrograms,
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'
import {ProgramVisibility} from './support/admin_programs'

describe('Program list page.', () => {
  const ctx = createTestContext()
  it('sorts by last updated, preferring draft over active', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)

    const programOne = 'List test program one'
    const programTwo = 'List test program two'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.addProgram(programTwo)

    // Most recently added program is on top.
    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    // Publish all programs, order should be maintained.
    await adminPrograms.publishAllDrafts()
    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    // Now create a draft version of the previously last program. After,
    // it should be on top.
    await adminPrograms.createNewVersion(programOne)
    await expectProgramListElements(adminPrograms, [programOne, programTwo])

    // Now create a new program, which should be on top.
    const programThree = 'List test program three'
    await adminPrograms.addProgram(programThree)
    await expectProgramListElements(adminPrograms, [
      programThree,
      programOne,
      programTwo,
    ])
  })

  it('shows which program is the common intake when enabled', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'intake_form_enabled')

    const programOne = 'List test program one'
    const programTwo = 'List test program two'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.addProgram(
      programTwo,
      'program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ true,
    )

    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    await validateScreenshot(page, 'intake-form-indicator')
  })

  async function expectProgramListElements(
    adminPrograms: AdminPrograms,
    expectedPrograms: string[],
  ) {
    if (expectedPrograms.length === 0) {
      throw new Error('expected at least one program')
    }
    const programListNames = await adminPrograms.programNames()
    expect(programListNames).toEqual(expectedPrograms)
  }

  it('publishes a single program', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)

    const programOne = 'List test program one'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.createNewVersion(programOne)
    await adminPrograms.expectDraftProgram(programOne)

    // Add listener to dismiss dialog after clicking 'Publish program'.
    page.once('dialog', (dialog) => {
      void dialog.dismiss()
      expect(dialog.type()).toEqual('confirm')
      expect(dialog.message()).toEqual(
        'Are you sure you want to publish List test program one and all of its draft questions?',
      )
    })

    await page.click('#publish-program-button')

    // Draft not published because dialog was dismissed.
    await adminPrograms.expectDraftProgram(programOne)

    // Add listener to confirm dialog after clicking 'Publish program'.
    page.once('dialog', (dialog) => {
      void dialog.accept()
    })

    await page.click('#publish-program-button')

    // Program was published.
    await adminPrograms.expectDoesNotHaveDraftProgram(programOne)
    await adminPrograms.expectActiveProgram(programOne)
  })
})
