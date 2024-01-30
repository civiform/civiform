import {
  createTestContext,
  dismissToast,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  validateToastMessage,
} from './support'

describe('Admin can manage program image', () => {
  const ctx = createTestContext()

  it('views a program without an image', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Program without image'
    await adminPrograms.addProgram(programName)

    await adminPrograms.goToProgramImagePage(programName)

    await validateScreenshot(page, 'program-image-none')
  })

  describe('back button', () => {
    it('back button redirects to block page if came from block page', async () => {
      const {page, adminPrograms, adminProgramImage} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')

      const programName = 'program name'
      await adminPrograms.addProgram(programName)
      // Navigate from edit program blocks page -> edit program image page
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.clickBackButton()

      await adminPrograms.expectProgramBlockEditPage()
    })

    it('back button redirects to details page if came from create program page', async () => {
      const {page, adminPrograms, adminProgramImage} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')

      // After creating a program, admin should be redirected to the program images page
      const programName = 'Back Test Program'
      await adminPrograms.addProgram(programName)
      await adminProgramImage.expectProgramImagePage()

      // WHEN back is clicked
      await adminProgramImage.clickBackButton()

      // THEN the admin goes back to the edit program details page for this new program
      await adminPrograms.expectProgramEditPage(programName)
    })

    it('back button preserves location after interaction', async () => {
      const {page, adminPrograms, adminProgramImage} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')

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

  describe('continue button', () => {
    it('continue button shows if from program creation page', async () => {
      const {page, adminPrograms, adminProgramImage} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')

      const programName = 'Back Test Program'
      await adminPrograms.addProgram(programName)
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.expectHasContinueButton()
      await validateScreenshot(page, 'program-image-with-continue')
    })

    it('continue button redirects to program blocks page', async () => {
      const {page, adminPrograms, adminProgramImage} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')

      const programName = 'Back Test Program'
      await adminPrograms.addProgram(programName)
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectHasContinueButton()

      await adminProgramImage.clickContinueButton()

      await adminPrograms.expectProgramBlockEditPage()
    })

    it('continue button hides if from edit program image page', async () => {
      const {page, adminPrograms, adminProgramImage} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')

      const programName = 'program name'
      await adminPrograms.addProgram(programName)
      // Navigate from edit program blocks page -> edit program image page
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.expectNoContinueButton()
    })
  })

  describe('description', () => {
    const programName = 'Test program'

    beforeEach(async () => {
      const {page, adminPrograms} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    it('sets new description', async () => {
      const {page, adminProgramImage} = ctx

      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('Fake image description')
      await validateToastMessage(
        page,
        adminProgramImage.descriptionUpdatedToastMessage(
          'Fake image description',
        ),
      )

      await dismissToast(page)
      await validateScreenshot(page, 'program-image-with-description')
    })

    it('updates existing description', async () => {
      const {page, adminProgramImage} = ctx
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

    it('removes description with empty', async () => {
      const {page, adminProgramImage} = ctx
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

    it('removes description with blank', async () => {
      const {page, adminProgramImage} = ctx
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

    it('disables translation button when no description', async () => {
      const {adminProgramImage} = ctx

      await adminProgramImage.setImageDescriptionAndSubmit('')
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.expectDisabledTranslationButton()
    })

    it('enables translation button when description exists', async () => {
      const {adminProgramImage} = ctx

      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.expectEnabledTranslationButton()
    })

    it('translation button redirects to translations page', async () => {
      const {adminPrograms, adminProgramImage} = ctx

      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.clickTranslationButton()
      await adminPrograms.expectProgramManageTranslationsPage(programName)
    })
  })

  describe('image file upload', () => {
    const programName = 'Test program'

    beforeEach(async () => {
      const {page, adminPrograms} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    it('form is correctly formatted', async () => {
      const {page} = ctx

      const formInputs = await page
        .locator('#image-file-upload-form')
        .locator('input')
        .all()
      const lastFormInput = formInputs[formInputs.length - 1]

      // AWS requires that the <input type="file"> element to be the last <input> in the <form>
      expect(await lastFormInput.getAttribute('type')).toBe('file')
    })

    it('shows uploaded image before submitting', async () => {
      const {page, adminProgramImage} = ctx

      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )

      await validateScreenshot(page, 'program-image-with-image-before-save')
    })

    it('prevents image upload when no description', async () => {
      const {adminProgramImage} = ctx

      await adminProgramImage.setImageDescriptionAndSubmit('')
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.expectDisabledImageFileUpload()
    })

    it('adds new image', async () => {
      const {page, adminProgramImage} = ctx

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

    it('deletes existing image', async () => {
      const {page, adminProgramImage} = ctx

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
