import {test, expect} from '../support/civiform_fixtures'
import {
  dismissToast,
  loginAsAdmin,
  validateScreenshot,
  validateToastMessage,
  validateToastHidden,
} from '../support'
import {Eligibility} from '../support/admin_programs'

test.describe('Admin can manage program image', () => {
  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
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

      await test.step('Verify preview with image', async () => {
        await adminProgramImage.setImageFileAndSubmit(
          'src/assets/program-summary-image-wide.png',
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
        await seeding.seedProgramsAndCategories()
        await page.goto('/')

        await adminPrograms.addProgram('Test program with tags', {
          description: programDescription,
          shortDescription: shortDescription,
          eligibility: Eligibility.IS_GATING,
          submitNewProgram: false,
        })
        await page.getByText('Education').check()
        await page.getByText('Healthcare').check()

        await adminPrograms.submitProgramDetailsEdits()

        await validateScreenshot(
          page
            .getByRole('listitem')
            .filter({hasText: 'Test program with tags'}),
          'admin-program-image-card-preview-with-tags',
        )

        await adminProgramImage.expectNoImagePreview()
        await adminProgramImage.expectProgramPreviewCard(
          programName,
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

    test('back button preserves location after interaction', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
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
    const programName = 'Back Test Program'
    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
    })
    test('continue button shows if from program creation page', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.expectHasContinueButton()
    })

    test('continue button redirects to program blocks page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectHasContinueButton()

      await adminProgramImage.clickContinueButton()

      await adminPrograms.expectProgramBlockEditPage()
    })

    test('continue button hides if from edit program image page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      // Navigate from edit program blocks page -> edit program image page
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.expectNoContinueButton()
    })
  })

  test.describe('description', () => {
    const programName = 'Test program'

    test.beforeEach(async ({adminPrograms}) => {
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

    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
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
