import { AdminPrograms, endSession, loginAsAdmin, startSession, validateScreenshot, } from './support'

describe('Most recently updated program is at top of list.', () => {
  it('sorts by last updated, preferring draft over active', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminPrograms = new AdminPrograms(page)

    const programOne = 'list test program one'
    const programTwo = 'list test program two'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.addProgram(programTwo)

    // Note: CI tests already have test programs
    // available. As such, we only assert the order
    // of the programs added in this test.

    // Most recently added program is on top.
    let programNames = await adminPrograms.programNames()
    expect(programNames.length).toBeGreaterThanOrEqual(2)
    expect(programNames.slice(0, 2)).toEqual([programTwo, programOne])

    // Publish all programs, order should be maintained.
    await adminPrograms.publishAllPrograms()
    programNames = await adminPrograms.programNames()
    expect(programNames.length).toBeGreaterThanOrEqual(2)
    expect(programNames.slice(0, 2)).toEqual([programTwo, programOne])

    // Now create a draft version of the previously last program. After,
    // it should be on top.
    await adminPrograms.createNewVersion(programOne)
    programNames = await adminPrograms.programNames()
    expect(programNames.length).toBeGreaterThanOrEqual(2)
    expect(programNames.slice(0, 2)).toEqual([programOne, programTwo])

    // Now create a new program, which should be on top.
    const programThree = 'list test program three'
    await adminPrograms.addProgram(programThree)
    programNames = await adminPrograms.programNames()
    expect(programNames.length).toBeGreaterThanOrEqual(3)
    expect(programNames.slice(0, 3)).toEqual([
      programThree,
      programOne,
      programTwo,
    ])
    await validateScreenshot(page)

    await endSession(browser)
  })
})
