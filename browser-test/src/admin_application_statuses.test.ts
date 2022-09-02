import {
  createTestContext,
  dismissModal,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsGuest,
  loginAsProgramAdmin,
  logout,
  selectApplicantLanguage,
} from './support'

describe('view program statuses', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('without program statuses', () => {
    const programWithoutStatusesName = 'test program without statuses'
    beforeAll(async () => {
      const {page, adminPrograms, applicantQuestions} = ctx
      await loginAsAdmin(page)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithoutStatusesName)
      await adminPrograms.publishProgram(programWithoutStatusesName)
      await adminPrograms.expectActiveProgram(programWithoutStatusesName)

      await logout(page)
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(
        programWithoutStatusesName,
      )
      await applicantQuestions.submitFromPreviewPage()
    })

    beforeEach(async () => {
      const {page, adminPrograms} = ctx
      // Navigate to the submitted application as the program admin.
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programWithoutStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
    })

    it('does not show status options', async () => {
      expect(await ctx.adminPrograms.isStatusSelectorVisible()).toBe(false)
    })

    it('does not show edit note', async () => {
      expect(await ctx.adminPrograms.isEditNoteVisible()).toBe(false)
    })
  })

  describe('with program statuses', () => {
    const programWithStatusesName = 'test program with statuses'
    const noEmailStatusName = 'No email status'
    const emailStatusName = 'Email status'
    beforeAll(async () => {
      const {page, adminPrograms, applicantQuestions, adminProgramStatuses} =
        ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'application_status_tracking_enabled')

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

      await logout(page)
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage()
    })

    beforeEach(async () => {
      const {page, adminPrograms} = ctx
      await loginAsProgramAdmin(page)
      await enableFeatureFlag(page, 'application_status_tracking_enabled')

      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
    })

    it('shows status selector', async () => {
      expect(await ctx.adminPrograms.isStatusSelectorVisible()).toBe(true)
    })

    it('shows default option as placeholder', async () => {
      expect(await ctx.adminPrograms.getStatusOption()).toBe(
        'Choose an option:',
      )
    })

    describe('when a status is changed, a confirmation dialog is shown', () => {
      it('when rejecting, the selected status is not changed', async () => {
        const {adminPrograms} = ctx
        await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        await dismissModal(adminPrograms.applicationFrame())
        expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
      })

      it('when confirmed, the page is redirected with a success toast', async () => {
        const {adminPrograms} = ctx
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
        const {adminPrograms} = ctx
        await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        await dismissModal(adminPrograms.applicationFrame())
      })

      it('when no email is configured for the applicant, a warning is shown', async () => {
        const {adminPrograms} = ctx
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          emailStatusName,
        )
        expect(await modal.innerText()).toContain(
          'will not receive an email for this change since they have not provided an email address.',
        )
        await dismissModal(adminPrograms.applicationFrame())
      })

      it('when changing status, the previous status is shown', async () => {
        const {adminPrograms} = ctx
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
      const {adminPrograms} = ctx
      await adminPrograms.editNote('Some note content')
      await adminPrograms.expectNoteUpdatedToast()
      // TODO(#3264): Assert that the note has been updated.
    })
  })
})
