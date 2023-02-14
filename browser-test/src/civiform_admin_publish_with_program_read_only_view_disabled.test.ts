import {
  AdminPrograms,
  AdminQuestions,
  dismissModal,
  disableFeatureFlag,
  loginAsAdmin,
  startSession,
  validateScreenshot,
} from './support'
import {Page} from 'playwright'

// TODO(#4125) This test suite is a duplicate of civiform_admin_publish.test.ts should be removed once the program_read_only_view
// flag has been removed. If you make changes to this file, please consider updating the other test too.
describe('publishing all draft questions and programs with program read only view disabled', () => {
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
    adminPrograms = new AdminPrograms(pageObject)
    adminQuestions = new AdminQuestions(pageObject)

    await loginAsAdmin(pageObject)
    await disableFeatureFlag(pageObject, 'program_read_only_view_enabled')

    // Create a hidden program with no questions
    await adminPrograms.addProgram(
      hiddenProgramNoQuestions,
      'program description',
      '',
      true,
    )

    // Create a new question refererenced by a program.
    await adminQuestions.addAddressQuestion({questionName, questionText})
    await adminPrograms.addProgram(visibleProgramWithQuestion)
    await adminPrograms.editProgramBlock(
      visibleProgramWithQuestion,
      'dummy description',
      [questionName],
    )

    // Publish.
    await adminPrograms.publishAllPrograms()

    // Make an edit to the program with no questions.
    await adminPrograms.createNewVersion(
      hiddenProgramNoQuestions,
      /* programReadOnlyViewEnabled = */ false,
    )

    // Make an edit to the shared question.
    await adminQuestions.createNewVersion(questionName)

    await adminPrograms.gotoAdminProgramsPage()
  })

  it('shows programs and questions that will be published in the modal', async () => {
    await adminPrograms.expectProgramReferencesModalContains({
      expectedQuestionsContents: [`${draftQuestionText} - Edit`],
      expectedProgramsContents: [
        `${hiddenProgramNoQuestions} - Hidden from applicants Edit`,
        `${visibleProgramWithQuestion} - Publicly visible Edit`,
      ],
    })
  })

  it('validate screenshot', async () => {
    await adminPrograms.openPublishAllProgramsModal()
    await validateScreenshot(
      adminPrograms.publishAllProgramsModalLocator(),
      'publish-modal',
    )
    await dismissModal(pageObject)
  })
})
