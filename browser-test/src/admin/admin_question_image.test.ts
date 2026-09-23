import {test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin} from '../support'

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

    await adminQuestions.addStaticQuestion({questionName})
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Initial state: alt text is empty and dropzone is disabled
    await adminQuestionImage.expectDescription('')
    await adminQuestionImage.expectDropzoneDisabled()
    await adminQuestionImage.expectDeleteButtonHidden()
    await adminQuestionImage.expectNoExistingImageAlert()

    // Entering alt text enables the dropzone
    await adminQuestionImage.setImageDescription('Sample alt text')
    await adminQuestionImage.expectDropzoneEnabled()

    // Clearing alt text disables the dropzone
    await adminQuestionImage.clearImageDescription()
    await adminQuestionImage.expectDropzoneDisabled()
  })

  test('upload-then-save preserves the image', async ({
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'preserve-image-question'
    const altText = 'Preserved alt text'

    await adminQuestions.addStaticQuestion({questionName})
    await adminQuestions.gotoQuestionEditPage(questionName)

    await adminQuestionImage.setImageDescription(altText)
    await adminQuestionImage.uploadImageFile(
      'src/assets/program-summary-image-wide.png',
    )

    // Verify delete button is visible, and existing-image-alert is NOT shown on initial upload
    await adminQuestionImage.expectDeleteButtonVisible()
    await adminQuestionImage.expectNoExistingImageAlert()

    // Save changes
    await adminQuestionImage.submitUpdate()
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()

    // Re-visit the edit page and verify image and alt text are preserved
    await adminQuestions.gotoQuestionEditPage(questionName)
    await adminQuestionImage.expectDescription(altText)
    await adminQuestionImage.expectHasExistingImageAlert()
    await adminQuestionImage.expectDeleteButtonVisible()
    await adminQuestionImage.expectDropzoneEnabled()
  })

  test('delete clears the alt text and resets the dropzone', async ({
    adminQuestions,
    adminQuestionImage,
  }) => {
    const questionName = 'delete-image-question'
    const altText = 'Alt text before delete'

    await adminQuestions.addStaticQuestion({questionName})
    await adminQuestions.gotoQuestionEditPage(questionName)

    await adminQuestionImage.setImageDescription(altText)
    await adminQuestionImage.uploadImageFile(
      'src/assets/program-summary-image-wide.png',
    )
    await adminQuestionImage.submitUpdate()
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()

    await adminQuestions.gotoQuestionEditPage(questionName)
    await adminQuestionImage.expectDescription(altText)
    await adminQuestionImage.expectHasExistingImageAlert()
    await adminQuestionImage.expectDeleteButtonVisible()

    // Delete image and verify immediate UI reset
    await adminQuestionImage.clickDeleteImageButton()
    await adminQuestionImage.expectDescription('')
    await adminQuestionImage.expectNoExistingImageAlert()
    await adminQuestionImage.expectDeleteButtonHidden()
    await adminQuestionImage.expectDropzoneDisabled()

    // Save question and verify deletion persists across reloads
    await adminQuestionImage.submitUpdate()
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()

    await adminQuestions.gotoQuestionEditPage(questionName)
    await adminQuestionImage.expectDescription('')
    await adminQuestionImage.expectNoExistingImageAlert()
    await adminQuestionImage.expectDeleteButtonHidden()
    await adminQuestionImage.expectDropzoneDisabled()
  })
})
