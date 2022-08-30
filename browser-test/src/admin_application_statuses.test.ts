import {
  dismissModal,
  startSession,
  logout,
  loginAsGuest,
  loginAsProgramAdmin,
  loginAsAdmin,
  selectApplicantLanguage,
  ApplicantQuestions,
  AdminPrograms,
  userDisplayName,
  AdminProgramStatuses,
  enableFeatureFlag,
} from './support'
import {Page} from 'playwright'

describe('view program statuses', () => {
  let pageObject: Page
  let adminPrograms: AdminPrograms
  let applicantQuestions: ApplicantQuestions
  let adminProgramStatuses: AdminProgramStatuses

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    adminPrograms = new AdminPrograms(pageObject)
    applicantQuestions = new ApplicantQuestions(pageObject)
    adminProgramStatuses = new AdminProgramStatuses(pageObject)
  })

  describe('without program statuses', () => {
    const programWithoutStatusesName = 'test program without statuses'
    beforeAll(async () => {
      await loginAsAdmin(pageObject)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithoutStatusesName)
      await adminPrograms.publishProgram(programWithoutStatusesName)
      await adminPrograms.expectActiveProgram(programWithoutStatusesName)

      await logout(pageObject)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(
        programWithoutStatusesName,
      )
      await applicantQuestions.submitFromPreviewPage()

      await logout(pageObject)

      // Navigate to the submitted application as the program admin.
      await loginAsProgramAdmin(pageObject)
      await adminPrograms.viewApplications(programWithoutStatusesName)
      await adminPrograms.viewApplicationForApplicant(userDisplayName())
    })

    afterAll(async () => {
      await logout(pageObject)
    })

    it('does not show status options', async () => {
      expect(await adminPrograms.isStatusSelectorVisible()).toBe(false)
    })

    it('does not show edit note', async () => {
      expect(await adminPrograms.isEditNoteVisible()).toBe(false)
    })
  })

  describe('with program statuses', () => {
    const programWithStatusesName = 'test program with statuses'
    const noEmailStatusName = 'No email status'
    const emailStatusName = 'Email status'
    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      await enableFeatureFlag(pageObject, 'application_status_tracking_enabled')

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithStatusesName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(
        programWithStatusesName,
      )
      await adminProgramStatuses.createStatus(noEmailStatusName)
      await adminProgramStatuses.createStatus(emailStatusName, {
        emailBody: 'Some email content',
      })
      await adminPrograms.publishProgram(programWithStatusesName)
      await adminPrograms.expectActiveProgram(programWithStatusesName)

      await logout(pageObject)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage()

      await logout(pageObject)
      await loginAsProgramAdmin(pageObject)
      await enableFeatureFlag(pageObject, 'application_status_tracking_enabled')

      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant(userDisplayName())
    })

    afterAll(async () => {
      await logout(pageObject)
    })

    it('shows status selector', async () => {
      expect(await adminPrograms.isStatusSelectorVisible()).toBe(true)
    })

    it('shows default option as placeholder', async () => {
      expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
    })

    describe('when a status is changed, a confirmation dialog is shown', () => {
      it('when rejecting, the selected status is not changed', async () => {
        await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        await dismissModal(adminPrograms.applicationFrame())
        expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
      })

      it('when confirmed, the page is redirected with a success toast', async () => {
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          noEmailStatusName,
        )
        expect(await modal.innerText()).toContain(
          `Status Change: Unset -> ${noEmailStatusName}`,
        )
        await adminPrograms.confirmStatusUpdateModal(modal)
        expect(await adminPrograms.getStatusOption()).toBe(noEmailStatusName)
        await adminPrograms.expectUpdateStatusToast()
      })

      it('when no email is configured for the status, a warning is shown', async () => {
        await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        await dismissModal(adminPrograms.applicationFrame())
      })

      it('when no email is configured for the applicant, a warning is shown', async () => {
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          emailStatusName,
        )
        expect(await modal.innerText()).toContain(
          'will not receive an email for this change since they have not provided an email address.',
        )
        await dismissModal(adminPrograms.applicationFrame())
      })

      it('when changing status, the previous status is shown', async () => {
        expect(await adminPrograms.getStatusOption()).toBe(noEmailStatusName)
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          emailStatusName,
        )
        expect(await modal.innerText()).toContain(
          `Status Change: ${noEmailStatusName} -> ${emailStatusName}`,
        )
        await dismissModal(adminPrograms.applicationFrame())
      })

      // TODO(#3297): Add a test that the send email checkbox is shown when an applicant has logged
      // in and an email is configured for the status.
    })

    it('allows editing a note', async () => {
      await adminPrograms.editNote('Some note content')
      await adminPrograms.expectNoteUpdatedToast()
      // TODO(#3264): Assert that the note has been updated.
    })
  })
})
