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

    test('save button shows if from program creation page', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await adminPrograms.goToProgramImagePage(programName)

      await adminProgramImage.expectHasContinueButton()
      await adminProgramImage.expectContinueButtonText('Save')
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
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )

      await adminProgramImage.submitProgramImageForm()

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()
    })

    test('shows alt text required when clearing description to empty', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescription('Original description')

      await adminProgramImage.setProgramImageDescriptionAndSubmit('')

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()
    })

    test('shows alt text required when clearing description to blank', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )
      await adminProgramImage.setImageDescription('Original description')

      await adminProgramImage.setProgramImageDescriptionAndSubmit('   ')

      await adminProgramImage.expectProgramImagePage()
      await adminProgramImage.expectAltTextRequiredClientErrorVisible()
    })

    test('shows file too large error and blocks submit', async ({
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescription('Alt text')
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-too-large.png',
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

    test('shows uploaded image before submitting', async ({
      page,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageDescription('Alt text')
      await adminProgramImage.setImageFileFromAssets(
        'program-summary-image-wide.png',
      )

      await validateScreenshot(page, 'program-image-with-image-before-save')
    })

    test('redirects to block edit page on submit', async ({
      adminPrograms,
      adminProgramImage,
    }) => {
      await adminProgramImage.setImageFileAndSave(
        'program-summary-image-wide.png',
        'Alt text',
      )

      await adminPrograms.expectProgramBlockEditPage(programName)
    })
  })
})
