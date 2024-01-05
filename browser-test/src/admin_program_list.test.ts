import {
  AdminPrograms,
  createTestContext,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'
import {ProgramVisibility} from './support/admin_programs'

describe('Program list page.', () => {
  const ctx = createTestContext()

  it('view draft program', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoAdminProgramsPage()
    await validateScreenshot(page, 'program-list-one-draft-program')
  })

  it('view active program', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()
    await validateScreenshot(page, 'program-list-one-active-program')
  })

  it('view program with active and draft versions', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()

    await adminPrograms.createNewVersion(programName)
    await adminPrograms.gotoAdminProgramsPage()
    await validateScreenshot(page, 'program-list-active-and-draft-versions')
  })

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

  it('shows information about universal questions when the flag is enabled and at least one universal question is set', async () => {
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
    expect(await page.innerText('.cf-admin-program-card')).not.toContain(
      'universal questions',
    )

    await validateScreenshot(
      page,
      'program-list-view-no-universal-questions-text',
    )

    // Create a universal question
    const textQuestion = 'text'
    await adminQuestions.addTextQuestion({
      questionName: textQuestion,
      universal: true,
    })
    await adminPrograms.gotoAdminProgramsPage()
    expect(await page.innerText('.cf-admin-program-card')).toContain(
      'universal questions',
    )
    await validateScreenshot(
      page,
      'program-list-view-with-universal-questions-text',
    )
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

    const publishProgramOneModalButton = '#publish-modal-program-one-button'
    const publishProgramOneModal = '#publish-modal-program-one'

    await adminPrograms.gotoAdminProgramsPage()
    await page.click(publishProgramOneModalButton)
    expect(await page.innerText(publishProgramOneModal)).toContain(
      'Are you sure you want to publish program one and all of its draft questions?',
    )
    // Warning should not show because there are no universal questions
    expect(await page.innerText(publishProgramOneModal)).not.toContain(
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
    await page.click(publishProgramOneModalButton)
    // Warning should show because there is a universal question that is not used in this program
    expect(await page.innerText(publishProgramOneModal)).toContain(
      'Warning: This program does not use all recommended universal questions.',
    )
    await validateScreenshot(page, 'publish-single-program-modal-with-warning')
    // Dismiss the modal
    await adminQuestions.clickSubmitButtonAndNavigate('Cancel')

    // Add the universal question to the program
    await adminPrograms.gotoEditDraftProgramPage(programOne)
    await adminPrograms.addQuestionFromQuestionBank(textQuestion)
    await adminPrograms.gotoAdminProgramsPage()
    await page.click(publishProgramOneModalButton)
    // Warning should not show because the program uses all universal questions
    expect(await page.innerText(publishProgramOneModal)).not.toContain(
      'Warning: This program does not use all recommended universal questions.',
    )
    await adminQuestions.clickSubmitButtonAndNavigate('Publish program')

    // Program was published.
    await adminPrograms.expectDoesNotHaveDraftProgram(programOne)
    await adminPrograms.expectActiveProgram(programOne)
  })

  it('program list has current image if images flag on', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Images Flag On Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()

    await validateScreenshot(page, 'program-list-with-image-flag-on')
  })

  it('program list does not show current image if images flag off', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    // Enable the flag to set a program image
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Images Flag Off Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()

    // Disable the flag then check the program list page
    await disableFeatureFlag(page, 'program_card_images')
    await adminPrograms.gotoAdminProgramsPage()

    await validateScreenshot(page, 'program-list-with-image-flag-off')
  })

  it('program list with no image', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'No Image Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()

    await validateScreenshot(page, 'program-list-no-image')
  })

  it('program list with new image in draft', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    // Start the program as having no image
    const programName = 'New Image Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()

    // Set a new image on the new draft program
    await adminPrograms.createNewVersion(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-tall.png',
    )
    await adminPrograms.gotoAdminProgramsPage()

    // Verify that the new image is shown in the Draft row
    // and a gray placeholder image icon is shown in the Active row.
    await validateScreenshot(page, 'program-list-with-new-draft-image')
  })

  it('program list with different active and draft image', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Different Images Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()

    // Set a new image on the new draft program
    await adminPrograms.createNewVersion(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-tall.png',
    )
    await adminPrograms.gotoAdminProgramsPage()

    await validateScreenshot(
      page,
      'program-list-with-different-active-and-draft-images',
    )
  })

  it('program list with same active and draft image', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Same Image Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()

    // Create a new draft version of the program, but don't edit the image
    await adminPrograms.createNewVersion(programName)
    await adminPrograms.gotoAdminProgramsPage()

    // Verify that the current image is shown twice, in both the Active row and Draft row
    await validateScreenshot(
      page,
      'program-list-with-same-active-and-draft-image',
    )
  })
})
