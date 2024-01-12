import {
  AdminPrograms,
  AdminQuestions,
  dismissModal,
  dropTables,
  gotoEndpoint,
  loginAsAdmin,
  startSession,
  validateScreenshot,
  createTestContext,
} from './support'
import {Page} from 'playwright'
import {ProgramVisibility} from './support/admin_programs'

describe('publishing all draft questions and programs', () => {
  const ctx = createTestContext()
  let pageObject: Page
  let adminPrograms: AdminPrograms
  let adminQuestions: AdminQuestions

  const hiddenProgramNoQuestions = 'Public test program hidden no questions'
  const visibleProgramWithQuestion = 'Public test program visible with question'
  const questionName = 'publish-test-address-q'
  const questionText = 'publish-test-address-q'
  // CreateNewVersion implicitly updates the question text to be suffixed with " new version".
  const draftQuestionText = `${questionText} new version`

  beforeAll(async () => {
    const session = await startSession()
    pageObject = session.page

    await dropTables(pageObject)
    await gotoEndpoint(pageObject)

    adminPrograms = new AdminPrograms(pageObject)
    adminQuestions = new AdminQuestions(pageObject)

    await loginAsAdmin(pageObject)

    // Create a hidden program with no questions
    await adminPrograms.addProgram(
      hiddenProgramNoQuestions,
      'program description',
      'https://usa.gov',
      ProgramVisibility.HIDDEN,
    )

    // Create a new question referenced by a program.
    await adminQuestions.addAddressQuestion({questionName, questionText})
    await adminPrograms.addProgram(visibleProgramWithQuestion)
    await adminPrograms.editProgramBlock(
      visibleProgramWithQuestion,
      'dummy description',
      [questionName],
    )

    // Publish.
    await adminPrograms.publishAllDrafts()

    // Make an edit to the program with no questions.
    await adminPrograms.createNewVersion(hiddenProgramNoQuestions)

    // Make an edit to the shared question.
    await adminQuestions.createNewVersion(questionName)

    await adminPrograms.gotoAdminProgramsPage()
  })

  it('shows programs and questions that will be published in the modal', async () => {
    await adminPrograms.expectProgramReferencesModalContains({
      expectedQuestionsContents: [`${draftQuestionText} - Edit`],
      expectedProgramsContents: [
        `${hiddenProgramNoQuestions} (Hidden from applicants) Edit`,
        `${visibleProgramWithQuestion} (Publicly visible) Edit`,
      ],
    })
  })

  it('validate screenshot', async () => {
    await adminPrograms.openPublishAllDraftsModal()
    await validateScreenshot(
      adminPrograms.publishAllProgramsModalLocator(),
      'publish-modal',
    )
    await dismissModal(pageObject)
  })

  it('publishing all programs with universal questions feature flag on shows a modal with information about universal questions', async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)
    // Create programs and questions (including universal questions)
    const programOne = 'program one'
    await adminPrograms.addProgram(programOne)
    const programTwo = 'program two'
    await adminPrograms.addProgram(programTwo)
    const nameQuestion = 'name'
    await adminQuestions.addNameQuestion({
      questionName: nameQuestion,
      universal: true,
    })
    const textQuestion = 'text'
    await adminQuestions.addTextQuestion({
      questionName: textQuestion,
      universal: true,
    })
    const addressQuestion = 'address'
    await adminQuestions.addAddressQuestion({
      questionName: addressQuestion,
      universal: false,
    })
    // Add questions to programs
    await adminPrograms.gotoEditDraftProgramPage(programOne)
    await adminPrograms.addQuestionFromQuestionBank(nameQuestion)
    await adminPrograms.addQuestionFromQuestionBank(textQuestion)
    await adminPrograms.gotoEditDraftProgramPage(programTwo)
    await adminPrograms.addQuestionFromQuestionBank(nameQuestion)
    await adminPrograms.addQuestionFromQuestionBank(addressQuestion)
    // Trigger the modal
    await adminPrograms.gotoAdminProgramsPage()
    await page.click('#publish-all-programs-modal-button')
    expect(await page.innerText('#publish-all-programs-modal')).toContain(
      'program one (Publicly visible) - Contains all universal questions',
    )
    expect(await page.innerText('#publish-all-programs-modal')).toContain(
      'program two (Publicly visible) - Contains 1 of 2 universal questions',
    )
    await validateScreenshot(page, 'publish-all-programs-modal-with-uq')
    // Publish the programs
    await adminQuestions.clickSubmitButtonAndNavigate(
      'Publish all draft programs and questions',
    )
    await adminPrograms.expectDoesNotHaveDraftProgram(programOne)
    await adminPrograms.expectDoesNotHaveDraftProgram(programTwo)
    await adminPrograms.expectActiveProgram(programOne)
    await adminPrograms.expectActiveProgram(programTwo)
  })
})
