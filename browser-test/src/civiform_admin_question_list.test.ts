import {createTestContext, loginAsAdmin} from './support'

describe('Most recently updated program is at top of list.', () => {
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

    // Most recently added question is on top.
    let questionNames = await adminQuestions.questionNames()
    expect(questionNames.length).toBeGreaterThanOrEqual(2)
    expect(questionNames.slice(0, 2)).toEqual([questionTwo, questionOne])

    // A question cannot be published in isolation. In order to make the previous questions active,
    // create a fake program and publish it.
    await adminPrograms.addProgram('question list test program')
    await adminPrograms.publishAllPrograms()

    // Previous relative order should be maintained.
    questionNames = await adminQuestions.questionNames()
    expect(questionNames.length).toBeGreaterThanOrEqual(2)
    expect(questionNames.slice(0, 2)).toEqual([questionTwo, questionOne])

    // Now create a draft version of the previously last question. After,
    // it should be on top.
    await adminQuestions.createNewVersion(questionOne)
    questionNames = await adminQuestions.questionNames()
    expect(questionNames.length).toBeGreaterThanOrEqual(2)
    expect(questionNames.slice(0, 2)).toEqual([questionOne, questionTwo])

    // Now create a new question, which should be on top.
    const questionThree = 'question list test question three'
    await adminQuestions.addNameQuestion({questionName: questionThree})
    questionNames = await adminQuestions.questionNames()
    expect(questionNames.length).toBeGreaterThanOrEqual(3)
    expect(questionNames.slice(0, 3)).toEqual([
      questionThree,
      questionOne,
      questionTwo,
    ])
  })
})
