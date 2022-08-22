import {
  startSession,
  loginAsAdmin,
  AdminQuestions,
  AdminPrograms,
} from './support'

describe('deleting question lifecycle', () => {
  it('create, publish, delete unused questions', async () => {
    const {page} = await startSession()
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)
    const programName = 'deleting program'
    const questions = await adminQuestions.addAllNonSingleBlockQuestionTypes(
      'delete-',
    )
    const onlyUsedQuestion = questions[0]
    const unreferencedQuestions = questions.slice(1)
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(
      programName,
      'qlc program description',
      [onlyUsedQuestion],
    )
    await adminPrograms.publishProgram(programName)
    await adminQuestions.expectActiveQuestionExist(onlyUsedQuestion)

    // Confirm the archive option is still available and displays a dialog.
    await adminQuestions.archiveQuestion({
      questionName: onlyUsedQuestion,
      expectModal: true,
    })

    // Make a Draft then discard it.
    await adminQuestions.createNewVersion(unreferencedQuestions[0])
    await adminQuestions.expectDraftQuestionExist(unreferencedQuestions[0])
    await adminQuestions.discardDraft(unreferencedQuestions[0])
    await adminQuestions.expectActiveQuestionExist(unreferencedQuestions[0])

    // Create an unreferenced question in the draft version.
    const draftOnlyQuestionName = 'draftonly-address'
    await adminQuestions.addAddressQuestion({
      questionName: draftOnlyQuestionName,
    })

    // Archive, unarchive, archive all unreferenced questions.
    for (const questionName of unreferencedQuestions) {
      await adminQuestions.expectActiveQuestionExist(questionName)
      await adminQuestions.archiveQuestion({questionName, expectModal: false})
      await adminQuestions.undeleteQuestion(questionName)
      await adminQuestions.expectActiveQuestionExist(questionName)
      await adminQuestions.archiveQuestion({questionName, expectModal: false})
    }
    // Archive, unarchive, archive an unreferenced draft question.
    await adminQuestions.expectDraftQuestionExist(draftOnlyQuestionName)
    await adminQuestions.archiveQuestion({
      questionName: draftOnlyQuestionName,
      expectModal: false,
    })
    await adminQuestions.undeleteQuestion(draftOnlyQuestionName)
    await adminQuestions.expectDraftQuestionExist(draftOnlyQuestionName)
    await adminQuestions.archiveQuestion({
      questionName: draftOnlyQuestionName,
      expectModal: false,
    })

    // Publish all the above changes.
    await adminPrograms.createNewVersion(programName)
    await adminPrograms.publishProgram(programName)
    await adminQuestions.expectActiveQuestionExist(onlyUsedQuestion)
    for (const questionName of unreferencedQuestions.concat(
      draftOnlyQuestionName,
    )) {
      await adminQuestions.expectQuestionNotExist(questionName)
    }
  })
})
