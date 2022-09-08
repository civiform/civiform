import {createTestContext, loginAsAdmin} from './support'

describe('view program references from question view', () => {
  const ctx = createTestContext()

  it('shows no results for an unreferenced question', async () => {
    const {page, adminQuestions} = ctx
    await loginAsAdmin(page)
    const questionName = 'unreferenced-q'
    await adminQuestions.addAddressQuestion({questionName})
    await adminQuestions.expectQuestionProgramReferencesText({
      questionName,
      expectedProgramReferencesText: 'Used across 0 programs',
    })
  })

  it('shows results for referencing programs', async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    const questionName = 'question-references-q'
    await loginAsAdmin(page)
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
      expectedProgramReferencesText: 'Used across 1 draft programs',
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
  })
})
