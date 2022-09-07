import {createTestContext, loginAsAdmin} from './support'

describe('Most recently updated question is at top of list.', () => {
  const ctx = createTestContext()
  it('sorts by last updated, preferring draft over active', async () => {
    const {page, adminPrograms, adminQuestions} = ctx

    await loginAsAdmin(page)

    const questionOne = 'question list test question one'
    const questionTwo = 'question list test question two'
    await adminQuestions.addNameQuestion({questionName: questionOne})
    await adminQuestions.addNameQuestion({questionName: questionTwo})

    // Note: CI tests already have test questions
    // available. As such, we only assert the order
    // of the questions added in this test.

    // A question cannot be published in isolation. In order to allow making created questions
    // active, create a fake program.
    const programName = 'question list test program'
    await adminPrograms.addProgram(programName)

    // Most recently added question is on top.
    let questionListNames = await adminQuestions.questionNames()
    expect(questionListNames.length).toBeGreaterThanOrEqual(2)
    expect(questionListNames.slice(0, 2)).toEqual([questionTwo, questionOne])
    // Question bank.
    let questionBankNames = await adminPrograms.questionBankNames(programName)
    expect(questionBankNames.length).toBeGreaterThanOrEqual(2)
    expect(questionBankNames.slice(0, 2)).toEqual([questionTwo, questionOne])

    // Publish all programs and questions, order should be maintained.
    await adminPrograms.publishProgram(programName)
    // Create a draft version of the program so that the question bank can be accessed.
    await adminPrograms.createNewVersion(programName)
    questionListNames = await adminQuestions.questionNames()
    expect(questionListNames.length).toBeGreaterThanOrEqual(2)
    expect(questionListNames.slice(0, 2)).toEqual([questionTwo, questionOne])
    questionBankNames = await adminPrograms.questionBankNames(programName)
    expect(questionBankNames.length).toBeGreaterThanOrEqual(2)
    expect(questionBankNames.slice(0, 2)).toEqual([questionTwo, questionOne])

    // Now create a draft version of the previously last question. After,
    // it should be on top.
    await adminQuestions.createNewVersion(questionOne)
    questionListNames = await adminQuestions.questionNames()
    expect(questionListNames.length).toBeGreaterThanOrEqual(2)
    expect(questionListNames.slice(0, 2)).toEqual([questionOne, questionTwo])
    questionBankNames = await adminPrograms.questionBankNames(programName)
    expect(questionBankNames.length).toBeGreaterThanOrEqual(2)
    expect(questionBankNames.slice(0, 2)).toEqual([questionOne, questionTwo])

    // Now create a new question, which should be on top.
    const questionThree = 'question list test question three'
    await adminQuestions.addNameQuestion({questionName: questionThree})
    questionListNames = await adminQuestions.questionNames()
    expect(questionListNames.length).toBeGreaterThanOrEqual(3)
    expect(questionListNames.slice(0, 3)).toEqual([
      questionThree,
      questionOne,
      questionTwo,
    ])
    questionBankNames = await adminPrograms.questionBankNames(programName)
    expect(questionBankNames.length).toBeGreaterThanOrEqual(3)
    expect(questionBankNames.slice(0, 3)).toEqual([
      questionThree,
      questionOne,
      questionTwo,
    ])
  })
})
