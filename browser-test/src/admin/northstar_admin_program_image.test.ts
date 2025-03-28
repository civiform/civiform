import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  dismissToast,
  loginAsAdmin,
  validateScreenshot,
  validateToastMessage,
} from '../support'
import {Eligibility, ProgramVisibility} from '../support/admin_programs'

test.describe('Admin can manage program image', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await loginAsAdmin(page)
  })

  test(
    'Views program card preview',
    {tag: ['@northstar']},
    async ({page, adminPrograms, adminProgramImage, seeding}) => {
      const programName = 'Test Program'
      const programDescription = 'Test description'
      const shortDescription = 'Short description'

      await test.step('Set up program', async () => {
        await adminPrograms.addProgram(
          programName,
          programDescription,
          shortDescription,
        )

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

      await test.step('Verify preview with program filtering', async () => {
        await enableFeatureFlag(page, 'program_filtering_enabled')

        await seeding.seedProgramsAndCategories()
        await page.goto('/')

        await adminPrograms.addProgram(
          'Test program with tags',
          programDescription,
          shortDescription,
          'https://usa.gov',
          ProgramVisibility.PUBLIC,
          'admin description',
          /* isCommonIntake= */ false,
          'selectedTI',
          'confirmationMessage',
          Eligibility.IS_GATING,
          /* submitNewProgram= */ false,
        )
        await page.getByText('Education').check()
        await page.getByText('Healthcare').check()

        await adminPrograms.submitProgramDetailsEdits()

        await validateScreenshot(
          page.getByRole('listitem'),
          'ns-admin-program-image-card-preview',
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
    },
  )
})

test.describe('image file upload', () => {
  const programName = 'Test program'

  test.beforeEach(async ({page, adminPrograms}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
  })

  test(
    'shows uploaded image before submitting',
    {tag: ['@northstar']},
    async ({page, adminProgramImage}) => {
      await adminProgramImage.setImageFile(
        'src/assets/program-summary-image-wide.png',
      )

      await validateScreenshot(page, 'program-image-with-image-before-save')
    },
  )

  test(
    'deletes existing image',
    {tag: ['@northstar']},
    async ({page, adminProgramImage}) => {
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
    },
  )
})
