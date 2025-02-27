import {test, expect} from '../../support/civiform_fixtures'
import {
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {BASE_URL} from '../../support/config'

test.describe('file upload applicant flow', {tag: ['@skip-on-azure']}, () => {
  test.beforeEach(async ({page, seeding}) => {
    await seeding.seedQuestions()
    await page.goto(BASE_URL)
  })

  test.describe('test multiple file upload with max files', () => {
    const programName = 'Test program for multiple file upload'
    const fileUploadQuestionText = 'Required file upload question'

    test('hides upload button at max', async ({
      applicantQuestions,
      applicantFileQuestion,
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      await test.step('Add file upload question and publish', async () => {
        await loginAsAdmin(page)

        await adminQuestions.addFileUploadQuestion({
          questionName: 'file-upload-test-q',
          questionText: fileUploadQuestionText,
          maxFiles: 2,
        })
        await adminPrograms.addAndPublishProgramWithQuestions(
          ['file-upload-test-q'],
          programName,
        )

        await logout(page)
      })

      await applicantQuestions.applyProgram(programName)

      await test.step('Adding maximum files disables file input', async () => {
        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload.png',
        )
        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload-second.png',
        )
        await applicantFileQuestion.expectFileNameDisplayed('file-upload.png')
        await applicantFileQuestion.expectFileNameDisplayed(
          'file-upload-second.png',
        )
        await applicantFileQuestion.expectFileInputDisabled()
      })

      await test.step('Removing a file shows file input again', async () => {
        await applicantFileQuestion.removeFileUpload('file-upload.png')
        await applicantFileQuestion.expectFileInputEnabled()
      })
    })

    test('shows correct hint text based on max files', async ({
      applicantQuestions,
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      await test.step('Add file upload questions and publish', async () => {
        await loginAsAdmin(page)

        await adminQuestions.addFileUploadQuestion({
          questionName: 'file-upload-no-limit',
          questionText: fileUploadQuestionText,
        })

        await adminQuestions.addFileUploadQuestion({
          questionName: 'file-upload-limit',
          questionText: fileUploadQuestionText,
          maxFiles: 1,
        })

        await adminQuestions.addFileUploadQuestion({
          questionName: 'second-file-upload-limit',
          questionText: fileUploadQuestionText,
          maxFiles: 2,
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockUsingSpec(programName, {
          description: 'File upload no limit',
          questions: [{name: 'file-upload-no-limit', isOptional: true}],
        })
        await adminPrograms.addProgramBlockUsingSpec(programName, {
          description: 'File upload with limit 1',
          questions: [{name: 'file-upload-limit', isOptional: true}],
        })
        await adminPrograms.addProgramBlockUsingSpec(programName, {
          description: 'File upload with limit 2',
          questions: [{name: 'second-file-upload-limit', isOptional: true}],
        })

        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(programName)
        await logout(page)
      })

      await applicantQuestions.applyProgram(programName)

      await test.step('Check that text is correct for file upload with no limit set', async () => {
        await expect(
          page.getByText('Select one or more files', {exact: true}),
        ).toBeVisible()
        await applicantQuestions.clickNext()
      })

      await test.step('Check that text is correct for file upload with a max of 1 file set', async () => {
        await expect(
          page.getByText('Select a file', {exact: true}),
        ).toBeVisible()
        await applicantQuestions.clickNext()
      })

      await test.step('Check that text is correct for file upload with a max above 1 set', async () => {
        await expect(
          page.getByText('Select one or more files (maximum of 2)', {
            exact: true,
          }),
        ).toBeVisible()
      })
    })
  })

  test.describe('required file upload question', () => {
    const programName = 'Test program for multiple file upload'
    const fileUploadQuestionText = 'Required file upload question'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
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

    test('validate accessibility and take screenshot', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
      await validateScreenshot(page, 'file-required-multiple-uploads-enabled')
    })

    test('form is correctly formatted', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      const formInputs = await page
        .locator('#cf-block-form')
        .locator('input')
        .all()
      const lastFormInput = formInputs[formInputs.length - 1]

      // AWS requires that the <input type="file"> element to be the last <input> in the <form>
      await expect(lastFormInput).toHaveAttribute('type', 'file')
    })

    test('does not show errors initially', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantFileQuestion.expectFileSelectionErrorHidden()
      await applicantFileQuestion.expectFileTooLargeErrorHidden()
    })

    test('can remove last file of a required question and show error', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )

      await applicantFileQuestion.removeFileUpload('file-upload.png')

      await applicantFileQuestion.expectFileNameCount('file-upload.png', 0)

      await applicantFileQuestion.removeFileUpload('file-upload-second.png')

      await applicantFileQuestion.expectFileNameCount(
        'file-upload-second.png',
        0,
      )

      await applicantQuestions.clickNext()

      await applicantQuestions.expectRequiredQuestionError(
        '.cf-question-fileupload',
      )
    })

    test('can upload multiple files', async ({
      page,
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await test.step('Upload two files', async () => {
        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload.png',
        )
        await applicantFileQuestion.expectFileNameDisplayed('file-upload.png')

        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload-second.png',
        )

        await applicantFileQuestion.expectFileNameDisplayed(
          'file-upload-second.png',
        )

        await validateScreenshot(page, 'file-uploaded-multiple-files')
      })

      await test.step('Upload long file name and validate mobile layout', async () => {
        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload-veryverylongnamethatcouldcauserenderingissuesandhideremovefile.png',
        )

        await applicantFileQuestion.expectFileNameDisplayed(
          'file-upload-veryverylongnamethatcouldcauserenderingissuesandhideremovefile.png',
        )

        await validateScreenshot(
          page.locator('main'),
          'file-uploaded-very-long-name',
          /* fullPage= */ false,
          /* mobileScreenshot= */ true,
        )
      })
    })

    test('review page renders correctly', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )

      await applicantQuestions.clickReview()

      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file-upload.png',
      )

      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file-upload-second.png',
      )
      await validateScreenshot(page.locator('main'), 'file-uploaded-review')
    })

    test('can download file content', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'file 1 content',
        'file1.txt',
      )
      await applicantQuestions.answerFileUploadQuestion(
        'file 2 content',
        'file2.txt',
      )

      await applicantQuestions.clickNext()

      expect(
        await applicantQuestions.downloadFileFromReviewPage('file1.txt'),
      ).toEqual('file 1 content')
      expect(
        await applicantQuestions.downloadFileFromReviewPage('file2.txt'),
      ).toEqual('file 2 content')
    })

    test('re-answering question shows previously uploaded file name on review and block pages', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      // Answer the file upload question
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.clickNext()

      // Verify the previously uploaded file name is shown on the review page
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file-upload.png',
      )

      // Re-open the file upload question
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      // Verify the previously uploaded file name is shown on the block page
      await applicantFileQuestion.expectFileNameDisplayed('file-upload.png')
    })

    test('uploading duplicate file appends suffix', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantFileQuestion.expectFileNameCount('file-upload.png', 1)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantFileQuestion.expectFileNameCount('file-upload.png', 1)
      await applicantFileQuestion.expectFileNameCount('file-upload-2.png', 1)
    })

    test('can remove files', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )

      await applicantFileQuestion.removeFileUpload('file-upload.png')

      await applicantFileQuestion.expectFileNameCount('file-upload.png', 0)

      await applicantFileQuestion.removeFileUpload('file-upload-second.png')

      await applicantFileQuestion.expectFileNameCount(
        'file-upload-second.png',
        0,
      )
    })

    test('remove button has correct aria-labelled by', async ({
      applicantQuestions,
      page,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )

      await expect(page.locator('#uploaded-file-1')).toContainText(
        'file-upload.png',
      )
      await expect(
        page
          .getByRole('list', {name: 'Uploaded files'})
          .locator('li')
          .filter({hasText: 'file-upload.png'})
          .getByText('Remove File'),
      ).toHaveAttribute('aria-labelledby', 'uploaded-file-1')

      await expect(page.locator('#uploaded-file-2')).toContainText(
        'file-upload-second.png',
      )
      await expect(
        page
          .getByRole('list', {name: 'Uploaded files'})
          .locator('li')
          .filter({hasText: 'file-upload-second.png'})
          .getByText('Remove File'),
      ).toHaveAttribute('aria-labelledby', 'uploaded-file-2')
    })

    test('too large file error', async ({
      page,
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await test.step('Shows error when file size is too large', async () => {
        await applicantQuestions.answerFileUploadQuestionWithMbSize(101)

        await applicantFileQuestion.expectFileTooLargeErrorShown()
        await validateScreenshot(page, 'file-error-too-large-multiple-files')
        await validateAccessibility(page)
      })

      await test.step('Cannot save file if too large', async () => {
        await applicantQuestions.clickNext()

        // Verify the file isn't saved and we're still on the file upload question block
        await applicantQuestions.validateQuestionIsOnPage(
          fileUploadQuestionText,
        )
      })

      await test.step('Hides error when smaller file is uploaded', async () => {
        await applicantQuestions.answerFileUploadQuestionWithMbSize(100)

        await applicantFileQuestion.expectFileTooLargeErrorHidden()
      })
    })
  })

  // Optional file upload.
  test.describe('optional file upload question', () => {
    const programName = 'Test program for optional file upload'
    const fileUploadQuestionText = 'Optional file upload question'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
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

    test('form is correctly formatted', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      const formInputs = await page
        .locator('#cf-block-form')
        .locator('input')
        .all()
      const lastFormInput = formInputs[formInputs.length - 1]

      // AWS requires that the <input type="file"> element to be the last <input> in the <form>
      await expect(lastFormInput).toHaveAttribute('type', 'file')
    })

    test('does not show errors initially', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantFileQuestion.expectFileSelectionErrorHidden()
      await applicantFileQuestion.expectFileTooLargeErrorHidden()
    })

    test('can remove last file of a required question and show error', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )

      await applicantFileQuestion.removeFileUpload('file-upload.png')

      await applicantFileQuestion.expectFileNameCount('file-upload.png', 0)

      await applicantFileQuestion.removeFileUpload('file-upload-second.png')

      await applicantFileQuestion.expectFileNameCount(
        'file-upload-second.png',
        0,
      )

      await applicantQuestions.clickNext()
    })

    test('can upload multiple files', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await test.step('Upload two files', async () => {
        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload.png',
        )
        await applicantFileQuestion.expectFileNameDisplayed('file-upload.png')

        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload-second.png',
        )

        await applicantFileQuestion.expectFileNameDisplayed(
          'file-upload-second.png',
        )
      })

      await test.step('Upload long file name and validate mobile layout', async () => {
        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload-veryverylongnamethatcouldcauserenderingissuesandhideremovefile.png',
        )

        await applicantFileQuestion.expectFileNameDisplayed(
          'file-upload-veryverylongnamethatcouldcauserenderingissuesandhideremovefile.png',
        )
      })
    })

    test('review page renders correctly', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )

      await applicantQuestions.clickReview()

      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file-upload.png',
      )

      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file-upload-second.png',
      )
    })

    test('can download file content', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(
        'file 1 content',
        'file1.txt',
      )
      await applicantQuestions.answerFileUploadQuestion(
        'file 2 content',
        'file2.txt',
      )

      await applicantQuestions.clickNext()

      expect(
        await applicantQuestions.downloadFileFromReviewPage('file1.txt'),
      ).toEqual('file 1 content')
      expect(
        await applicantQuestions.downloadFileFromReviewPage('file2.txt'),
      ).toEqual('file 2 content')
    })

    test('re-answering question shows previously uploaded file name on review and block pages', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      // Answer the file upload question
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.clickNext()

      // Verify the previously uploaded file name is shown on the review page
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file-upload.png',
      )

      // Re-open the file upload question
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      // Verify the previously uploaded file name is shown on the block page
      await applicantFileQuestion.expectFileNameDisplayed('file-upload.png')
    })

    test('uploading duplicate file appends suffix', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantFileQuestion.expectFileNameCount('file-upload.png', 1)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantFileQuestion.expectFileNameCount('file-upload.png', 1)
      await applicantFileQuestion.expectFileNameCount('file-upload-2.png', 1)
    })

    test('can remove files', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )

      await applicantFileQuestion.removeFileUpload('file-upload.png')

      await applicantFileQuestion.expectFileNameCount('file-upload.png', 0)

      await applicantFileQuestion.removeFileUpload('file-upload-second.png')

      await applicantFileQuestion.expectFileNameCount(
        'file-upload-second.png',
        0,
      )
    })

    test('remove button has correct aria-labelled by', async ({
      applicantQuestions,
      page,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )

      await expect(page.locator('#uploaded-file-1')).toContainText(
        'file-upload.png',
      )
      await expect(
        page
          .getByRole('list', {name: 'Uploaded files'})
          .locator('li')
          .filter({hasText: 'file-upload.png'})
          .getByText('Remove File'),
      ).toHaveAttribute('aria-labelledby', 'uploaded-file-1')

      await expect(page.locator('#uploaded-file-2')).toContainText(
        'file-upload-second.png',
      )
      await expect(
        page
          .getByRole('list', {name: 'Uploaded files'})
          .locator('li')
          .filter({hasText: 'file-upload-second.png'})
          .getByText('Remove File'),
      ).toHaveAttribute('aria-labelledby', 'uploaded-file-2')
    })
  })

  /**
   * Tests for buttons on the file upload question.
   *
   * For each button, we test:
   *   1) Don't upload a file, click button
   *   2) Upload a file, click button
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

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
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
      test('clicking review without file redirects to review page', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.clickReview()

        await applicantQuestions.expectReviewPage()
      })

      test('clicking review with file saves file and redirects to review page', async ({
        applicantQuestions,
      }) => {
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

        // Verify the file was saved
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })

    test.describe('previous button', () => {
      test('clicking previous without file redirects to previous page', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.clickPrevious()

        // Verify we're taken to the previous page, which has the email question
        await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
      })

      test('clicking previous with file saves file and redirects to previous page', async ({
        applicantQuestions,
      }) => {
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
        await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)

        // Verify the file was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()

        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })

    test.describe('save & next button', () => {
      test('clicking save&next without file shows error on same page', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        // Don't upload a file, and click Save & next
        await applicantQuestions.clickNext()

        // Verify we're still on the file upload question block and an error is shown
        await applicantQuestions.validateQuestionIsOnPage(
          fileUploadQuestionText,
        )

        await expect(
          page.locator('.cf-applicant-question-errors'),
        ).toBeVisible()
      })

      test('clicking save&next with file saves file and redirects to next page', async ({
        applicantQuestions,
      }) => {
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
        await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)

        // Verify the file was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })
  })
})
