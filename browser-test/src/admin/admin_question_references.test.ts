import {test} from '@playwright/test'
import {createTestContext, loginAsAdmin, validateScreenshot} from '../support'

test.describe('view program references from question view', () => {
  const ctx = createTestContext()

  test('shows no results for an unreferenced question', async () => {
    const {page, adminQuestions} = ctx
    await loginAsAdmin(page)
    const questionName = 'unreferenced-q'
    await adminQuestions.addAddressQuestion({questionName})
    await adminQuestions.expectQuestionProgramReferencesText({
      questionName,
      expectedProgramReferencesText: 'Used in 0 programs',
      version: 'draft',
    })
  })

  test('shows results for referencing programs', async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    const questionName = 'question-references-q'
    await loginAsAdmin(page)
    await adminQuestions.addAddressQuestion({questionName})

    // Add a reference to the question in the second block. We'll later assert
    // that the links in the modal takes us to the correct block.
    const firstProgramName = 'First program'
    await adminPrograms.addProgram(firstProgramName)
    await adminPrograms.addProgramBlock(firstProgramName, 'first block', [])
    await adminPrograms.addProgramBlock(firstProgramName, 'second block', [
      questionName,
    ])

    const secondProgramName = 'Second program'
    await adminPrograms.addProgram(secondProgramName)
    await adminPrograms.addProgramBlock(secondProgramName, 'first block', [
      questionName,
    ])

    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.expectQuestionProgramReferencesText({
      questionName,
      expectedProgramReferencesText: 'Added to 2 programs.',
      version: 'draft',
    })

    // Publish.
    await adminPrograms.publishAllDrafts()

    // Add a reference from a new program in the draft version.
    const thirdProgramName = 'Third program'
    await adminPrograms.addProgram(thirdProgramName)
    await adminPrograms.addProgramBlock(thirdProgramName, 'first block', [
      questionName,
    ])

    // Remove question from an existing published program.
    await adminPrograms.createNewVersion(secondProgramName)
    await adminPrograms.removeQuestionFromProgram(
      secondProgramName,
      'Screen 2',
      [questionName],
    )

    await adminQuestions.gotoAdminQuestionsPage()
    await validateScreenshot(page, 'question-used-in-programs')
    await adminQuestions.expectQuestionProgramReferencesText({
      questionName,
      expectedProgramReferencesText:
        'Used in 1 program.\n\nAdded to 1 program.\n\nRemoved from 1 program.',
      version: 'active',
    })

    await adminQuestions.expectProgramReferencesModalContains({
      questionName,
      expectedUsedProgramReferences: ['first-program'],
      expectedAddedProgramReferences: ['third-program'],
      expectedRemovedProgramReferences: ['second-program'],
    })

    await adminQuestions.clickOnProgramReferencesModal(questionName)
    await validateScreenshot(page, 'question-program-modal')
  })
})
