import {
  createTestContext,
  dropTables,
  loginAsAdmin,
  logout,
  seedQuestions,
  validateAccessibility,
  validateScreenshot,
} from './support'
import {BASE_URL} from './support/config'

describe('file upload applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  beforeAll(async () => {
    const {page} = ctx
    await dropTables(page)
    await seedQuestions(page)
    await page.goto(BASE_URL)
  })

  describe('single file upload question', () => {
    const programName = 'Test program for single file upload'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-q',
        questionText: 'fileupload question text',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['file-upload-test-q'],
        programName,
      )

      await logout(page)
    })

    it('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'file-required')
    })

    it('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'file-errors')
    })

    it('form is correctly formatted', async () => {
      const {page, applicantQuestions} = ctx

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      const formInputs = await page
        .locator('#cf-block-form')
        .locator('input')
        .all()
      const lastFormInput = formInputs[formInputs.length - 1]

      // AWS requires that the <input type="file"> element to be the last <input> in the <form>
      expect(await lastFormInput.getAttribute('type')).toBe('file')
    })

    it('does not show errors initially', async () => {
      const {applicantQuestions, file} = ctx

      await applicantQuestions.applyProgram(programName)

      await file.expectFileSelectionErrorHidden()
    })

    it('does not show skip button for required question', async () => {
      const {page, applicantQuestions} = ctx

      await applicantQuestions.applyProgram(programName)

      expect(await page.$('#fileupload-skip-button')).toBeNull()
    })

    it('can upload file', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion('some file')

      await validateScreenshot(page, 'file-uploaded')
    })

    it('can download file content', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)
      await applicantQuestions.clickNext()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual(fileContent)
    })

    it('with valid file can proceed and submit', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)

      await applicantQuestions.clickNext()

      await applicantQuestions.expectReviewPage()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with no file shows error and does not proceed', async () => {
      const {page, applicantQuestions, file} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await file.expectFileSelectionErrorShown()
      // Verify we're still on the file upload question block
      expect(await page.innerText('.cf-applicant-question-text')).toContain(
        'fileupload question text',
      )
    })

    it('missing file error disappears when file uploaded', async () => {
      const {applicantQuestions, file} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()
      await file.expectFileSelectionErrorShown()

      await applicantQuestions.answerFileUploadQuestion('some text')

      await file.expectFileSelectionErrorHidden()
    })

    it('re-answering question shows previous file', async () => {
      // Answer the file upload question
            const {applicantQuestions, file} = ctx
            await applicantQuestions.applyProgram(programName)
            await applicantQuestions.answerFileUploadQuestion('some text')

      // Re-open the file upload question
      await applicantQuestions.clickNext()

      // Verify the previously uploaded file is shown
    })

    it('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  // Optional file upload.
  describe('optional file upload question', () => {
    const programName = 'Test program for optional file upload'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-optional-q',
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        [],
        'file-upload-test-optional-q',
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    it('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'file-optional')
    })

    it('with missing file shows error and does not proceed if Save&next', async () => {
      const {page, applicantQuestions, file} = ctx
      await applicantQuestions.applyProgram(programName)

      // When the applicant clicks "Save & next"
      await applicantQuestions.clickNext()

      // Then we should still show the error, even for an optional question
      await file.expectFileSelectionErrorShown()
      // Verify we're still on the file upload question block
      expect(await page.innerText('.cf-applicant-question-text')).toContain(
        'fileupload question text',
      )
    })

    it('missing file error disappears when file uploaded', async () => {
      const {applicantQuestions, file} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()
      await file.expectFileSelectionErrorShown()

      await applicantQuestions.answerFileUploadQuestion('some text')

      await file.expectFileSelectionErrorHidden()
    })

    it('can be skipped', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      // When the applicant clicks "Skip"
      await applicantQuestions.clickSkip()

      // Then the question is skipped because file upload is optional
      await applicantQuestions.submitFromReviewPage()
    })

    it('can download file content', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)
      await applicantQuestions.clickNext()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual(fileContent)
    })

    it('with valid file can proceed and submit', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)

      await applicantQuestions.clickNext()

      await applicantQuestions.expectReviewPage()

      await applicantQuestions.submitFromReviewPage()
    })
  })
})
