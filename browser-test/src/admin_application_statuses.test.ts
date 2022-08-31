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
  AdminProgramStatuses,
  enableFeatureFlag,
  loginAsTestUser,
  testUserDisplayName,
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

      // Submit an application as a guest.
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')
      await applicantQuestions.clickApplyProgramButton(
        programWithoutStatusesName,
      )
      await applicantQuestions.submitFromPreviewPage()
      await logout(pageObject)

      // Navigate to the submitted application as the program admin.
      await loginAsProgramAdmin(pageObject)
    })

    afterAll(async () => {
      await logout(pageObject)
    })

    it('does not show status options', async () => {
      await adminPrograms.viewApplications(programWithoutStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
      expect(await adminPrograms.isStatusSelectorVisible()).toBe(false)
    })

    it('does not show edit note', async () => {
      await adminPrograms.viewApplications(programWithoutStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
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

      // Submit an application as a guest.
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage()
      await logout(pageObject)

      // Submit an application as the logged in test user.
      await loginAsTestUser(pageObject)
      await selectApplicantLanguage(pageObject, 'English')
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage()
      await logout(pageObject)

      await loginAsProgramAdmin(pageObject)
      await enableFeatureFlag(pageObject, 'application_status_tracking_enabled')
    })

    afterAll(async () => {
      await logout(pageObject)
    })

    it('shows status selector', async () => {
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
      expect(await adminPrograms.isStatusSelectorVisible()).toBe(true)
    })

    it('shows default option as placeholder', async () => {
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
      expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
    })

    describe('when a status is changed, a confirmation dialog is shown', () => {
      it('when rejecting, the selected status is not changed', async () => {
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant('Guest')
        await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        await dismissModal(adminPrograms.applicationFrame())
        expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
      })

      it('when confirmed, the page is redirected with a success toast', async () => {
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant('Guest')
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          noEmailStatusName,
        )
        await adminPrograms.confirmStatusUpdateModal(modal)
        expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
        await adminPrograms.expectUpdateStatusToast()
        // TODO(#3020): Assert that the selected status has been updated.
      })

      it('when no email is configured for the status, a warning is shown', async () => {
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant('Guest')
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          noEmailStatusName,
        )
        expect(await modal.innerText()).toContain(
          'will not receive an email because there is no email content set for this status.',
        )
        await dismissModal(adminPrograms.applicationFrame())
      })

      it('when no email is configured for the applicant, a warning is shown', async () => {
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant('Guest')
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          emailStatusName,
        )
        expect(await modal.innerText()).toContain(
          'will not receive an email for this change since they have not provided an email address.',
        )
        await dismissModal(adminPrograms.applicationFrame())
      })

      it('when email is configured for the status and applicant, a checkbox is shown to notify the applicant', async () => {
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          emailStatusName,
        )
        const notifyCheckbox = await modal.$('input[type=checkbox]')
        if (!notifyCheckbox) {
          throw new Error('Expected a checkbox input')
        }
        expect(await notifyCheckbox.isChecked()).toBe(true)
        expect(await modal.innerText()).toContain(' of this change at ')
        await dismissModal(adminPrograms.applicationFrame())
      })
    })

    it('allows editing a note', async () => {
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
      await adminPrograms.editNote('Some note content')
      await adminPrograms.expectNoteUpdatedToast()
      // TODO(#3020): Assert that the note has been updated.
    })
  })
})
