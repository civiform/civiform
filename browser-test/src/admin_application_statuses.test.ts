import {
  dismissModal,
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
  createBrowserContext,
} from './support'

describe('view program statuses', () => {
  const ctx = createBrowserContext()
  let adminPrograms: AdminPrograms
  let applicantQuestions: ApplicantQuestions
  let adminProgramStatuses: AdminProgramStatuses

  beforeEach(async () => {
    adminPrograms = new AdminPrograms(ctx.page)
    applicantQuestions = new ApplicantQuestions(ctx.page)
    adminProgramStatuses = new AdminProgramStatuses(ctx.page)
  })

  describe('without program statuses', () => {
    const programWithoutStatusesName = 'test program without statuses'
    beforeEach(async () => {
      await loginAsAdmin(ctx.page)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithoutStatusesName)
      await adminPrograms.publishProgram(programWithoutStatusesName)
      await adminPrograms.expectActiveProgram(programWithoutStatusesName)

      await logout(ctx.page)
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(
        programWithoutStatusesName,
      )
      await applicantQuestions.submitFromPreviewPage()

      await logout(ctx.page)

      // Navigate to the submitted application as the program admin.
      await loginAsProgramAdmin(ctx.page)
      await adminPrograms.viewApplications(programWithoutStatusesName)
      await adminPrograms.viewApplicationForApplicant(userDisplayName())
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
    beforeEach(async () => {
      await loginAsAdmin(ctx.page)
      await enableFeatureFlag(ctx.page, 'application_status_tracking_enabled')

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

      await logout(ctx.page)
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage()

      await logout(ctx.page)
      await loginAsProgramAdmin(ctx.page)
      await enableFeatureFlag(ctx.page, 'application_status_tracking_enabled')

      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant(userDisplayName())
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
        await adminPrograms.confirmStatusUpdateModal(modal)
        expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
        await adminPrograms.expectUpdateStatusToast()
        // TODO(#3020): Assert that the selected status has been updated.
      })

      it('when no email is configured for the status, a warning is shown', async () => {
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          noEmailStatusName,
        )
        expect(await modal.innerText()).toContain(
          'will not receive an email because there is no email content set for this status.',
        )
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

      // TODO(#3297): Add a test that the send email checkbox is shown when an applicant has logged
      // in and an email is configured for the status.
    })

    it('allows editing a note', async () => {
      await adminPrograms.editNote('Some note content')
      await adminPrograms.expectNoteUpdatedToast()
      // TODO(#3020): Assert that the note has been updated.
    })
  })
})
