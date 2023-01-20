import {
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'

// TODO introduce BeforeAll and possibly several describes in one file https://sourcegraph.com/github.com/civiform/civiform/-/blob/browser-test/src/civiform_admin_program_statuses.test.ts?subtree=true

describe('program viewing', () => {
  const ctx = createTestContext()

  it('view active program, without draft and after creating draft', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_read_only_view_enabled')

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllPrograms()
    await adminPrograms.viewActiveVersion(programName)
    // TODO(jhummel) add screenshot here when the other pull request is submitted
    // await validateScreenshot(page, 'program-read-only-viewer')
    await adminPrograms.gotoAdminProgramsPage()
    await validateScreenshot(page, 'program-list-only-one-active-program')
    await adminPrograms.createNewVersionMaybeReadOnlyViewEnabled(
      programName,
      true,
    )

    await adminPrograms.viewActiveVersion(programName)
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

    await adminPrograms.viewActiveVersion(programName)

    // TODO(jhummel) complete this test before submitting when the read only view is available
    // await adminPrograms.selectProgramBlock('1')
    // TODO(jhummel) expect content of block to show
    // await validateScreenshot(page, 'view_program_block_1')
    // await adminPrograms.selectProgramBlock('2')
    // TODO(jhummel) expect content of block to show
    // await validateScreenshot(page, 'view_program_block_2')
    // await adminPrograms.viewActiveVersionAndStartEditing
    // TODO(jhummel) expect one of the edit elements to show
    // await validateScreenshot(page, 'view_program_start_editing')
  })
})
