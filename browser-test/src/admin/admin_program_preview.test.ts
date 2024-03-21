import {expect, test} from '@playwright/test'
import {
  createTestContext,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

test.describe('admin program preview', () => {
  const ctx = createTestContext()

  test('preview draft program and submit', async () => {
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

  test('preview active program and submit', async () => {
    const {page, adminPrograms, adminQuestions, applicantQuestions} = ctx
    await loginAsAdmin(page)

    await adminQuestions.addEmailQuestion({questionName: 'email-q'})
    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'description', [
      'email-q',
    ])
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoViewActiveProgramPage(programName)

    await page.click('button:has-text("Preview as applicant")')
    await waitForPageJsLoad(page)

    await applicantQuestions.clickContinue()
    await applicantQuestions.answerEmailQuestion('email@example.com')

    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await adminPrograms.expectProgramBlockReadOnlyPage()
  })

  test('preview program and use back button', async () => {
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

    await applicantQuestions.clickContinue()

    await page.click('a:has-text("Back to admin view")')

    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  test('download pdf preview of draft program', async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)

    await adminQuestions.addEmailQuestion({questionName: 'email-q'})
    const programName = 'Example Draft Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'description', [
      'email-q',
    ])
    await adminPrograms.gotoEditDraftProgramPage(programName)

    const pdfFile = await adminPrograms.getProgramPdf()

    expect(pdfFile.length).toBeGreaterThan(1)
    // The java services.export.PdfExporterTest class has tests that verify the PDF contents.
    // This browser test just ensures a file is downloaded when the button is clicked.
  })

  test('download pdf preview of active program', async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)

    await adminQuestions.addEmailQuestion({questionName: 'email-q'})
    const programName = 'Example Active Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'description', [
      'email-q',
    ])
    await adminPrograms.publishProgram(programName)
    await adminPrograms.gotoViewActiveProgramPage(programName)

    const pdfFile = await adminPrograms.getProgramPdf()

    expect(pdfFile.length).toBeGreaterThan(1)
    // The java services.export.PdfExporterTest class has tests that verify the PDF contents.
    // This browser test just ensures a file is downloaded when the button is clicked.
  })
})
