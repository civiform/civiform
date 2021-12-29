import {
  startSession,
  loginAsAdmin,
  AdminQuestions,
  AdminPrograms,
  endSession,
} from './support'

describe('deleting question lifecycle', () => {
  it('create, publish, delete unused questions', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)
    const programName = 'deleting program'
    const questions = await adminQuestions.addAllNonSingleBlockQuestionTypes(
      'delete-'
    )
    await adminPrograms.addProgram(programName)
    const onlyUsedQuestion = questions[0]
    await adminPrograms.editProgramBlock(
      programName,
      'qlc program description',
      [onlyUsedQuestion]
    )
    await adminQuestions.discardDraft(questions[1])
    await adminPrograms.publishProgram(programName)
    await adminQuestions.expectActiveQuestionExist(onlyUsedQuestion)
    await adminQuestions.expectActiveQuestionNotExist(questions[1])
    for (let i = 2; i < questions.length; i++) {
      await adminQuestions.expectActiveQuestionExist(questions[i])
      await adminQuestions.archiveQuestion(questions[i])
      await adminQuestions.undeleteQuestion(questions[i])
      await adminQuestions.expectActiveQuestionExist(questions[i])
      await adminQuestions.archiveQuestion(questions[i])
    }
    await adminPrograms.createNewVersion(programName)
    await adminPrograms.publishProgram(programName)
    await adminQuestions.expectActiveQuestionExist(onlyUsedQuestion)
    for (let i = 2; i < questions.length; i++) {
      await adminQuestions.expectActiveQuestionNotExist(questions[i])
    }
  })
})
