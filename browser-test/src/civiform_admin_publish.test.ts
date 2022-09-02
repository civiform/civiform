import {
  dismissModal,
  startSession,
  loginAsAdmin,
  validateScreenshot,
  AdminPrograms,
  AdminQuestions,
} from './support'
import {Page} from 'playwright'

describe('publishing all draft questions and programs', () => {
  let pageObject: Page
  let adminPrograms: AdminPrograms
  let adminQuestions: AdminQuestions

  const programNoQuestions = 'publish-test-program-no-questions'
  const programWithQuestion = 'publish-test-program-with-question'
  const questionName = 'publish-test-address-q'

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    adminPrograms = new AdminPrograms(pageObject)
    adminQuestions = new AdminQuestions(pageObject)

    await loginAsAdmin(pageObject)

    // Create a program with no questions
    await adminPrograms.addProgram(programNoQuestions)

    // Create a new question refererenced by a program.
    await adminQuestions.addAddressQuestion({questionName: questionName})
    await adminPrograms.addProgram(programWithQuestion)
    await adminPrograms.editProgramBlock(
      programWithQuestion,
      'dummy description',
      [questionName],
    )

    // Publish.
    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.publishAllPrograms()

    // Make an edit to the program with no questions.
    await adminPrograms.createNewVersion(programNoQuestions)

    // Make an edit to the shared question.
    await adminQuestions.createNewVersion(questionName)

    await adminPrograms.gotoAdminProgramsPage()
  })

  it('shows programs and questions that will be published in the modal', async () => {
    await adminPrograms.expectProgramReferencesModalContains({
      expectedQuestionNames: [questionName],
      expectedProgramNames: [programNoQuestions, programWithQuestion],
    })
  })

  it('validate screenshot', async () => {
    await adminPrograms.openPublishAllProgramsModal()
    await validateScreenshot(adminPrograms.publishAllLocator(), 'publish-modal')
    await dismissModal(pageObject)
  })
})
