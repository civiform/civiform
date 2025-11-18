import {test} from '../support/civiform_fixtures'
import {loginAsAdmin} from '../support'
import {ProgramLifecycle} from '../support/admin_programs'

test.describe('login only program', () => {
  test('default login only value for any program is false', async ({
    page,
    adminPrograms,
  }) => {
    await test.step('create new program and verify default', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram('default program')
      await adminPrograms.expectLoginOnlyProgramIsChecked(false)
    })
  })

  test('login only persists through publish', async ({page, adminPrograms}) => {
    const programName = 'test program'

    await test.step('create new program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
    })

    await test.step('set login only to true and publish', async () => {
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
