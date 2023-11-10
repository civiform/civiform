import {
  createTestContext,
  enableFeatureFlag,
  disableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
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
})
