import {
  startSession,
  loginAsAdmin,
  AdminQuestions,
  AdminPrograms,
  endSession,
  logout,
  loginAsTestUser,
  selectApplicantLanguage,
  ApplicantQuestions,
} from './support'
import {QuestionType} from './support/admin_questions'

describe('optional application flow', () => {
  for (const type of Object.values(QuestionType)) {
    it(`program with optional ${type} question`, async () => {
      const {browser, page} = await startSession()

      await loginAsAdmin(page)
      const adminQuestions = new AdminQuestions(page)
      const adminPrograms = new AdminPrograms(page)

      const questionName = `optional-${type}`
      await adminQuestions.addQuestionForType(type, questionName)

      const programName = `Optional Questions Program for question ${type}`
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'first description',
        [],
        questionName,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.expectDraftProgram(programName)
      await adminPrograms.publishAllPrograms()
      await adminPrograms.expectActiveProgram(programName)

      await logout(page)
      await loginAsTestUser(page)
      await selectApplicantLanguage(page, 'English')

      const applicantQuestions = new ApplicantQuestions(page)
      await applicantQuestions.applyProgram(programName)

      // Skip first block without uploading a file
      if (type === QuestionType.FILE_UPLOAD) {
        await applicantQuestions.clickSkip()
      } else {
        await applicantQuestions.clickNext()
      }

      // Submit the first program
      await applicantQuestions.submitFromReviewPage()
      await endSession(browser)
    })
  }
})
