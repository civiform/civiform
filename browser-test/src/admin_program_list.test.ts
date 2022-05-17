import {
  startSession,
  loginAsAdmin,
  AdminQuestions,
  AdminPrograms,
  endSession,
} from './support'

describe('Most recently updated program is at top of list.', () => {
  it('sorts by last updated, preferring draft over active', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    const programOne = 'zzz program'
    const programTwo = 'aaa program'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.addProgram(programTwo)

    // Most recently added program is on top.
    expect(await adminPrograms.programNames()).toEqual([programTwo, programOne])

    // Publish all programs, the order should remain the same.
    await adminPrograms.publishAllPrograms()
    expect(await adminPrograms.programNames()).toEqual([programTwo, programOne])

    // Now create a draft version of the previously last program, it should be on top.
    await adminPrograms.createNewVersion(programOne)
    expect(await adminPrograms.programNames()).toEqual([programOne, programTwo])

    // Now create a new program, which should be on top.
    const programThree = 'mmm program'
    await adminPrograms.addProgram(programThree)
    expect(await adminPrograms.programNames()).toEqual([programThree, programOne, programTwo])

    await endSession(browser)
  })
})
