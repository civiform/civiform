import {
  createTestContext,
  dismissToast,
  enableFeatureFlag,
  disableFeatureFlag,
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

    await disableFeatureFlag(page, 'program_card_images')
  })

  describe('description', () => {
    const programName = 'Test program'

    beforeAll(async () => {
      const {page, adminPrograms} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_card_images')
      await adminPrograms.addProgram(programName)
    })

    beforeEach(async () => {
      await ctx.adminPrograms.goToProgramImagePage(programName)
    })

    afterAll(async () => {
      await disableFeatureFlag(ctx.page, 'program_card_images')
    })

    it('sets new description', async () => {
      const {page, adminProgramImage} = ctx

      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )
      await adminProgramImage.expectProgramImagePage(programName)
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
      await adminProgramImage.expectProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Fake image description')

      await adminProgramImage.setImageDescriptionAndSubmit(
        'New image description',
      )
      await adminProgramImage.expectProgramImagePage(programName)
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
      await adminProgramImage.expectProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Fake image description')

      await adminProgramImage.setImageDescriptionAndSubmit('')
      await adminProgramImage.expectProgramImagePage(programName)
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
      await adminProgramImage.expectProgramImagePage(programName)
      await adminProgramImage.expectDescriptionIs('Fake image description')

      // WHEN a blank description is entered
      await adminProgramImage.setImageDescriptionAndSubmit('   ')
      await adminProgramImage.expectProgramImagePage(programName)

      // We internally set it to completely empty
      await adminProgramImage.expectDescriptionIs('')
      await validateToastMessage(
        page,
        adminProgramImage.descriptionClearedToastMessage(),
      )
    })
  })
})
