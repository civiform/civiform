import {test} from '../support/civiform_fixtures'
import {loginAsAdmin} from '../support'
import {ProgramLifecycle} from '../support/admin_programs'

test.describe('program login only', () => {
  test('program admin application submission email preference persists through error', async ({
    page,
    adminPrograms,
  }) => {
    await test.step('create new program and verify default', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(
        '', // empty string will error
        'program description',
        'short program description',
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        /* submitNewProgram= */ false,
      )
      await adminPrograms.expectLoginOnlyProgramIsChecked(false)
    })
  })

  test('login only persists through publish', async ({page, adminPrograms}) => {
    const programName = 'test program'

    await test.step('create new program and set login only', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
    })

    await test.step('set login only to true', async () => {
      await adminPrograms.goToProgramDescriptionPage(programName)
      await adminPrograms.setProgramToLoginOnly(true)
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
    })

    await test.step('verify login only through publish', async () => {
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        ProgramLifecycle.ACTIVE,
      )
      await adminPrograms.expectLoginOnlyProgramIsChecked(true)
      await adminPrograms.publishAllDrafts()
    })

    await test.step('set login only to false', async () => {
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        ProgramLifecycle.ACTIVE,
      )
      await adminPrograms.setProgramToLoginOnly(false)
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
    })

    await test.step('verify login only persists through publish', async () => {
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        ProgramLifecycle.ACTIVE,
      )
      await adminPrograms.expectLoginOnlyProgramIsChecked(false)
    })
  })
})
