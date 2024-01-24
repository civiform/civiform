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
z
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
