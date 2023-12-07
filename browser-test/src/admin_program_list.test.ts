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

  it('publishing a single program shows a modal with conditional warning about universal questions', async () => {
    const {page, adminPrograms, adminQuestions} = ctx

    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'universal_questions')

    // Create a program and question that is not universal
    const programOne = 'program one'
    await adminPrograms.addProgram(programOne)
    const nameQuestion = 'name'
    await adminQuestions.addNameQuestion({
      questionName: nameQuestion,
      universal: false,
    })
    await adminPrograms.gotoEditDraftProgramPage(programOne)
    await adminPrograms.addQuestionFromQuestionBank(nameQuestion)

    await adminPrograms.gotoAdminProgramsPage()
    await page.click('#program-one-publish-modal-button')
    expect(await page.innerText('#program-one-publish-modal')).toContain(
      'Are you sure you want to publish program one and all of its draft questions?',
    )
    // Warning should not show because there are no universal questions
    expect(await page.innerText('#program-one-publish-modal')).not.toContain(
      'Warning: This program does not use all recommended universal questions.',
    )
    await validateScreenshot(page, 'publish-single-program-modal-no-warning')
    // Dismiss the modal
    await adminQuestions.clickSubmitButtonAndNavigate('Cancel')

    // Create a universal question
    const textQuestion = 'text'
    await adminQuestions.addTextQuestion({
      questionName: textQuestion,
      universal: true,
    })
    await adminPrograms.gotoAdminProgramsPage()
    await page.click('#program-one-publish-modal-button')
    // Warning should show because there is a universal question that is not used in this program
    expect(await page.innerText('#program-one-publish-modal')).toContain(
      'Warning: This program does not use all recommended universal questions.',
    )
    await validateScreenshot(page, 'publish-single-program-modal-with-warning')
    // Dismiss the modal
    await adminQuestions.clickSubmitButtonAndNavigate('Cancel')

    // Add the universal question to the program
    await adminPrograms.gotoEditDraftProgramPage(programOne)
    await adminPrograms.addQuestionFromQuestionBank(textQuestion)
    await adminPrograms.gotoAdminProgramsPage()
    await page.click('#program-one-publish-modal-button')
    // Warning should not show because the program uses all universal questions
    expect(await page.innerText('#program-one-publish-modal')).not.toContain(
      'Warning: This program does not use all recommended universal questions.',
    )
    await adminQuestions.clickSubmitButtonAndNavigate('Publish program')

    // Program was published.
    await adminPrograms.expectDoesNotHaveDraftProgram(programOne)
    await adminPrograms.expectActiveProgram(programOne)
  })
})
