import {
  createTestContext,
  loginAsAdmin,
  logout,
  selectApplicantLanguage,
  validateScreenshot,
  validateToastMessage,
} from './support'

describe('Admin can manage program image', () => {
  const ctx = createTestContext()

  it('creates a program without an image', async () => {
    const {page, adminPrograms} = ctx

        await loginAsAdmin(page)

        const programName = 'Program without image'
        await adminPrograms.addProgram(programName)

        await adminPrograms.goToProgramImagePage(programName)

        await validateScreenshot(page, 'program-image-none')
  })
}
