import {test, expect} from '@playwright/test'
import {
  createTestContext,
  disableFeatureFlag,
  dropTables,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  seedQuestions,
  validateAccessibility,
  validateScreenshot,
} from './support'
import {BASE_URL} from './support/config'

test.describe('file upload applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  test.beforeAll(async () => {
    const {page} = ctx
    await dropTables(page)
    await seedQuestions(page)
    await page.goto(BASE_URL)
  })

  test.describe('required file upload question', () => {
    const programName = 'Test program for single file upload'
    const fileUploadQuestionText = 'Required file upload question'

    test.beforeAll(async () => {
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

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'file-required')
    })

    test('form is correctly formatted', async () => {
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

    test('does not show errors initially', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx

      await applicantQuestions.applyProgram(programName)

      await applicantFileQuestion.expectFileSelectionErrorHidden()
    })

    test('no continue button initially', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx

      await applicantQuestions.applyProgram(programName)

      await applicantFileQuestion.expectNoContinueButton()
    })

    test('does not show skip button for required question', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx

      await applicantQuestions.applyProgram(programName)

      await applicantFileQuestion.expectNoSkipButton()
    })

    test('can upload file', async () => {
      const {page, applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestion('some file', 'file.txt')

      await applicantFileQuestion.expectFileNameDisplayed('file.txt')
      await validateScreenshot(page, 'file-uploaded')
    })

    /** Regression test for https://github.com/civiform/civiform/issues/6221. */
    test('can replace file', async () => {
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

    test('can download file content', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)
      await applicantQuestions.clickNext()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual(fileContent)
    })

    /** Regression test for https://github.com/civiform/civiform/issues/6516. */
    test('missing file error disappears when file uploaded', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()
      await applicantFileQuestion.expectFileSelectionErrorShown()

      await applicantQuestions.answerFileUploadQuestion('some text')

      await applicantFileQuestion.expectFileSelectionErrorHidden()
    })

    test('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })

    test('re-answering question shows previously uploaded file name on review and block pages', async () => {
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

    test('re-answering question shows continue button but no delete button', async () => {
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
  })

  // Optional file upload.
  test.describe('optional file upload question', () => {
    const programName = 'Test program for optional file upload'
    const fileUploadQuestionText = 'Optional file upload question'

    test.beforeAll(async () => {
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

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'file-optional')
    })

    test('with missing file shows error and does not proceed if Save&next', async () => {
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

    test('with missing file can be skipped', async () => {
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

    test('can upload file', async () => {
      const {applicantQuestions, applicantFileQuestion} = ctx
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestion('some file', 'file.txt')

      await applicantFileQuestion.expectFileNameDisplayed('file.txt')
    })

    /** Regression test for https://github.com/civiform/civiform/issues/6221. */
    test('can replace file', async () => {
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

    test('can download file content', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)
      await applicantQuestions.clickNext()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual(fileContent)
    })

    test('can submit application', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion('some sample text')
      await applicantQuestions.clickNext()

      // Verify we can submit the application
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.submitFromReviewPage()
    })

    test('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })

    test('re-answering question shows previously uploaded file name on review and block pages', async () => {
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

    test('re-answering question shows continue and delete buttons', async () => {
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

    test('delete button removes file and redirects to next block', async () => {
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
  })

  /**
   * Tests for buttons on the file upload question.
   *
   * Fixing https://github.com/civiform/civiform/issues/6450 changes the behavior
   * of the buttons, so for each button we test these behaviors:
   *   1) SAVE_ON_ALL_ACTIONS flag off, don't upload a file, click button
   *   2) SAVE_ON_ALL_ACTIONS flag on, don't upload a file, click button
   *   3) SAVE_ON_ALL_ACTIONS flag off, upload a file, click button
   *   4) SAVE_ON_ALL_ACTIONS flag on, upload a file, click button
   *
   * Note that the optional file upload question has two additional buttons,
   * Skip and Delete. Tests for those buttons are in the
   * 'optional file upload question' test.describe section.
   */
  test.describe('buttons', () => {
    const programName = 'Test program for file upload buttons'
    const emailQuestionText = 'Test email question'
    const fileUploadQuestionText = 'Test file upload question'
    const numberQuestionText = 'Test number question'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      // Create a program with 3 blocks:
      // - Block 1: Optional email question
      // - Block 2: Required file upload question
      // - Block 3: Optional number question
      // Having blocks before and after the file upload question lets us verify
      // the previous and next buttons work correctly.
      // Making the questions optional lets us click "Review" and "Previous"
      // without seeing the "error saving answers" modal, since that modal will
      // trigger if there are validation errors (like missing required questions).
      await adminQuestions.addEmailQuestion({
        questionName: 'email-test-q',
        questionText: emailQuestionText,
      })
      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-buttons-test-q',
        questionText: fileUploadQuestionText,
      })
      await adminQuestions.addNumberQuestion({
        questionName: 'number-test-q',
        questionText: numberQuestionText,
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Email block',
        [],
        'email-test-q',
      )

      await adminPrograms.addProgramBlock(programName, 'File upload block', [
        'file-upload-buttons-test-q',
      ])

      await adminPrograms.addProgramBlock(programName)
      await adminPrograms.goToBlockInProgram(programName, 'Screen 3')
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Number block',
        [],
        'number-test-q',
      )

      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    test.describe('review button', () => {
      test('clicking review without file redirects to review page (flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.clickReview()

        await applicantQuestions.expectReviewPage()
      })

      test('clicking review without file redirects to review page (flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.clickReview()

        await applicantQuestions.expectReviewPage()
      })

      test('clicking review with file discards file and redirects to review page (flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.answerFileUploadQuestion(
          'some sample text',
          'sample.txt',
        )

        await applicantQuestions.clickReview()

        // Verify we're taken to the review page
        await applicantQuestions.expectReviewPage()

        // Verify the file was *not* saved (because the flag is off)
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          fileUploadQuestionText,
        )
      })

      test('clicking review with file saves file and redirects to review page (flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.answerFileUploadQuestion(
          'some sample text',
          'sample.txt',
        )

        await applicantQuestions.clickReview()

        // Verify we're taken to the review page
        await applicantQuestions.expectReviewPage()

        // Verify the file *was* saved (because the flag is on)
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })

    test.describe('previous button', () => {
      test('clicking previous without file redirects to previous page (flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.clickPrevious()

        // Verify we're taken to the previous page, which has the email question
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          emailQuestionText,
        )
      })

      test('clicking previous without file redirects to previous page (flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.clickPrevious()

        // Verify we're taken to the previous page, which has the email question
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          emailQuestionText,
        )
      })

      test('clicking previous with file discards file and redirects to previous page (flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.answerFileUploadQuestion(
          'some sample text',
          'sample.txt',
        )

        await applicantQuestions.clickPrevious()

        // Verify we're taken to the previous page, which has the email question
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          emailQuestionText,
        )

        // Verify the file was *not* saved (because the flag is off)
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          fileUploadQuestionText,
        )
      })

      test('clicking previous with file saves file and redirects to previous page (flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.answerFileUploadQuestion(
          'some sample text',
          'sample.txt',
        )

        await applicantQuestions.clickPrevious()

        // Verify we're taken to the previous page, which has the email question
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          emailQuestionText,
        )

        // Verify the file *was* saved (because the flag is on)
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()

        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })

    test.describe('save & next button', () => {
      test('clicking save&next without file shows error on same page (flag off)', async () => {
        const {page, applicantQuestions, applicantFileQuestion} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        // Don't upload a file, and click Save & next
        await applicantQuestions.clickNext()

        // Verify we're still on the file upload question block and an error is shown
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          fileUploadQuestionText,
        )
        await applicantFileQuestion.expectFileSelectionErrorShown()
        await validateScreenshot(page, 'file-errors')
      })

      test('clicking save&next without file shows error on same page (flag on)', async () => {
        const {page, applicantQuestions, applicantFileQuestion} = ctx
        await enableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        // Don't upload a file, and click Save & next
        await applicantQuestions.clickNext()

        // Verify we're still on the file upload question block and an error is shown
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          fileUploadQuestionText,
        )
        await applicantFileQuestion.expectFileSelectionErrorShown()
      })

      test('clicking save&next with file saves file and redirects to next page (flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.answerFileUploadQuestion(
          'some sample text',
          'sample.txt',
        )

        await applicantQuestions.clickNext()

        // Verify we're taken to the next page
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          numberQuestionText,
        )

        // Verify the file was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })

      test('clicking save&next with file saves file and redirects to next page (flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.answerFileUploadQuestion(
          'some sample text',
          'sample.txt',
        )

        await applicantQuestions.clickNext()

        // Verify we're taken to the next page
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          numberQuestionText,
        )

        // Verify the file was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })

    test.describe('continue button', () => {
      test('clicking continue button redirects to first unseen block', async () => {
        const {page, applicantQuestions, applicantFileQuestion} = ctx

        // Answer the file upload question
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )
        await applicantQuestions.answerFileUploadQuestion(
          'some old text',
          'old.txt',
        )
        await applicantQuestions.clickNext()

        // Re-open the file upload question
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.editQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        // Click "Continue"
        await applicantFileQuestion.clickContinue()

        // When this test re-opens the file upload question from the review page,
        // that puts the application into "review" mode, which means that the next
        // block an applicant should see is the first block that hasn't ever been seen.
        // In this case, because we never opened the very first block with the email
        // question, clicking "Continue" should take us back to that email question.
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          emailQuestionText,
        )

        // Verify the old file is still present
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'old.txt',
        )
      })

      test('clicking continue without new file redirects to next page (flag off)', async () => {
        const {page, applicantQuestions, applicantFileQuestion} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        // First, open the email block so that the email block is considered answered
        // and we're not taken back to it when we click "Continue".
        // (see test case 'clicking continue button redirects to first unseen block').
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickNext()

        // Answer the file upload question
        await applicantQuestions.answerFileUploadQuestion(
          'some old text',
          'old.txt',
        )
        await applicantQuestions.clickNext()

        // Re-open the file upload question
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.editQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        // Click "Continue"
        await applicantFileQuestion.clickContinue()

        // Verify we're taken to the next page
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          numberQuestionText,
        )

        // Verify the old file is still present
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'old.txt',
        )
      })

      test('clicking continue without new file redirects to next page (flag on)', async () => {
        const {page, applicantQuestions, applicantFileQuestion} = ctx
        await enableFeatureFlag(page, 'save_on_all_actions')

        // First, open the email block so that the email block is considered answered
        // and we're not taken back to it when we click "Continue".
        // (see test case 'clicking continue button redirects to first unseen block').
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickNext()

        // Answer the file upload question
        await applicantQuestions.answerFileUploadQuestion(
          'some old text',
          'old.txt',
        )
        // Note: When the 'save_on_all_actions' flag is on, clicking "Save & next" here
        // will take us to the third block. Clicking *any* button on that third block
        // will save our data (because the flag is on), which guarantees that the third
        // block will be marked as seen.
        // Since this test is actually about verifying that clicking "Continue" will
        // take us to the next unseen block, we want the third block to remain unseen.
        // So, we instead click "Review" here to save the file and go to the review page
        // without seeing the third block.
        await applicantQuestions.clickReview()

        // Re-open the file upload question
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.editQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        // Click "Continue"
        await applicantFileQuestion.clickContinue()

        // Verify we're taken to the next page
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          numberQuestionText,
        )

        // Verify the old file is still present
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'old.txt',
        )
      })

      test('clicking continue with new file does *not* save new file and redirects to next page (flag off)', async () => {
        const {page, applicantQuestions, applicantFileQuestion} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        // First, open the email block so that the email block is considered answered
        // and we're not taken back to it when we click "Continue".
        // (see test case 'clicking continue button redirects to first unseen block').
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickNext()

        // Answer the file upload question
        await applicantQuestions.answerFileUploadQuestion(
          'some old text',
          'old.txt',
        )
        await applicantQuestions.clickNext()

        // Re-open the file upload question
        await applicantQuestions.clickReview()
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

        // Verify we're taken to the next page
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          numberQuestionText,
        )

        // Verify the old file is still used
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'old.txt',
        )
        const downloadedFileContent =
          await applicantQuestions.downloadSingleQuestionFromReviewPage()
        expect(downloadedFileContent).toEqual('some old text')
      })

      test('clicking continue with new file does *not* save new file and redirects to next page (flag on)', async () => {
        const {page, applicantQuestions, applicantFileQuestion} = ctx
        await enableFeatureFlag(page, 'save_on_all_actions')

        // First, open the email block so that the email block is considered answered
        // and we're not taken back to it when we click "Continue".
        // (see test case 'clicking continue button redirects to first unseen block').
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickNext()

        // Answer the file upload question
        await applicantQuestions.answerFileUploadQuestion(
          'some old text',
          'old.txt',
        )
        // Note: When the 'save_on_all_actions' flag is on, clicking "Save & next" here
        // will take us to the third block. Clicking *any* button on that third block
        // will save our data (because the flag is on), which guarantees that the third
        // block will be marked as seen.
        // Since this test is actually about verifying that clicking "Continue" will
        // take us to the next unseen block, we want the third block to remain unseen.
        // So, we instead click "Review" here to save the file and go to the review page
        // without seeing the third block.
        await applicantQuestions.clickReview()

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

        // Verify we're taken to the next page
        expect(await page.innerText('.cf-applicant-question-text')).toContain(
          numberQuestionText,
        )

        // Verify the old file is still used
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
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
})
