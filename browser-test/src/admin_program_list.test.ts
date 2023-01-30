import {
  AdminPrograms,
  createTestContext,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
} from './support'

describe('Most recently updated program is at top of list.', () => {
  const ctx = createTestContext()
  it('sorts by last updated, preferring draft over active', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_read_only_view_enabled')

    const programOne = 'List test program one'
    const programTwo = 'List test program two'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.addProgram(programTwo)

    // Most recently added program is on top.
    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    // Publish all programs, order should be maintained.
    await adminPrograms.publishAllPrograms()
    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    // Now create a draft version of the previously last program. After,
    // it should be on top.
    await adminPrograms.createNewVersion(programOne)
    await expectProgramListElements(adminPrograms, [programOne, programTwo])

    // Now create a new program, which should be on top.
    const programThree = 'List test program three'
    await adminPrograms.addProgram(programThree)
    await expectProgramListElements(adminPrograms, [
      programThree,
      programOne,
      programTwo,
    ])
  })

  // TODO(#4125) This test case is a duplicate of the test above and should be removed once the program_read_only_view
  // flag has been removed.
  it('sorts by last updated, preferring draft over active with program read only view disabled', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)
    await disableFeatureFlag(page, 'program_read_only_view_enabled')

    const programOne = 'List test program one'
    const programTwo = 'List test program two'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.addProgram(programTwo)

    // Most recently added program is on top.
    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    // Publish all programs, order should be maintained.
    await adminPrograms.publishAllPrograms()
    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    // Now create a draft version of the previously last program. After,
    // it should be on top.
    await adminPrograms.createNewVersion(
      programOne,
      /* programReadOnlyViewEnabled */ false,
    )
    await expectProgramListElements(adminPrograms, [programOne, programTwo])

    // Now create a new program, which should be on top.
    const programThree = 'List test program three'
    await adminPrograms.addProgram(programThree)
    await expectProgramListElements(adminPrograms, [
      programThree,
      programOne,
      programTwo,
    ])
  })

  async function expectProgramListElements(
    adminPrograms: AdminPrograms,
    expectedPrograms: string[],
  ) {
    if (expectedPrograms.length === 0) {
      throw new Error('expected at least one program')
    }
    const programListNames = await adminPrograms.programNames()
    expect(programListNames).toEqual(expectedPrograms)
  }
})
