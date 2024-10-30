import {test, expect} from '../../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  seedQuestions,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {BASE_URL} from '../../support/config'

test.describe('file upload applicant flow', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await seedQuestions(page)
    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await page.goto(BASE_URL)
  })

  test.describe('required file upload question', () => {
    const programName = 'Test program for single file upload'
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

    test('validate screenshots', async ({
      page,
      applicantFileQuestion,
      applicantQuestions,
    }) => {
      await test.step('Initial rendering screenshot', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantFileQuestion.expectFileSelectionErrorHidden()

        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'file-required-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })

      await test.step('Show required question alert', async () => {
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectRequiredQuestionError(
          '.cf-question-fileupload',
        )

        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'file-required-error-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })
    })

    // TODO(#8143): Remove ".fixme" once https://github.com/civiform/civiform/issues/8143 is fixed
    test.fixme(
      'File too large error',
      async ({applicantFileQuestion, applicantQuestions}) => {
        await test.step('Initially no error is shown', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantFileQuestion.expectFileTooLargeErrorHidden()
        })

        await test.step('Shows error when file size is too large', async () => {
          await applicantQuestions.answerFileUploadQuestionWithMbSize(101)

          await applicantFileQuestion.expectFileTooLargeErrorShown()
          // Don't perform a screenshot here because it shows a spinner that doesn't become stable
          // while the file is uploading.
        })

        await test.step('Cannot save file if too large', async () => {
          await applicantQuestions.clickContinue()

          // Verify the file isn't saved and we're still on the file upload question block
          await applicantQuestions.validateQuestionIsOnPage(
            fileUploadQuestionText,
          )
        })

        await test.step('Hides error when smaller file is uploaded', async () => {
          await applicantQuestions.answerFileUploadQuestionWithMbSize(100)

          await applicantFileQuestion.expectFileTooLargeErrorHidden()
        })
      },
    )

    test('form is correctly formatted', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      const formInputs = await page
        .locator('#cf-block-form')
        .locator('input')
        .all()
      const lastFormInput = formInputs[formInputs.length - 1]

      // AWS requires that the <input type="file"> element to be the last <input> in the <form>
      await expect(lastFormInput).toHaveAttribute('type', 'file')
    })

    test('validate happy upload case', async ({
      page,
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Upload multiple files and validate screenshot', async () => {
        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload.png',
        )

        await applicantFileQuestion.expectFileNameCount('file-upload.png', 1)

        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload-second.png',
        )

        await applicantFileQuestion.expectFileNameCount(
          'file-upload-second.png',
          1,
        )

        await validateScreenshot(
          page,
          'file-uploaded-north-star',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })

      await test.step('uploading duplicate file appends suffix', async () => {
        await applicantQuestions.answerFileUploadQuestionFromAssets(
          'file-upload.png',
        )
        await applicantFileQuestion.expectFileNameCount('file-upload.png', 1)

        await applicantFileQuestion.expectFileNameCount('file-upload-2.png', 1)
      })

      await test.step('Remove files', async () => {
        await applicantFileQuestion.removeFileUpload('file-upload.png')

        await applicantFileQuestion.expectFileNameCount('file-upload.png', 0)

        await applicantFileQuestion.removeFileUpload('file-upload-second.png')

        await applicantFileQuestion.expectFileNameCount(
          'file-upload-second.png',
          0,
        )
      })
    })

    // TODO remove ".fixme" once https://github.com/civiform/civiform/issues/8143 is fixed
    test.fixme(
      'can download file content from multiple files',
      async ({applicantQuestions}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.answerFileUploadQuestion(
          'file 1 content',
          'file1.txt',
        )
        await applicantQuestions.answerFileUploadQuestion(
          'file 2 content',
          'file2.txt',
        )

        await applicantQuestions.clickContinue()

        expect(
          await applicantQuestions.downloadFileFromReviewPage('file1.txt'),
        ).toEqual('file 1 content')
        expect(
          await applicantQuestions.downloadFileFromReviewPage('file2.txt'),
        ).toEqual('file 2 content')
      },
    )

    test('has no accessiblity violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await validateAccessibility(page)
    })

    // TODO(#9082): When CSP issues are fixed, un-skip this test
    test.skip('can upload file', async ({
      page,
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Expect initial state', async () => {
        // Expect does not show errors
        await applicantFileQuestion.expectFileSelectionErrorHidden()
        await applicantFileQuestion.expectFileTooLargeErrorHidden()

        await applicantFileQuestion.expectNoContinueButton()
        await applicantFileQuestion.expectNoSkipButton()
      })

      await test.step('Upload file and validate screenshot', async () => {
        await applicantQuestions.answerFileUploadQuestion(
          'some file',
          'file.txt',
        )

        await applicantFileQuestion.expectFileNameDisplayed('file.txt')
        await validateScreenshot(page, 'file-uploaded')
      })
    })

    /**
     * TODO(#9082): When CSP issues are fixed, un-skip this test
     * Regression test for https://github.com/civiform/civiform/issues/6221.
     */
    test.skip('can replace file', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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

      await applicantQuestions.clickContinue()

      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file2.txt',
      )
    })

    // TODO(#9082): When CSP issues are fixed, un-skip this test
    test.skip('can download file content from a single file question', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)
      await applicantQuestions.clickContinue()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual(fileContent)
    })

    /**
     * TODO(#9082): When CSP issues are fixed, un-skip this test
     * Regression test for https://github.com/civiform/civiform/issues/6516.
     */
    test.skip('missing file error disappears when file uploaded', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.clickContinue()
      await applicantFileQuestion.expectFileSelectionErrorShown()

      await applicantQuestions.answerFileUploadQuestion('some text')

      await applicantFileQuestion.expectFileSelectionErrorHidden()
    })

    // TODO(#9082): When CSP issues are fixed, un-skip this test
    test('too large file error', async ({
      page,
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Shows error when file size is too large', async () => {
        await applicantQuestions.answerFileUploadQuestionWithMbSize(101)

        await applicantFileQuestion.expectFileTooLargeErrorShown()
        await validateScreenshot(page, 'file-error-too-large')
        await validateAccessibility(page)
      })

      await test.step('Cannot save file if too large', async () => {
        await applicantQuestions.clickContinue()

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

    test('has no accessibility violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await validateAccessibility(page)
    })

    // TODO(#9082): When CSP issues are fixed, un-skip this test
    test.skip('re-answering question shows previously uploaded file name on review and block pages', async ({
      page,
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      // Answer the file upload question
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickContinue()

      // Verify the previously uploaded file name is shown on the review page
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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

    // TODO ssandbekkhaug
    test('re-answering question shows continue button but no delete button', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      // Answer the file upload question
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickContinue()

      // Re-open the file upload question
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      await applicantFileQuestion.expectHasContinueButton()
      // A required file upload question should never show a Delete button
      await applicantFileQuestion.expectNoDeleteButton()
    })
  })

  test.describe('test multiple file upload with max files', () => {
    const programName = 'Test program for multiple file upload'
    const fileUploadQuestionText = 'Required file upload question'

    // TODO ssandbekkhaug
    test('hides upload button at max', async ({
      applicantQuestions,
      applicantFileQuestion,
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      await test.step('Add file upload question and publish', async () => {
        await enableFeatureFlag(page, 'multiple_file_upload_enabled')
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

      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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

    // TODO ssandbekkhaug
    test('shows correct hint text based on max files', async ({
      applicantQuestions,
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      await enableFeatureFlag(page, 'multiple_file_upload_enabled')

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

      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Check that text is correct for file upload with no limit set', async () => {
        await expect(
          page.getByText('Select one or more files', {exact: true}),
        ).toBeVisible()
        await applicantQuestions.clickContinue()
      })

      await test.step('Check that text is correct for file upload with a max of 1 file set', async () => {
        await expect(
          page.getByText('Select a file', {exact: true}),
        ).toBeVisible()
        await applicantQuestions.clickContinue()
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

  test.describe('required file upload question with multiple file uploads', () => {
    const programName = 'Test program for multiple file upload'
    const fileUploadQuestionText = 'Required file upload question'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await enableFeatureFlag(page, 'multiple_file_upload_enabled')
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

    // TODO ssandbekkhaug
    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await validateScreenshot(page, 'file-required-multiple-uploads-enabled')
    })

    // TODO ssandbekkhaug
    test('form is correctly formatted', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      const formInputs = await page
        .locator('#cf-block-form')
        .locator('input')
        .all()
      const lastFormInput = formInputs[formInputs.length - 1]

      // AWS requires that the <input type="file"> element to be the last <input> in the <form>
      await expect(lastFormInput).toHaveAttribute('type', 'file')
    })

    // TODO ssandbekkhaug
    test('does not show errors initially', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await applicantFileQuestion.expectFileSelectionErrorHidden()
      await applicantFileQuestion.expectFileTooLargeErrorHidden()
    })

    // TODO ssandbekkhaug
    test('can remove last file of a required question and show error', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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

      await applicantQuestions.clickContinue()

      await applicantQuestions.expectRequiredQuestionError(
        '.cf-question-fileupload',
      )
    })
    // TODO ssandbekkhaug
    test('can upload multiple files', async ({
      page,
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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

    // TODO ssandbekkhaug
    test('review page renders correctly', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
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

    // TODO ssandbekkhaug
    test('re-answering question shows previously uploaded file name on review and block pages', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      // Answer the file upload question
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.clickContinue()

      // Verify the previously uploaded file name is shown on the review page
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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

    // TODO ssandbekkhaug
    test('uploading duplicate file appends suffix', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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

    // TODO ssandbekkhaug
    test('can remove files', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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

    // TODO ssandbekkhaug
    test('remove button has correct aria-labelled by', async ({
      applicantQuestions,
      page,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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

    // TODO remove ".fixme" once https://github.com/civiform/civiform/issues/8143 is fixed
    test.fixme(
      'too large file error',
      async ({page, applicantQuestions, applicantFileQuestion}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('Shows error when file size is too large', async () => {
          await applicantQuestions.answerFileUploadQuestionWithMbSize(101)

          await applicantFileQuestion.expectFileTooLargeErrorShown()
          await validateScreenshot(page, 'file-error-too-large-multiple-files')
          await validateAccessibility(page)
        })

        await test.step('Cannot save file if too large', async () => {
          await applicantQuestions.clickContinue()

          // Verify the file isn't saved and we're still on the file upload question block
          await applicantQuestions.validateQuestionIsOnPage(
            fileUploadQuestionText,
          )
        })

        await test.step('Hides error when smaller file is uploaded', async () => {
          await applicantQuestions.answerFileUploadQuestionWithMbSize(100)

          await applicantFileQuestion.expectFileTooLargeErrorHidden()
        })
      },
    )

    // TODO ssandbekkhaug
    test('has no accessibility violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await validateAccessibility(page)
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

    // TODO ssandbekkhaug
    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await validateScreenshot(page, 'file-optional')
    })

    // TODO ssandbekkhaug
    test('with missing file shows error and does not proceed if Save&next', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      // When the applicant clicks "Save & next"
      await applicantQuestions.clickContinue()

      // Then we should still show the error, even for an optional question
      await applicantFileQuestion.expectFileSelectionErrorShown()
      // Verify we're still on the file upload question block
      await applicantQuestions.validateQuestionIsOnPage(fileUploadQuestionText)
    })

    // TODO ssandbekkhaug
    test('with missing file can be skipped', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantFileQuestion.expectHasSkipButton()

      // When the applicant clicks "Skip"
      await applicantQuestions.clickSkip()

      // Then the question is skipped because file upload question is optional
      // Verify we're taken to the next page (which is the review page
      // since this program only has one block)
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      await applicantQuestions.submitFromReviewPage()
    })

    // TODO ssandbekkhaug
    test('can upload file', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await applicantQuestions.answerFileUploadQuestion('some file', 'file.txt')

      await applicantFileQuestion.expectFileNameDisplayed('file.txt')
    })

    /** Regression test for https://github.com/civiform/civiform/issues/6221. */
    // TODO ssandbekkhaug
    test('can replace file', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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

      await applicantQuestions.clickContinue()

      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        fileUploadQuestionText,
        'file2.txt',
      )
    })

    // TODO ssandbekkhaug
    test('can submit application', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerFileUploadQuestion('some sample text')
      await applicantQuestions.clickContinue()

      // Verify we can submit the application
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      await applicantQuestions.submitFromReviewPage()
    })

    // TODO ssandbekkhaug
    test('has no accessibility violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await validateAccessibility(page)
    })

    // TODO ssandbekkhaug
    test('re-answering question shows previously uploaded file name on review and block pages', async ({
      page,
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      // Answer the file upload question
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickContinue()

      // Verify the previously uploaded file name is shown on the review page
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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

    // TODO ssandbekkhaug
    test('re-answering question shows continue and delete buttons', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      // Answer the file upload question
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickContinue()

      // Re-open the file upload question
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      await applicantFileQuestion.expectHasContinueButton()
      await applicantFileQuestion.expectHasDeleteButton()
    })

    // TODO ssandbekkhaug
    test('delete button removes file and redirects to next block', async ({
      applicantQuestions,
      applicantFileQuestion,
    }) => {
      // Answer the file upload question
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerFileUploadQuestion(
        'some text',
        'testFileName.txt',
      )
      await applicantQuestions.clickContinue()

      // Re-open the file upload question
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      await applicantQuestions.editQuestionFromReviewPage(
        fileUploadQuestionText,
      )

      await applicantFileQuestion.clickDelete()

      // Verify we're taken to the next page (which is the review page
      // since this program only has one block)
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)

      // Verify the file was deleted so the file upload question is now unanswered
      await applicantQuestions.validateNoPreviouslyAnsweredText(
        fileUploadQuestionText,
      )
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
      // TODO ssandbekkhaug
      test('clicking review without file redirects to review page', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        await applicantQuestions.clickReview()

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })

      // TODO ssandbekkhaug
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
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)

        // Verify the file was saved
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })

    test.describe('previous button', () => {
      // TODO ssandbekkhaug
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

      // TODO ssandbekkhaug
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
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)

        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })

    test.describe('save & next button', () => {
      // TODO ssandbekkhaug
      test('clicking save&next without file shows error on same page', async ({
        applicantQuestions,
        applicantFileQuestion,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        // Don't upload a file, and click Save & next
        await applicantQuestions.clickContinue()

        // Verify we're still on the file upload question block and an error is shown
        await applicantQuestions.validateQuestionIsOnPage(
          fileUploadQuestionText,
        )
        await applicantFileQuestion.expectFileSelectionErrorShown()
      })

      // TODO ssandbekkhaug
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

        await applicantQuestions.clickContinue()

        // Verify we're taken to the next page
        await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)

        // Verify the file was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'sample.txt',
        )
      })
    })

    test.describe('continue button', () => {
      // TODO ssandbekkhaug
      test('clicking continue button redirects to first unseen block', async ({
        applicantQuestions,
        applicantFileQuestion,
      }) => {
        // Answer the file upload question
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          fileUploadQuestionText,
        )
        await applicantQuestions.answerFileUploadQuestion(
          'some old text',
          'old.txt',
        )
        await applicantQuestions.clickContinue()

        // Re-open the file upload question
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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
        await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)

        // Verify the old file is still present
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'old.txt',
        )
      })

      // TODO ssandbekkhaug
      test('clicking continue without new file redirects to next page', async ({
        applicantQuestions,
        applicantFileQuestion,
      }) => {
        // First, open the email block so that the email block is considered answered
        // and we're not taken back to it when we click "Continue".
        // (see test case 'clicking continue button redirects to first unseen block').
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.clickContinue()

        // Answer the file upload question
        await applicantQuestions.answerFileUploadQuestion(
          'some old text',
          'old.txt',
        )
        // Note: If we clicked "Save & next" here, we would be taken to the third block.
        // Clicking *any* button on that third block will save our data, which guarantees
        // that the third block will be marked as seen.
        // Since this test is actually about verifying that clicking "Continue" will
        // take us to the next unseen block, we want the third block to remain unseen.
        // So, we instead click "Review" here to save the file and go to the review page
        // without seeing the third block.
        await applicantQuestions.clickReview()

        // Re-open the file upload question
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
        await applicantQuestions.editQuestionFromReviewPage(
          fileUploadQuestionText,
        )

        // Click "Continue"
        await applicantFileQuestion.clickContinue()

        // Verify we're taken to the next page
        await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)

        // Verify the old file is still present
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          fileUploadQuestionText,
          'old.txt',
        )
      })

      // TODO ssandbekkhaug
      test('clicking continue with new file does *not* save new file and redirects to next page', async ({
        applicantQuestions,
        applicantFileQuestion,
      }) => {
        // First, open the email block so that the email block is considered answered
        // and we're not taken back to it when we click "Continue".
        // (see test case 'clicking continue button redirects to first unseen block').
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.clickContinue()

        // Answer the file upload question
        await applicantQuestions.answerFileUploadQuestion(
          'some old text',
          'old.txt',
        )
        // Note: If we clicked "Save & next" here, we would be taken to the third block.
        // Clicking *any* button on that third block will save our data, which guarantees
        // that the third block will be marked as seen.
        // Since this test is actually about verifying that clicking "Continue" will
        // take us to the next unseen block, we want the third block to remain unseen.
        // So, we instead click "Review" here to save the file and go to the review page
        // without seeing the third block.
        await applicantQuestions.clickReview()

        // Re-open the file upload question
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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
        await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)

        // Verify the old file is still used
        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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
