import {
  startSession,
  loginAsAdmin,
  AdminQuestions,
  AdminPrograms,
  endSession,
  sleep,
} from './support'

describe('Most recently updated program is at top of list.', () => {
  it('sorts by last updated, preferring draft over active', async () => {
    const { browser, page } = await startSession()

    // We pause for a brief period between actions
    // since this affects the last updated timestamp.
    // In our client-side sorting, we consider timestamps
    // to be the same if they're within 1 second of each other
    // due to TODO(issue).
    const delayMillis = 1000

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    // Using program names early in the alphabet
    // ensures that they will appear first in the list.
    // This is important when the secondary alphabetical
    // sort is used when all programs have similar last
    // modified timestamps.
    await adminPrograms.addProgram('aaam')
    await sleep(delayMillis)
    await adminPrograms.addProgram('aaaz')
    await sleep(delayMillis)
    await adminPrograms.addProgram('aaaa')
    await sleep(delayMillis)

    // Note: CI tests already have test programs
    // available. As such, we only assert the order
    // of the programs added in this test.

    // Most recently added program is on top.
    let programNames = await adminPrograms.programNames()
    expect(programNames.length).toBeGreaterThanOrEqual(3)
    expect(programNames.slice(0, 3)).toEqual(['aaaa', 'aaaz', 'aaam'])

    // Publish all programs. Since programs have a similar
    // timestamp, the list is sorted alphabetically.
    await adminPrograms.publishAllPrograms()
    await sleep(delayMillis)
    programNames = await adminPrograms.programNames()
    expect(programNames.length).toBeGreaterThanOrEqual(3)
    expect(programNames.slice(0, 3)).toEqual(['aaaa', 'aaam', 'aaaz'])

    // Now create a draft version of the previously middle program, it should be on top.
    await adminPrograms.createNewVersion('aaam')
    await sleep(delayMillis)
    programNames = await adminPrograms.programNames()
    expect(programNames.length).toBeGreaterThanOrEqual(3)
    expect(programNames.slice(0, 3)).toEqual(['aaam', 'aaaa', 'aaaz'])

    // Now create a new program, which should be on top.
    await adminPrograms.addProgram('aaad')
    await sleep(delayMillis)
    programNames = await adminPrograms.programNames()
    expect(programNames.length).toBeGreaterThanOrEqual(4)
    expect(programNames.slice(0, 4)).toEqual(['aaad', 'aaam', 'aaaa', 'aaaz'])

    await endSession(browser)
  })
})
