import {test, expect} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot, enableFeatureFlag} from '../support'
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

      await test.step('Verify preview with image', async () => {
        await adminProgramImage.setImageFileAndSubmit(
          'src/assets/program-summary-image-wide.png',
          'Alt text',
        )
        await adminProgramImage.expectProgramImagePage()

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
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.clickBackButton()

      await adminPrograms.expectProgramBlockEditPage()
    })

    test('back button redirects to details page if came from create program page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.clickBackButton()

      await adminPrograms.expectProgramEditPage(programName)
    })

    test('back button preserves location after interaction', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      await adminProgramImage.expectProgramImagePage()

      await adminProgramImage.setImageDescriptionAndSubmit('description')
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
        'description',
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

      await adminPrograms.expectProgramBlockEditPage(programName)
    })

    test('continue button hides if from edit program image page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.expectOnProgramImagePageWithEditStatus('EDIT')
      await adminProgramImage.expectNoContinueButton()
      await adminProgramImage.expectHasSaveButton()
    })
  })

  test.describe('description', () => {
    const programName = 'Test program'

    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    test('sets new description', async ({adminProgramImage}) => {
      await adminProgramImage.setImageDescription('Fake image description')

      await adminProgramImage.submitImageDescription()
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('Fake image description')
    })

    test('updates existing description', async ({adminProgramImage}) => {
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
    })

    test('removes description with empty', async ({adminProgramImage}) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('Fake image description')

      await adminProgramImage.setImageDescriptionAndSubmit('')
      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectDescriptionIs('')
    })

    test('removes description with blank', async ({adminProgramImage}) => {
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
    })

    test('shows client error when clearing description to empty with saved image', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
        'Original description',
      )

      await adminProgramImage.setImageDescriptionAndSubmit('')

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()
      await adminProgramImage.expectDescriptionIs('')
    })

    test('shows client error when clearing description to blank with saved image', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
        'Original description',
      )

      await adminProgramImage.setImageDescriptionAndSubmit('      ')

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()
      await adminProgramImage.expectDescriptionIs('      ')
    })

    test('disables save button after save', async ({adminProgramImage}) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.expectDisabledProgramImageFormSubmit()
    })

    test('disables save button when no text change', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.setImageDescription('Fake image description')
      await adminProgramImage.expectDisabledProgramImageFormSubmit()

      await adminProgramImage.setImageDescription('Something different')
      await adminProgramImage.setImageDescription('Fake image description')
      await adminProgramImage.expectDisabledProgramImageFormSubmit()
    })

    test('enables save button when description changes', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.setImageDescription('Something different')
      await adminProgramImage.expectEnabledProgramImageFormSubmit()
    })

    test('enables save button when description cleared', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminProgramImage.setImageDescription('')
      await adminProgramImage.expectEnabledProgramImageFormSubmit()
    })
  })

  test.describe('client validation', () => {
    const programName = 'Test program'

    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    test('shows alt text required when submitting with file and no description', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )

      await adminProgramImage.submitProgramImageForm()

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()
    })

    test('shows alt text required when clearing description to empty', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescription('Original description')

      await adminProgramImage.setImageDescriptionAndSubmit('')

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()
    })

    test('shows alt text required when clearing description to blank', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescription('Original description')

      await adminProgramImage.setImageDescriptionAndSubmit('   ')

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()
    })

    test('shows file too large error and blocks submit', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescription('Alt text')
      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-too-large.png',
      )

      await adminProgramImage.expectTooLargeErrorShown()

      await adminProgramImage.submitProgramImageForm()

      await adminProgramImage.expectProgramImagePage()
    })
  })

  test.describe('image file upload', () => {
    const programName = 'Test program'

    test.beforeEach(async ({adminPrograms}) => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
    })

    test('stays on program image page after submit', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescription('Alt text')
      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )
      await adminProgramImage.submitProgramImageForm()

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectImagePreview()
    })
  })
})
