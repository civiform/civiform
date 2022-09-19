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

    // A question cannot be published in isolation. In order to allow making created questions
    // active, create a fake program.
    const programName = 'question-list-test-program'
    await adminPrograms.addProgram(programName)

    // Most recently added question is on top.
    await expectQuestionListElements(adminQuestions, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])

    // Publish all programs and questions, order should be maintained.
    await adminPrograms.publishProgram(programName)
    // Create a draft version of the program so that the question bank can be accessed.
    await adminPrograms.createNewVersion(programName)
    await expectQuestionListElements(adminQuestions, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])

    // Now create a draft version of the previously last question. After,
    // it should be on top.
    await adminQuestions.createNewVersion(questionOnePublishedText)
    // CreateNewVersion implicitly updates the question text to be suffixed with " new version".
    const questionOneDraftText = `${questionOnePublishedText} new version`
    await expectQuestionListElements(adminQuestions, [
      questionOneDraftText,
      questionTwoPublishedText,
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      questionOnePublishedText,
      questionTwoPublishedText,
    ])

    // Now create a new question, which should be on top.
    const questionThreePublishedText = 'question list test question three'
    await adminQuestions.addNameQuestion({
      questionName: questionThreePublishedText,
      questionText: questionThreePublishedText,
    })
    await expectQuestionListElements(adminQuestions, [
      questionThreePublishedText,
      questionOneDraftText,
      questionTwoPublishedText,
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      questionThreePublishedText,
      questionOnePublishedText,
      questionTwoPublishedText,
    ])
  })

  async function expectQuestionListElements(
    adminQuestions: AdminQuestions,
    expectedQuestions: string[],
  ) {
    if (!expectedQuestions) {
      throw new Error('expected at least one question')
    }
    const questionListNames = await adminQuestions.questionNames()
    expect(questionListNames).toEqual(expectedQuestions)
  }

  async function expectQuestionBankElements(
    programName: string,
    adminPrograms: AdminPrograms,
    expectedQuestions: string[],
  ) {
    if (!expectedQuestions) {
      throw new Error('expected at least one question')
    }
    const questionBankNames = await adminPrograms.questionBankNames(programName)
    expect(questionBankNames).toEqual(expectedQuestions)
  }
})
