import { AdminPrograms, AdminQuestions, loginAsAdmin, startSession, validateScreenshot, } from './support'
import { Page } from 'playwright'

describe('view program references from question view', () => {
  let pageObject: Page
  let adminPrograms: AdminPrograms
  let adminQuestions: AdminQuestions

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    adminPrograms = new AdminPrograms(pageObject)
    adminQuestions = new AdminQuestions(pageObject)

    await loginAsAdmin(pageObject)
  })

  it('shows no results for an unreferenced question', async () => {
    const questionName = 'unreferenced-q'
    await adminQuestions.addAddressQuestion({questionName})
    await adminQuestions.expectQuestionProgramReferencesText({
      questionName,
      expectedProgramReferencesText: 'Used across 0 active & 0 draft programs',
    })
    await validateScreenshot(pageObject);
  })

  it('shows results for referencing programs', async () => {
    const questionName = 'question-references-q'
    await adminQuestions.addAddressQuestion({questionName})

    // Add a reference to the question in the second block. We'll later assert
    // that the links in the modal takes us to the correct block.
    const firstProgramName = 'first-program'
    await adminPrograms.addProgram(firstProgramName)
    await adminPrograms.addProgramBlock(firstProgramName, 'first block', [])
    await adminPrograms.addProgramBlock(firstProgramName, 'second block', [
      questionName,
    ])

    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.expectQuestionProgramReferencesText({
      questionName,
      expectedProgramReferencesText: 'Used across 0 active & 1 draft programs',
    })

    // Publish and add a reference from a new program in the draft version.
    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.publishAllPrograms()
    const secondProgramName = 'second-program'
    await adminPrograms.addProgram(secondProgramName)
    await adminPrograms.addProgramBlock(secondProgramName, 'first block', [
      questionName,
    ])

    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.expectQuestionProgramReferencesText({
      questionName,
      expectedProgramReferencesText: 'Used across 1 active & 2 draft programs',
    })

    await adminQuestions.expectProgramReferencesModalContains({
      questionName,
      expectedDraftProgramReferences: ['first-program', 'second-program'],
      expectedActiveProgramReferences: ['first-program'],
    })
    await validateScreenshot(pageObject);
  })
})
