import {
  AdminPrograms,
  AdminQuestions,
  createTestContext,
  loginAsAdmin,
  validateScreenshot,
} from './support'
describe('Admin question list', () => {
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
    const programName = 'Question list test program'
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
      questionOneDraftText,
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
      questionOneDraftText,
      questionTwoPublishedText,
    ])
  })

  it('filters question list with search query', async () => {
    const {page, adminQuestions} = ctx
    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: 'q-f',
      questionText: 'first question',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'q-s',
      questionText: 'second question',
    })

    await adminQuestions.gotoAdminQuestionsPage()

    await page.locator('#question-bank-filter').fill('first')
    expect(await adminQuestions.questionBankNames()).toEqual(['first question'])
    await page.locator('#question-bank-filter').fill('second')
    expect(await adminQuestions.questionBankNames()).toEqual([
      'second question',
    ])
    await page.locator('#question-bank-filter').fill('')
    expect(await adminQuestions.questionBankNames()).toEqual([
      'second question',
      'first question',
    ])
  })

  it('sorts question list based on selection', async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    await loginAsAdmin(page)
    // Set the questionText to the same as questionName to make validation easier since questionBankNames() returns the questionText. The questionText is not actually used to sort.
    await adminQuestions.addTextQuestion({
      questionName: 'b',
      questionText: 'b',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'a',
      questionText: 'a',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'c',
      questionText: 'c',
    })

    await adminPrograms.addAndPublishProgramWithQuestions(['a'], 'program-one')
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['a', 'c'],
      'program-two',
    )

    await adminQuestions.gotoAdminQuestionsPage()

    await adminQuestions.sortQuestions('adminname-asc')
    expect(await adminQuestions.questionBankNames()).toEqual(['a', 'b', 'c'])
    await adminQuestions.sortQuestions('adminname-desc')
    expect(await adminQuestions.questionBankNames()).toEqual(['c', 'b', 'a'])

    // Question 'b' is referenced by 0 programs, 'c' by 1 program, and 'a' by 2 programs.
    await adminQuestions.sortQuestions('numprograms-asc')
    expect(await adminQuestions.questionBankNames()).toEqual(['b', 'c', 'a'])
    await adminQuestions.sortQuestions('numprograms-desc')
    expect(await adminQuestions.questionBankNames()).toEqual(['a', 'c', 'b'])

    // Question 'c' was modified after question 'a' which was modified after 'b'.
    await adminQuestions.sortQuestions('lastmodified-desc')
    expect(await adminQuestions.questionBankNames()).toEqual(['c', 'a', 'b'])

    await validateScreenshot(page, 'questions-list-sort-dropdown')
  })

  it('shows if questions are marked for archival', async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    await loginAsAdmin(page)

    const questionOne = 'question list test question one'
    const questionTwo = 'question list test question two'
    const questionThree = 'question list test question three'
    await adminQuestions.addNameQuestion({
      questionName: questionOne,
      questionText: questionOne,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionTwo,
      questionText: questionTwo,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionThree,
      questionText: questionThree,
    })

    // Publish questions
    await adminPrograms.publishAllPrograms()

    await adminQuestions.createNewVersion(questionTwo)
    await adminQuestions.archiveQuestion({
      questionName: questionThree,
      expectModal: false,
    })

    await validateScreenshot(page, 'questions-list-with-archived-questions')
  })

  it('does not sort archived questions', async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    await loginAsAdmin(page)

    const questionOne = 'question list test question one'
    const questionTwo = 'question list test question two'
    const questionThree = 'question list test question three'
    await adminQuestions.addNameQuestion({
      questionName: questionOne,
      questionText: questionOne,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionTwo,
      questionText: questionTwo,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionThree,
      questionText: questionThree,
    })

    // Publish questions
    await adminPrograms.publishAllPrograms()

    await adminQuestions.archiveQuestion({
      questionName: questionThree,
      expectModal: false,
    })

    // Check that archived question is still at the bottom after sorting.
    await adminQuestions.sortQuestions('adminname-asc')
    expect(await adminQuestions.questionBankNames()).toEqual([
      'question list test question one',
      'question list test question two',
      'question list test question three',
    ])
    await adminQuestions.sortQuestions('lastmodified-desc')
    expect(await adminQuestions.questionBankNames()).toEqual([
      'question list test question two',
      'question list test question one',
      'question list test question three',
    ])
  })

  async function expectQuestionListElements(
    adminQuestions: AdminQuestions,
    expectedQuestions: string[],
  ) {
    if (expectedQuestions.length === 0) {
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
    if (expectedQuestions.length === 0) {
      throw new Error('expected at least one question')
    }
    await adminPrograms.gotoEditDraftProgramPage(programName)
    await adminPrograms.openQuestionBank()
    const questionBankNames = await adminPrograms.questionBankNames()
    expect(questionBankNames).toEqual(expectedQuestions)
  }
})
