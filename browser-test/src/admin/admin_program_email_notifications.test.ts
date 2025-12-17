import {test} from '../support/civiform_fixtures'
import {loginAsAdmin} from '../support'
import {ProgramLifecycle} from '../support/admin_programs'

// TODO(#8576): Add tests that emails are actually sent, once #8575 is complete

test.describe('program email notifications', () => {
  test('program admin application submission email preference persists through error', async ({
    page,
    adminPrograms,
  }) => {
    await test.step('create new program and verify default', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(
        '', // empty string will error
        {
          submitNewProgram: false,
        },
      )
      await adminPrograms.expectEmailNotificationPreferenceIsChecked(true)
    })

    await test.step('unselect email notifications and verify it persists through form error', async () => {
      await adminPrograms.setEmailNotificationPreferenceCheckbox(false)
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.expectDisplayNameErrorToast()
      await adminPrograms.expectEmailNotificationPreferenceIsChecked(false)
    })
  })

  test('program admin application submission email preference persists through publish', async ({
    page,
    adminPrograms,
  }) => {
    const programName = 'test program'

    await test.step('create new program and unset notifications', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
    })

    await test.step('unset email preference', async () => {
      await adminPrograms.goToProgramDescriptionPage(programName)
      await adminPrograms.setEmailNotificationPreferenceCheckbox(false)
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
    })

    await test.step('verify unset email preference persists through publish', async () => {
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        ProgramLifecycle.ACTIVE,
      )
      await adminPrograms.expectEmailNotificationPreferenceIsChecked(false)
      await adminPrograms.publishAllDrafts()
    })

    await test.step('set email preference', async () => {
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        ProgramLifecycle.ACTIVE,
      )
      await adminPrograms.setEmailNotificationPreferenceCheckbox(true)
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
    })

    await test.step('verify set email preference persists through publish', async () => {
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        ProgramLifecycle.ACTIVE,
      )
      await adminPrograms.expectEmailNotificationPreferenceIsChecked(true)
    })
  })
})
