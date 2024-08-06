import {test, expect} from '../support/civiform_fixtures'
import {
  dismissToast,
  loginAsAdmin,
  validateScreenshot,
  validateToastMessage,
  validateToastHidden,
} from '../support'

test.describe('Admin can manage program image', () => {
  test('views a program without an image', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programName = 'Program without image'
    await adminPrograms.addProgram(programName)

    await adminPrograms.goToProgramImagePage(programName)

    await validateScreenshot(page, 'program-image-none')
  })

  test.describe('back button', () => {
    test('back button redirects to block page if came from block page', async ({
      page,
      adminPrograms,
      adminProgramImage,
    }) => {
      await loginAsAdmin(page)

      const programName = 'program name'
      await adminPrograms.addProgram(programName)
      // Navigate from edit program blocks page -> edit program image page
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.clickBackButton()

      await adminPrograms.expectProgramBlockEditPage()
    })

    test('back button redirects to details page if came from create program page', async ({
      page,
      adminPrograms,
      adminProgramImage,
    }) => {
      await loginAsAdmin(page)

      // After creating a program, admin should be redirected to the program images page
      const programName = 'Back Test Program'
      await adminPrograms.addProgram(programName)
      await adminProgramImage.expectProgramImagePage()

      // WHEN back is clicked
      await adminProgramImage.clickBackButton()

      // THEN the admin goes back to the edit program details page for this new program
      await adminPrograms.expectProgramEditPage(programName)
    })

    test('back button preserves location after interaction', async ({
      page,
      adminPrograms,
      adminProgramImage,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Back Test Program'
      await adminPrograms.addProgram(programName)
      await adminProgramImage.expectProgramImagePage()

      // When an admin submits an image or description on the page, the page reloads.
      // This test verifies that the redirect URL for the back button is preserved
      // even after those submission and page reloads.
      await adminProgramImage.setImageDescriptionAndSubmit('description')
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )

      await adminProgramImage.clickBackButton()

      await adminPrograms.expectProgramEditPage(programName)
    })
  })

  test.describe('continue button', () => {
    test('continue button shows if from program creation page', async ({
      page,
      adminPrograms,
      adminProgramImage,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Back Test Program'
      await adminPrograms.addProgram(programName)
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.expectHasContinueButton()
    })

    test('continue button redirects to program blocks page', async ({
      page,
      adminPrograms,
      adminProgramImage,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Back Test Program'
      await adminPrograms.addProgram(programName)
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectHasContinueButton()

      await adminProgramImage.clickContinueButton()

      await adminPrograms.expectProgramBlockEditPage()
    })

    test('continue button hides if from edit program image page', async ({
      page,
      adminPrograms,
      adminProgramImage,
    }) => {
      await loginAsAdmin(page)

      const programName = 'program name'
      await adminPrograms.addProgram(programName)
      // Navigate from edit program blocks page -> edit program image page
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.expectNoContinueButton()
    })
  })

  test.describe('description', () => {
    const programName = 'Test program'

    test.beforeEach(async ({page, adminPrograms}) => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    test('sets new description', async ({page, adminProgramImage}) => {
      await adminProgramImage.setImageDescription('Fake image description')

      await adminProgramImage.submitImageDescription()
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('Fake image description')
      await validateToastMessage(
        page,
        adminProgramImage.descriptionUpdatedToastMessage(
          'Fake image description',
        ),
      )

      await dismissToast(page)
      await validateToastHidden(page)
    })

    test('updates existing description', async ({page, adminProgramImage}) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('Fake image description')

      await adminProgramImage.setImageDescriptionAndSubmit(
        'New image description',
      )
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('New image description')
      await validateToastMessage(
        page,
        adminProgramImage.descriptionUpdatedToastMessage(
          'New image description',
        ),
      )
    })

    test('removes description with empty', async ({
      page,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('Fake image description')

      await adminProgramImage.setImageDescriptionAndSubmit('')
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('')
      await validateToastMessage(
        page,
        adminProgramImage.descriptionClearedToastMessage(),
      )
    })

    test('removes description with blank', async ({
      page,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('Fake image description')

      // WHEN a blank description is entered
      await adminProgramImage.setImageDescriptionAndSubmit('   ')
      await adminProgramImage.expectProgramImagePage()

      // We internally set it to completely empty
      await adminProgramImage.expectDescriptionIs('')
      await validateToastMessage(
        page,
        adminProgramImage.descriptionClearedToastMessage(),
      )
    })

    test('does not remove description if image present (description set to empty)', async ({
      page,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Original description',
      )
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )

      await adminProgramImage.setImageDescriptionAndSubmit('')

      await adminProgramImage.expectDescriptionIs('Original description')
      await validateToastMessage(
        page,
        adminProgramImage.descriptionNotClearedToastMessage(),
      )
    })

    test('does not remove description if image present (description set to blank)', async ({
      page,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Original description',
      )
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )

      await adminProgramImage.setImageDescriptionAndSubmit('      ')

      await adminProgramImage.expectDescriptionIs('Original description')
      await validateToastMessage(
        page,
        adminProgramImage.descriptionNotClearedToastMessage(),
      )
    })

    test('can remove description after deleting image', async ({
      adminProgramImage,
    }) => {
      // Set a description and image
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Original description',
      )
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )

      // If the image is deleted
      await adminProgramImage.clickDeleteImageButton()
      await adminProgramImage.confirmDeleteImageButton()

      // Then the description can also be deleted afterwards
      await adminProgramImage.setImageDescriptionAndSubmit('')

      await adminProgramImage.expectDescriptionIs('')
    })

    test('disables submit button after save', async ({adminProgramImage}) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      // On page reload the submit button should be disabled because no changes
      // have been made to the description since submission.
      await adminProgramImage.expectDisabledImageDescriptionSubmit()
    })

    test('disables submit button when no text change', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      // Set an identical description and verify submit is still disabled.
      await adminProgramImage.setImageDescription('Fake image description')
      await adminProgramImage.expectDisabledImageDescriptionSubmit()

      // Set a new description then go back to the original description
      // and verify submit is still disabled.
      await adminProgramImage.setImageDescription('Something different')
      await adminProgramImage.setImageDescription('Fake image description')
      await adminProgramImage.expectDisabledImageDescriptionSubmit()
    })

    test('enables submit button when change', async ({adminProgramImage}) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.setImageDescription('Something different')

      await adminProgramImage.expectEnabledImageDescriptionSubmit()
    })

    test('enables submit button when text removed', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      // Verify that we enable the submit button if the admin
      // updates the description to be empty.
      await adminProgramImage.setImageDescription('')

      await adminProgramImage.expectEnabledImageDescriptionSubmit()
    })

    test('disables translation button when no description', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('')

      await adminProgramImage.expectDisabledTranslationButton()
    })

    test('enables translation button when description exists', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.expectEnabledTranslationButton()
    })

    test('translation button redirects to translations page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.clickTranslationButton()
      await adminPrograms.expectProgramManageTranslationsPage(programName)
    })
  })

  test.describe('image file upload', () => {
    const programName = 'Test program'

    test.beforeEach(async ({page, adminPrograms}) => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    test('form is correctly formatted', async ({page}) => {
      const formInputs = await page
        .locator('#image-file-upload-form')
        .locator('input')
        .all()
      const lastFormInput = formInputs[formInputs.length - 1]

      // AWS requires that the <input type="file"> element to be the last <input> in the <form>
      await expect(lastFormInput).toHaveAttribute('type', 'file')
    })

    test('shows uploaded image before submitting', async ({
      page,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )

      await validateScreenshot(page, 'program-image-with-image-before-save')
    })

    test('prevents image upload when no description', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('')

      await adminProgramImage.expectDisabledImageFileUpload()
      await adminProgramImage.expectDisabledImageFileUploadSubmit()
    })

    test('disables submit button when no image', async ({
      adminProgramImage,
    }) => {
      // The submit button will also be disabled if there's no description,
      // which we don't want to test here. So, set a description to rule
      // that out.
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.expectDisabledImageFileUploadSubmit()
    })

    test('disables submit button when too large image', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-too-large.png',
      )

      await adminProgramImage.expectDisabledImageFileUploadSubmit()
    })

    test('enables submit button when small enough image', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )

      await adminProgramImage.expectEnabledImageFileUploadSubmit()
    })

    test('disables submit button when image removed', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )
      await adminProgramImage.expectEnabledImageFileUploadSubmit()

      await adminProgramImage.setImageFile('')

      await adminProgramImage.expectDisabledImageFileUploadSubmit()
    })

    test('shows error when too large image', async ({
      page,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await dismissToast(page)

      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-too-large.png',
      )

      await adminProgramImage.expectTooLargeErrorShown()
      await expect( page.textContent('html')).toContain(
        'Error: Your file is too large.',
      )
    })

    test('hides error when too large image replaced with smaller image', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-too-large.png',
      )
      await adminProgramImage.expectTooLargeErrorShown()

      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-tall.png',
      )

      await adminProgramImage.expectTooLargeErrorHidden()
    })

    test('adds new image', async ({page, adminProgramImage}) => {
      await adminProgramImage.expectNoImagePreview()
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )
      await adminProgramImage.expectProgramImagePage()
      await validateToastMessage(
        page,
        adminProgramImage.imageUpdatedToastMessage(),
      )

      await dismissToast(page)
      await validateScreenshot(page, 'program-image-with-image')
      await adminProgramImage.expectImagePreview()
    })

    test('deletes existing image', async ({page, adminProgramImage}) => {
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )
      await dismissToast(page)
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectImagePreview()

      await adminProgramImage.clickDeleteImageButton()
      await validateScreenshot(page, 'delete-image-confirmation-modal')

      await adminProgramImage.confirmDeleteImageButton()

      await adminProgramImage.expectProgramImagePage()
      await validateToastMessage(
        page,
        adminProgramImage.imageRemovedToastMessage(),
      )
      await adminProgramImage.expectNoImagePreview()
    })
  })
})
