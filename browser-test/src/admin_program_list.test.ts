import {AdminPrograms, createTestContext, loginAsAdmin} from './support'

describe('Most recently updated program is at top of list.', () => {
  const ctx = createTestContext()
  it('sorts by last updated, preferring draft over active', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)

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

  async function expectProgramListElements(
    adminPrograms: AdminPrograms,
    expectedPrograms: string[],
  ) {
    if (!expectedPrograms) {
      throw new Error('expected at least one program')
    }
    const programListNames = await adminPrograms.programNames()
    expect(programListNames).toEqual(expectedPrograms)
  }
})
