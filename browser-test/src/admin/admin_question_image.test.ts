import {test, expect} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('Admin can manage question image', () => {
  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(
      page,
      'ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED',
    )
    await enableFeatureFlag(page, 'IMAGES_IN_QUESTION_FEATURE_ENABLED')
  })

  test('alt text gates the file upload dropzone', async ({
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'gate-dropzone-question'

    await test.step('Create question and go to edit page', async () => {
      await adminQuestions.addStaticQuestion({questionName})
      await adminQuestions.gotoQuestionEditPage(questionName)
    })

    await test.step('Verify initial state: alt text empty and dropzone disabled', async () => {
      await adminQuestionImage.expectDescription('')
      await adminQuestionImage.expectDropzoneDisabled()
      await adminQuestionImage.expectDeleteButtonHidden()
      await adminQuestionImage.expectNoExistingImageAlert()
    })

    await test.step('Enter alt text and verify dropzone becomes enabled', async () => {
      await adminQuestionImage.setImageDescription('Sample alt text')
      await adminQuestionImage.expectDropzoneEnabled()
    })

    await test.step('Clear alt text and verify dropzone becomes disabled', async () => {
      await adminQuestionImage.clearImageDescription()
      await adminQuestionImage.expectDropzoneDisabled()
    })
  })

  test('upload-then-save preserves the image', async ({
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'preserve-image-question'
    const altText = 'Preserved alt text'

    await test.step('Create question and go to edit page', async () => {
      await adminQuestions.addStaticQuestion({questionName})
      await adminQuestions.gotoQuestionEditPage(questionName)
    })

    await test.step('Upload image with alt text and verify initial alert is not shown', async () => {
      await adminQuestionImage.setImageDescription(altText)
      await adminQuestionImage.uploadImageFile(
        'src/assets/program-summary-image-wide.png',
      )
      await adminQuestionImage.expectDeleteButtonVisible()
      await adminQuestionImage.expectNoExistingImageAlert()
    })

    await test.step('Verify preview has the image and the alt-text', async () => {
      await adminQuestionImage.expectPreviewImageWithAltText(altText)
    })

    await test.step('Save question edits', async () => {
      await adminQuestionImage.submitUpdate()
      await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    })

    await test.step('Re-visit edit page and verify image and alt text are preserved', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestionImage.expectDescription(altText)
      await adminQuestionImage.expectHasExistingImageAlert()
      await adminQuestionImage.expectExistingImageFileName(
        'program-summary-image-wide.png',
      )
      await adminQuestionImage.expectDeleteButtonVisible()
      await adminQuestionImage.expectDropzoneEnabled()
      await adminQuestionImage.expectPreviewImageWithAltText(altText)
    })
  })

  test('delete clears the alt text and resets the dropzone', async ({
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'delete-image-question'
    const altText = 'Alt text before delete'

    await test.step('Create question, upload image, and save', async () => {
      await adminQuestions.addStaticQuestion({questionName})
      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestionImage.setImageDescription(altText)
      await adminQuestionImage.uploadImageFile(
        'src/assets/program-summary-image-wide.png',
      )
      await adminQuestionImage.submitUpdate()
      await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    })

    await test.step('Re-visit edit page and verify image exists', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestionImage.expectDescription(altText)
      await adminQuestionImage.expectHasExistingImageAlert()
      await adminQuestionImage.expectDeleteButtonVisible()
      await adminQuestionImage.expectPreviewImageWithAltText(altText)
    })

    await test.step('Delete image and verify UI reset immediately', async () => {
      await adminQuestionImage.clickDeleteImageButton()
      await adminQuestionImage.expectDescription('')
      await adminQuestionImage.expectNoExistingImageAlert()
      await adminQuestionImage.expectDeleteButtonHidden()
      await adminQuestionImage.expectDropzoneDisabled()
      await adminQuestionImage.expectNoPreviewImage()
    })

    await test.step('Save update and verify deletion persists across reloads', async () => {
      await adminQuestionImage.submitUpdate()
      await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()

      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestionImage.expectDescription('')
      await adminQuestionImage.expectNoExistingImageAlert()
      await adminQuestionImage.expectDeleteButtonHidden()
      await adminQuestionImage.expectDropzoneDisabled()
      await adminQuestionImage.expectNoPreviewImage()
    })
  })

  test('upload, publish, and roundtrip preserves image key and alt text', async ({
    adminPrograms,
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'roundtrip-image-question'
    const programName = 'Roundtrip Image Program'
    const altText = 'Roundtrip alt text'

    await test.step('Create question and add to program block', async () => {
      await adminQuestions.addStaticQuestion({questionName})
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'block description',
        questions: [{name: questionName}],
      })
    })

    await test.step('Edit question from program, upload image, and submit update', async () => {
      await adminPrograms.editQuestion(questionName)
      await adminQuestions.expectQuestionEditPage(questionName)

      await adminQuestionImage.setImageDescription(altText)
      await adminQuestionImage.uploadImageFile(
        'src/assets/program-summary-image-wide.png',
      )
      await adminQuestionImage.expectDeleteButtonVisible()
      await adminQuestionImage.expectPreviewImageWithAltText(altText)
      await adminQuestionImage.expectNoExistingImageAlert()

      await adminQuestionImage.submitUpdate()
      await adminPrograms.expectProgramBlockEditPage(programName)
    })

    await test.step('Publish program and all drafts', async () => {
      await adminPrograms.publishProgram(programName)
    })

    await test.step('Create new draft version of program and navigate back to edit question', async () => {
      await adminPrograms.createNewVersion(programName)
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.editQuestion(questionName)
      await adminQuestions.expectQuestionEditPage(questionName)
    })

    await test.step('Verify image key and alt text survive the publishing round-trip', async () => {
      await adminQuestionImage.expectDescription(altText)
      await expect(adminQuestionImage.getImageUploadInput()).toHaveAttribute(
        'data-has-existing-image',
        'true',
      )
      await adminQuestionImage.expectHasExistingImageAlert()
      await adminQuestionImage.expectExistingImageFileName(
        'program-summary-image-wide.png',
      )
      await adminQuestionImage.expectDeleteButtonVisible()
      await adminQuestionImage.expectPreviewImageWithAltText(altText)
      await adminQuestionImage.expectDropzoneEnabled()
    })
  })

  test('preview dynamically updates when image and alt text are replaced', async ({
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'dynamic-preview-question'
    const initialAltText = 'First image alt text'
    const updatedAltText = 'Updated second image alt text'

    await test.step('Create question and go to edit page', async () => {
      await adminQuestions.addStaticQuestion({questionName})
      await adminQuestions.gotoQuestionEditPage(questionName)
    })

    await test.step('Upload initial image and verify preview shows it', async () => {
      await adminQuestionImage.setImageDescription(initialAltText)
      await adminQuestionImage.uploadImageFile(
        'src/assets/program-summary-image-wide.png',
      )
      await adminQuestionImage.expectPreviewImageWithAltText(initialAltText)
    })

    await test.step('Upload replacement image with updated alt text', async () => {
      await adminQuestionImage.setImageDescription(updatedAltText)
      await adminQuestionImage.uploadImageFile(
        'src/assets/program-summary-image-tall.png',
      )
    })

    await test.step('Verify preview reflects the new image and new alt text', async () => {
      await adminQuestionImage.expectPreviewImageWithAltText(updatedAltText)
    })
  })

  test('preview renders image when added to an active (published) question', async ({
    adminPrograms,
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'active-q-preview-image'
    const programName = 'Active Question Program'
    const altText = 'Alt text added to active question'

    await test.step('Create static question, add to program, and publish so it is ACTIVE', async () => {
      await adminQuestions.addStaticQuestion({questionName})
      await adminPrograms.addAndPublishProgramWithQuestions(
        [questionName],
        programName,
      )
    })

    await test.step('Navigate to edit page of the active question', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestionImage.expectNoPreviewImage()
    })

    await test.step('Upload image with alt text (creates a draft question row)', async () => {
      await adminQuestionImage.setImageDescription(altText)
      await adminQuestionImage.uploadImageFile(
        'src/assets/program-summary-image-wide.png',
      )
    })

    await test.step('Verify live preview renders the image despite starting from an active question', async () => {
      await adminQuestionImage.expectPreviewImageWithAltText(altText)
    })
  })

  test('question preview with image snapshot', async ({
    page,
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'snapshot-preview-question'
    const altText = 'Sample tax form example'

    await test.step('Create question and go to edit page', async () => {
      await adminQuestions.addStaticQuestion({questionName})
      await adminQuestions.gotoQuestionEditPage(questionName)
    })

    await test.step('Upload image with alt text', async () => {
      await adminQuestionImage.setImageDescription(altText)
      await adminQuestionImage.uploadImageFile(
        'src/assets/program-summary-image-wide.png',
      )
    })

    await test.step('Verify image preview and validate screenshot', async () => {
      await adminQuestionImage.expectPreviewImageWithAltText(altText)
      await validateScreenshot(
        page.getByRole('main'),
        'question-preview-with-image',
        {fullPage: false},
      )
    })
  })
})
