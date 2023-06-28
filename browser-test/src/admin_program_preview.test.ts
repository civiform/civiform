import {
  createTestContext,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'

describe('admin program preview', () => {
  const ctx = createTestContext()

  it('preview draft program and submit', async () => {
    const {page, adminPrograms, adminQuestions, applicantQuestions} = ctx
    await loginAsAdmin(page)

    await adminQuestions.addEmailQuestion({questionName: 'email-q'})
    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'description', [
      'email-q',
    ])
    await adminPrograms.gotoEditDraftProgramPage(programName)

    await page.click('button:has-text("Preview as applicant")')
    await waitForPageJsLoad(page)
    await validateScreenshot(page, 'program-preview-application-review-page')

    await applicantQuestions.clickContinue()
    await applicantQuestions.answerEmailQuestion('email@example.com')
    await validateScreenshot(page, 'program-preview-application-block')

    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  it('preview active program and submit', async () => {
    const {page, adminPrograms, adminQuestions, applicantQuestions} = ctx
    await loginAsAdmin(page)

    await adminQuestions.addEmailQuestion({questionName: 'email-q'})
    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'description', [
      'email-q',
    ])
    await adminPrograms.publishAllPrograms()
    await adminPrograms.gotoViewActiveProgramPage(programName)

    await page.click('button:has-text("Preview as applicant")')
    await waitForPageJsLoad(page)

    await applicantQuestions.clickContinue()
    await applicantQuestions.answerEmailQuestion('email@example.com')

    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await adminPrograms.expectProgramBlockReadOnlyPage()
  })
})
