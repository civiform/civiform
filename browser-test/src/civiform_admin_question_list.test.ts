import {
  AdminPrograms,
  AdminQuestions,
  createTestContext,
  loginAsAdmin,
} from './support'

describe('Most recently updated question is at top of list.', () => {
  const ctx = createTestContext()
  it('sorts by last updated, preferring draft over active', async () => {
    const {page, adminPrograms, adminQuestions} = ctx

    await loginAsAdmin(page)

    const questionOnePublishedText = 'question list test question one'
    const questionTwoPublishedText = 'question list test question two'
    await adminQuestions.addNameQuestion({
      questionName: questionOnePublishedText,
      questionText: questionOnePublishedText,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionTwoPublishedText,
      questionText: questionTwoPublishedText,
    })

    // Note: CI tests already have test questions
    // available. As such, we only assert the order
    // of the questions added in this test.
    // TODO(#3029): Consider asserting on the whole list if it can be considered stable even in
    // prober environments and with canonical question creation.

    // A question cannot be published in isolation. In order to allow making created questions
    // active, create a fake program.
    const programName = 'question list test program'
    await adminPrograms.addProgram(programName)

    // Most recently added question is on top.
    await expectQuestionListTopElements(adminQuestions, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])
    await expectQuestionBankTopElements(programName, adminPrograms, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])

    // Publish all programs and questions, order should be maintained.
    await adminPrograms.publishProgram(programName)
    // Create a draft version of the program so that the question bank can be accessed.
    await adminPrograms.createNewVersion(programName)
    await expectQuestionListTopElements(adminQuestions, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])
    await expectQuestionBankTopElements(programName, adminPrograms, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])

    // Now create a draft version of the previously last question. After,
    // it should be on top.
    await adminQuestions.createNewVersion(questionOnePublishedText)
    // CreateNewVersion implicitly updates the question text to be suffixed with " new version".
    const questionOneDraftText = `${questionOnePublishedText} new version`
    await expectQuestionListTopElements(adminQuestions, [
      questionOneDraftText,
      questionTwoPublishedText,
    ])
    await expectQuestionBankTopElements(programName, adminPrograms, [
      questionOnePublishedText,
      questionTwoPublishedText,
    ])

    // Now create a new question, which should be on top.
    const questionThreePublishedText = 'question list test question three'
    await adminQuestions.addNameQuestion({
      questionName: questionThreePublishedText,
      questionText: questionThreePublishedText,
    })
    await expectQuestionListTopElements(adminQuestions, [
      questionThreePublishedText,
      questionOneDraftText,
      questionTwoPublishedText,
    ])
    await expectQuestionBankTopElements(programName, adminPrograms, [
      questionThreePublishedText,
      questionOnePublishedText,
      questionTwoPublishedText,
    ])
  })

  async function expectQuestionListTopElements(
    adminQuestions: AdminQuestions,
    expectedQuestions: string[],
  ) {
    if (!expectedQuestions) {
      throw new Error('expected at least one question')
    }
    const questionListNames = await adminQuestions.questionNames()
    expect(questionListNames.length).toBeGreaterThanOrEqual(
      expectedQuestions.length,
    )
    expect(questionListNames.slice(0, expectedQuestions.length)).toEqual(
      expectedQuestions,
    )
  }

  async function expectQuestionBankTopElements(
    programName: string,
    adminPrograms: AdminPrograms,
    expectedQuestions: string[],
  ) {
    if (!expectedQuestions) {
      throw new Error('expected at least one question')
    }
    const questionBankNames = await adminPrograms.questionBankNames(programName)
    expect(questionBankNames.length).toBeGreaterThanOrEqual(
      expectedQuestions.length,
    )
    expect(questionBankNames.slice(0, expectedQuestions.length)).toEqual(
      expectedQuestions,
    )
  }
})
