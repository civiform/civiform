import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin, logout} from '../support'
import {ProgramLifecycle, ProgramVisibility} from '../support/admin_programs'
import {SAMPLE_PROGRAMS} from '../support/seeding'

test.describe('login only program', () => {
  test('default login only value for any program is false', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await test.step('seed a program and verify default', async () => {
      await seeding.seedProgramsAndCategories()
      await loginAsAdmin(page)
      await adminPrograms.goToProgramDescriptionPage(
        SAMPLE_PROGRAMS.minimal,
        ProgramLifecycle.DRAFT,
      )
      await adminPrograms.expectLoginOnlyProgramIsChecked(false)
    })
  })

  test('login only disabled for external programs', async ({
    page,
    adminPrograms,
  }) => {
    await test.step('create new external program and verify login only is disabled', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addExternalProgram(
        'External Program Name',
        'short program description',
        'https://usa.gov',
        ProgramVisibility.PUBLIC,
      )
      await adminPrograms.goToProgramDescriptionPage(
        'External Program Name',
        ProgramLifecycle.DRAFT,
      )
      await expect(
        page.getByRole('checkbox', {
          name: 'Require applicants to log in to apply to this program',
        }),
      ).toBeDisabled()
    })

    await logout(page)
  })

  test('login only persists through publish', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    const programName = SAMPLE_PROGRAMS.minimal

    await test.step('seed programs', async () => {
      await seeding.seedProgramsAndCategories()
      await loginAsAdmin(page)
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
