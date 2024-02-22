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
  const fileUploadQuestionText = 'Required file upload question'

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
        questionText: fileUploadQuestionText,
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
      const {applicantQuestions, applicantFileQuestion} = ctx

      await applicantQuestions.applyProgram(programName)

      await applicantFileQuestion.expectFileSelectionErrorHidden()
    })

    it('no continue button initially', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx

      await applicantQuestions.applyProgram(programName)

      await applicantFileQuestion.expectNoContinueButton()
    })

    it('does not show skip button for required question', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx

      await applicantQuestions.applyProgram(programName)

      await applicantFileQuestion.expectNoSkipButton()
    })

    it('can upload file', async () => {
      const {page, applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestion('some file', 'file.txt')

      await applicantFileQuestion.expectFileNameDisplayed('file.txt')
      await validateScreenshot(page, 'file-uploaded')
    })

    /** Regression test for https://github.com/civiform/civiform/issues/6221. */
    it('can replace file', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestion(
        'some file',
        'file1.txt',
      )
      await applicantFileQuestion.expectFileNameDisplayed('file1.txt')

      await applicantQuestions.answerFileUploadQuestion(
        'some file',
        'file2.txt',
      )
      await applicantFileQuestion.expectFileNameDisplayed('file2.txt')

      await applicantQuestions.clickNext()

      await applicantQuestions.expectReviewPage()
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file2.txt',
      )
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
      await applicantQuestions.answerFileUploadQuestion('some sample text')

      await applicantQuestions.clickNext()

      // Verify we're taken to the next page (which is the review page since
      // this program only has one block) and can submit the application.
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.submitFromReviewPage()
    })

    it('with missing file shows error and does not proceed', async () => {
      const {page, applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.clickNext()

      await applicantFileQuestion.expectFileSelectionErrorShown()
      // Verify we're still on the file upload question block
      expect(await page.innerText('.cf-applicant-question-text')).toContain(
        fileUploadQuestionText,
      )
    })

    it('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })

    it('re-answering question shows previously uploaded file name on review and block pages', async () => {
      // Answer the file upload question
      const {page, applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickNext()

      // Verify the previously uploaded file name is shown on the review page
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'testFileName.txt',
      )

      // Re-open the file upload question
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      // Verify the previously uploaded file name is shown on the block page
      await applicantFileQuestion.expectFileNameDisplayed('testFileName.txt')
      await validateScreenshot(page, 'file-required-re-answered')
    })

    it('re-answering question shows continue button but no delete button', async () => {
      // Answer the file upload question
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickNext()

      // Re-open the file upload question
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      await applicantFileQuestion.expectHasContinueButton()
      // A required file upload question should never show a Delete button
      await applicantFileQuestion.expectNoDeleteButton()
    })

    it('continue button does not save new file', async () => {
      // Answer the file upload question
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'some old text',
        'old.txt',
      )
      await applicantQuestions.clickNext()

      // Re-open the file upload question
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      // Upload a new file
      await applicantQuestions.answerFileUploadQuestion(
        'some new text',
        'new.txt',
      )

      // Click "Continue", which does *not* save any new file upload
      // (we may want to change this behavior in the future, but we
      // should still test the existing behavior)
      await applicantFileQuestion.clickContinue()

      // Verify we're taken to the next page (which is the review page
      // since this program only has one block)
      await applicantQuestions.expectReviewPage()
      // Verify the old file is still used
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'old.txt',
      )
      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual('some old text')
    })
  })

  // Optional file upload.
  describe('optional file upload question', () => {
    const programName = 'Test program for optional file upload'
    const fileUploadQuestionText = 'Optional file upload question'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-optional-q',
        questionText: fileUploadQuestionText,
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
      const {page, applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)

      // When the applicant clicks "Save & next"
      await applicantQuestions.clickNext()

      // Then we should still show the error, even for an optional question
      await applicantFileQuestion.expectFileSelectionErrorShown()
      // Verify we're still on the file upload question block
      expect(await page.innerText('.cf-applicant-question-text')).toContain(
        fileUploadQuestionText,
      )
    })

    it('with missing file can be skipped', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantFileQuestion.expectHasSkipButton()

      // When the applicant clicks "Skip"
      await applicantQuestions.clickSkip()

      // Then the question is skipped because file upload question is optional
      // Verify we're taken to the next page (which is the review page
      // since this program only has one block)
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.submitFromReviewPage()
    })

    it('can upload file', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestion('some file', 'file.txt')

      await applicantFileQuestion.expectFileNameDisplayed('file.txt')
    })

    /** Regression test for https://github.com/civiform/civiform/issues/6221. */
    it('can replace file', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestion(
        'some file',
        'file1.txt',
      )
      await applicantFileQuestion.expectFileNameDisplayed('file1.txt')

      await applicantQuestions.answerFileUploadQuestion(
        'some file',
        'file2.txt',
      )
      await applicantFileQuestion.expectFileNameDisplayed('file2.txt')

      await applicantQuestions.clickNext()

      await applicantQuestions.expectReviewPage()
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file2.txt',
      )
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

      // Verify we're taken to the next page (which is the review page since
      // this program only has one block) and can submit the application.
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.submitFromReviewPage()
    })

    it('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })

    it('re-answering question shows previously uploaded file name on review and block pages', async () => {
      // Answer the file upload question
      const {page, applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickNext()

      // Verify the previously uploaded file name is shown on the review page
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'testFileName.txt',
      )

      // Re-open the file upload question
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      // Verify the previously uploaded file name is shown on the block page
      await applicantFileQuestion.expectFileNameDisplayed('testFileName.txt')
      await validateScreenshot(page, 'file-optional-re-answered')
    })

    it('re-answering question shows continue and delete buttons', async () => {
      // Answer the file upload question
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickNext()

      // Re-open the file upload question
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      await applicantFileQuestion.expectHasContinueButton()
      await applicantFileQuestion.expectHasDeleteButton()
    })

    it('delete button removes file and redirects to next block', async () => {
      // Answer the file upload question
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickNext()

      // Re-open the file upload question
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      await applicantFileQuestion.clickDelete()

      // Verify we're taken to the next page (which is the review page
      // since this program only has one block)
      await applicantQuestions.expectReviewPage()

      // Verify the file was deleted so the file upload question is now unanswered
      await applicantQuestions.validateNoPreviouslyAnsweredText(
        fileUploadQuestionText,
      )
    })

    it('continue button does not save new file', async () => {
      // Answer the file upload question
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'some old text',
        'old.txt',
      )
      await applicantQuestions.clickNext()

      // Re-open the file upload question
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      // Upload a new file
      await applicantQuestions.answerFileUploadQuestion(
        'some new text',
        'new.txt',
      )

      // Click "Continue", which does *not* save any new file upload
      // (we may want to change this behavior in the future, but we
      // should still test the existing behavior)
      await applicantFileQuestion.clickContinue()

      // Verify we're taken to the next page (which is the review page
      // since this program only has one block)
      await applicantQuestions.expectReviewPage()
      // Verify the old file is still used
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'old.txt',
      )
      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual('some old text')
    })
  })
})
