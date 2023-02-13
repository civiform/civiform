import {
  createTestContext,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
} from './support'
import {QuestionType} from './support/admin_questions'

describe('deleting question lifecycle', () => {
  const ctx = createTestContext()

  // TODO(#4125) This test case is a duplicate of the test below and should be removed once the program_read_only_view
  // flag has been removed.
  it('create, publish, delete unused questions with read only program view disabled', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

    await loginAsAdmin(page)
    await disableFeatureFlag(page, 'program_read_only_view_enabled')

    const programName = 'Deleting program'
    const onlyUsedQuestion = 'delete-address'
    await adminQuestions.addQuestionForType(
      QuestionType.ADDRESS,
      onlyUsedQuestion,
    )

    const unusedCheckboxQuestion = 'delete-checkbox'
    await adminQuestions.addQuestionForType(
      QuestionType.CHECKBOX,
      unusedCheckboxQuestion,
    )
    const unusedEmailQuestion = 'delete-email'
    await adminQuestions.addQuestionForType(
      QuestionType.EMAIL,
      unusedEmailQuestion,
    )
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
    await adminQuestions.createNewVersion(unusedCheckboxQuestion)
    await adminQuestions.expectDraftQuestionExist(unusedCheckboxQuestion)
    await adminQuestions.discardDraft(unusedCheckboxQuestion)
    await adminQuestions.expectActiveQuestionExist(unusedCheckboxQuestion)

    // Create an unreferenced question in the draft version.
    const draftOnlyQuestionName = 'draftonly-address'
    await adminQuestions.addAddressQuestion({
      questionName: draftOnlyQuestionName,
    })

    // Archive, unarchive, archive all unreferenced questions.
    for (const questionName of [unusedEmailQuestion, unusedCheckboxQuestion]) {
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
    await adminPrograms.createNewVersion(
      programName,
      /* programReadOnlyViewEnabled = */ false,
    )
    await adminPrograms.publishProgram(programName)
    await adminQuestions.expectActiveQuestionExist(onlyUsedQuestion)
    for (const questionName of [
      unusedEmailQuestion,
      unusedCheckboxQuestion,
      draftOnlyQuestionName,
    ]) {
      await adminQuestions.expectQuestionNotExist(questionName)
    }
  })

  it('create, publish, delete unused questions', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_read_only_view_enabled')

    const programName = 'Deleting program'
    const onlyUsedQuestion = 'delete-address'
    await adminQuestions.addQuestionForType(
      QuestionType.ADDRESS,
      onlyUsedQuestion,
    )

    const unusedCheckboxQuestion = 'delete-checkbox'
    await adminQuestions.addQuestionForType(
      QuestionType.CHECKBOX,
      unusedCheckboxQuestion,
    )
    const unusedEmailQuestion = 'delete-email'
    await adminQuestions.addQuestionForType(
      QuestionType.EMAIL,
      unusedEmailQuestion,
    )
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
    await adminQuestions.createNewVersion(unusedCheckboxQuestion)
    await adminQuestions.expectDraftQuestionExist(unusedCheckboxQuestion)
    await adminQuestions.discardDraft(unusedCheckboxQuestion)
    await adminQuestions.expectActiveQuestionExist(unusedCheckboxQuestion)

    // Create an unreferenced question in the draft version.
    const draftOnlyQuestionName = 'draftonly-address'
    await adminQuestions.addAddressQuestion({
      questionName: draftOnlyQuestionName,
    })

    // Archive, unarchive, archive all unreferenced questions.
    for (const questionName of [unusedEmailQuestion, unusedCheckboxQuestion]) {
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
    for (const questionName of [
      unusedEmailQuestion,
      unusedCheckboxQuestion,
      draftOnlyQuestionName,
    ]) {
      await adminQuestions.expectQuestionNotExist(questionName)
    }
  })
})
