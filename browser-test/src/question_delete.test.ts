import { AdminPrograms, AdminQuestions, loginAsAdmin, startSession, } from './support'

describe('deleting question lifecycle', () => {
  it('create, publish, delete unused questions', async () => {
    const { page } = await startSession()
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
    await adminPrograms.publishProgram(programName)
    await adminQuestions.expectActiveQuestionExist(onlyUsedQuestion)

    // Make a Draft then discard it.
    await adminQuestions.createNewVersion(questions[1])
    await adminQuestions.expectDraftQuestionExist(questions[1])
    await adminQuestions.discardDraft(questions[1])
    await adminQuestions.expectActiveQuestionExist(questions[1])
    // Archive, unarchive, archive all other questions.
    for (let i = 2; i < questions.length; i++) {
      await adminQuestions.expectActiveQuestionExist(questions[i])
      await adminQuestions.archiveQuestion(questions[i])
      await adminQuestions.undeleteQuestion(questions[i])
      await adminQuestions.expectActiveQuestionExist(questions[i])
      await adminQuestions.archiveQuestion(questions[i])
    }

    // Publish all the above changes.
    await adminPrograms.createNewVersion(programName)
    await adminPrograms.publishProgram(programName)
    await adminQuestions.expectActiveQuestionExist(onlyUsedQuestion)
    for (let i = 2; i < questions.length; i++) {
      await adminQuestions.expectActiveQuestionNotExist(questions[i])
    }
  })
})
