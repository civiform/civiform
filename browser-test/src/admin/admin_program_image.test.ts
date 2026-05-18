import {test, expect} from '../support/civiform_fixtures'
import {
  dismissToast,
  loginAsAdmin,
  validateScreenshot,
  validateToastMessage,
  validateToastHidden,
  enableFeatureFlag,
} from '../support'
import {Eligibility} from '../support/admin_programs'

test.describe('Admin can manage program image', () => {
  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'file_upload_question_improvements_enabled')
  })

  test.describe('Program card preview', () => {
    test('Views program card preview', async ({
      page,
      adminPrograms,
      adminProgramImage,
      seeding,
    }) => {
      const programName = 'Test Program'
      const programDescription = 'Test description'
      const shortDescription = 'Short description'

      await test.step('Set up program', async () => {
        await adminPrograms.addProgram(programName, {
          description: programDescription,
          shortDescription: shortDescription,
        })

        await adminPrograms.goToProgramImagePage(programName)

        await validateScreenshot(page, 'program-image-none')
      })

      await test.step('Verify preview without image', async () => {
        await adminProgramImage.expectNoImagePreview()
        await adminProgramImage.expectProgramPreviewCard(
          programName,
          programDescription,
          shortDescription,
        )
      })

      // @TODO: Re-enable this step after image upload is implemented
      await test.step.skip('Verify preview with image', async () => {
        await adminProgramImage.setImageFileFromAssets(
          'program-summary-image-wide.png',
        )
        await adminProgramImage.expectProgramImagePage()

        await validateToastMessage(
          page,
          adminProgramImage.imageUpdatedToastMessage(),
        )
        await dismissToast(page)

        await adminProgramImage.expectImagePreview()
        await adminProgramImage.expectProgramPreviewCard(
          programName,
          programDescription,
          shortDescription,
        )

        await validateScreenshot(
          page.getByRole('main'),
          'program-image-preview',
        )
      })

      await test.step('Verify preview with category tags', async () => {
        const programNameWithTags = 'Test program with tags'
        await seeding.seedProgramsAndCategories()
        await page.goto('/')

        await adminPrograms.addProgram(programNameWithTags, {
          description: programDescription,
          shortDescription: shortDescription,
          eligibility: Eligibility.IS_GATING,
          submitNewProgram: false,
        })
        await page.getByText('Education').check()
        await page.getByText('Healthcare').check()

        await adminPrograms.submitProgramDetailsEdits()

        await validateScreenshot(
          page.getByRole('listitem').filter({hasText: programNameWithTags}),
          'admin-program-image-card-preview-with-tags',
        )

        await adminProgramImage.expectNoImagePreview()
        await adminProgramImage.expectProgramPreviewCard(
          programNameWithTags,
          programDescription,
          shortDescription,
        )

        await expect(page.getByText('Education')).toBeVisible()
        await expect(page.getByText('Healthcare')).toBeVisible()
      })
    })
  })

  test.describe('back button', () => {
    const programName = 'Back Test Program'

    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
    })

    test('back button redirects to block page if came from block page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      // Navigate from edit program blocks page -> edit program image page
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.clickBackButton()

      await adminPrograms.expectProgramBlockEditPage()
    })

    test('back button redirects to details page if came from create program page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      // After creating a program, admin should be redirected to the program images page
      await adminProgramImage.expectProgramImagePage()

      // WHEN back is clicked
      await adminProgramImage.clickBackButton()

      // THEN the admin goes back to the edit program details page for this new program
      await adminPrograms.expectProgramEditPage(programName)
    })
  })

  test.describe('continue button', () => {
    const programName = 'Back Test Program'

    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
    })

    test('continue button shows if from program creation page', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.expectHasContinueButton()

      await adminProgramImage.expectContinueButtonText('Continue')
    })

    test('continue button redirects to program edit page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectHasContinueButton()

      await adminProgramImage.clickContinueButton()

      await adminPrograms.expectProgramEditPage(programName)
    })

    test('save button shows if from program creation page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      // Navigate from edit program blocks page -> edit program image page
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.expectHasContinueButton()
      await adminProgramImage.expectContinueButtonText('Save')
    })
  })

  test.describe('description', () => {
    const programName = 'Test program'

    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    test('sets new description', async ({
      page,
      adminProgramImage,
      adminPrograms,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescription('Fake image description')

      await adminProgramImage.submitImageDescription()
      await adminPrograms.expectProgramBlockEditPage(programName)
      await validateToastMessage(
        page,
        adminProgramImage.descriptionUpdatedToastMessage(
          'Fake image description',
        ),
      )

      await dismissToast(page)
      await validateToastHidden(page)
    })

    test('updates existing description', async ({
      page,
      adminProgramImage,
      adminPrograms,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminPrograms.expectProgramBlockEditPage(programName)
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.expectDescriptionIs('Fake image description')
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )

      await adminProgramImage.setImageDescriptionAndSubmit(
        'New image description',
      )
      await adminPrograms.expectProgramBlockEditPage(programName)
      await validateToastMessage(
        page,
        adminProgramImage.descriptionUpdatedToastMessage(
          'New image description',
        ),
      )
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('New image description')
    })

    test('blocks clearing description with empty after file upload (alt required)', async ({
      page,
      adminProgramImage,
      adminPrograms,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminPrograms.expectProgramBlockEditPage(programName)
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Fake image description')

      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit('')
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Fake image description')
    })

    test('blocks clearing description with blank after file upload (alt required)', async ({
      page,
      adminProgramImage,
      adminPrograms,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminPrograms.expectProgramBlockEditPage(programName)
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Fake image description')

      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit('   ')
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Fake image description')
    })

    test('does not remove description if image present (description set to empty)', async ({
      page,
      adminProgramImage,
      adminPrograms,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Original description',
      )
      await adminPrograms.expectProgramBlockEditPage(programName)
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )

      await adminProgramImage.setImageDescriptionAndSubmit('')

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Original description')
    })

    test('does not remove description if image present (description set to blank)', async ({
      page,
      adminProgramImage,
      adminPrograms,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Original description',
      )
      await adminPrograms.expectProgramBlockEditPage(programName)
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )

      await adminProgramImage.setImageDescriptionAndSubmit('      ')

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Original description')
    })

    // TODO: Unskip when ProgramImageFragment exposes delete-image (legacy view has it).
    test.skip('can remove description after deleting image', async ({
      page,
      adminProgramImage,
      adminPrograms,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Original description',
      )
      await adminPrograms.expectProgramBlockEditPage(programName)
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )

      await adminProgramImage.clickDeleteImageButton()
      await adminProgramImage.confirmDeleteImageButton()

      await adminProgramImage.setImageDescriptionAndSubmit('')
      await adminPrograms.expectProgramBlockEditPage(programName)
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('')
    })

    test('disables submit button when no text change and enables when text is changed', async ({
      page,
      adminProgramImage,
      adminPrograms,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminPrograms.expectProgramBlockEditPage(programName)
      await dismissToast(page)

      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescription('Fake image description')
      await adminProgramImage.expectDisabledImageDescriptionSubmit()

      await adminProgramImage.setImageDescription('Something different')
      await adminProgramImage.expectEnabledImageDescriptionSubmit()
    })
  })

  test.describe('image file upload', () => {
    const programName = 'Test program'

    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    test('shows uploaded image before submitting', async ({
      page,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )

      await validateScreenshot(page, 'program-image-with-image-before-save')
    })

    // TODO: Unskip when ProgramImageFragment implements delete-image
    test.skip('deletes existing image', async ({page, adminProgramImage}) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescriptionAndSubmit('Test description')
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
