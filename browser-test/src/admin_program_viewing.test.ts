import {
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'

describe('admin program view page', () => {
  const ctx = createTestContext()

  it('view active program, without draft and after creating draft', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_read_only_view_enabled')

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllPrograms()
    await adminPrograms.gotoViewActiveProgramPage(programName)
    await validateScreenshot(page, 'program-read-only-view')
    await adminPrograms.gotoAdminProgramsPage()
    await validateScreenshot(page, 'program-list-only-one-active-program')
    await adminPrograms.createNewVersion(
      programName,
      /* programReadOnlyViewEnabled = */ true,
    )

    await adminPrograms.gotoViewActiveProgramPage(programName)
    await adminPrograms.gotoAdminProgramsPage()
    await validateScreenshot(page, 'program-list-active-and-draft-program')
  })

  it('view program, view multiple blocks, then start editing', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_read_only_view_enabled')

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName, 'screen 2 description', [])
    await adminPrograms.publishAllPrograms()

    await adminPrograms.gotoViewActiveProgramPage(programName)

    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.gotoToBlockInReadOnlyProgram('2')
    await adminPrograms.expectReadOnlyProgramBlock('2')
    await validateScreenshot(page, 'view-program-block-2')

    await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
    await adminPrograms.expectProgramBlockEditPage(programName)
    await validateScreenshot(page, 'view-program-start-editing')
  })
})
