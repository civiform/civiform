import {
  AdminPrograms,
  createTestContext,
  dismissModal,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsGuest,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  supportsEmailInspection,
  testUserDisplayName,
  extractEmailsForRecipient,
} from './support'

describe('view program statuses', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('without program statuses', () => {
    const programWithoutStatusesName = 'test-program-without-statuses'
    beforeAll(async () => {
      const {page, adminPrograms, applicantQuestions} = ctx
      await loginAsAdmin(page)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithoutStatusesName)
      await adminPrograms.publishProgram(programWithoutStatusesName)
      await adminPrograms.expectActiveProgram(programWithoutStatusesName)
      await logout(page)

      // Submit an application as a guest.
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
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

    it('does not show application status in list', async () => {
      const {page, adminPrograms} = ctx
      expect(
        await page.innerText(
          adminPrograms.selectApplicationCardForApplicant('Guest'),
        ),
      ).not.toContain('Status: ')
    })

    it('does not show edit note', async () => {
      expect(await ctx.adminPrograms.isEditNoteVisible()).toBe(false)
    })
  })

  describe('with program statuses', () => {
    const programWithStatusesName = 'test-program-with-statuses'
    const noEmailStatusName = 'No email status'
    const emailStatusName = 'Email status'
    const emailBody = 'Some email content'

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
        emailBody: emailBody,
      })
      await adminPrograms.publishProgram(programWithStatusesName)
      await adminPrograms.expectActiveProgram(programWithStatusesName)
      await logout(page)

      // Submit an application as a guest.
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage()
      await logout(page)

      // Submit an application as the logged in test user.
      await loginAsTestUser(page)
      await selectApplicantLanguage(page, 'English')
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage()
      await logout(page)
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

    it('shows default status value in application list if no status is set', async () => {
      const {page, adminPrograms} = ctx
      await adminPrograms.viewApplications(programWithStatusesName)
      expect(
        await page.innerText(
          adminPrograms.selectApplicationCardForApplicant('Guest'),
        ),
      ).toContain('Status: None')
    })

    describe('when a status is changed, a confirmation dialog is shown', () => {
      it('when rejecting, the selected status is not changed', async () => {
        const {adminPrograms} = ctx
        await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        await dismissModal(adminPrograms.applicationFrame())
        expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
      })

      it('when confirmed, the page is redirected with a success toast and preserves the selected application', async () => {
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

        // Confirm that the application is shown after reloading the page.
        const applicationText = await adminPrograms
          .applicationFrameLocator()
          .locator('#application-view')
          .innerText()
        expect(applicationText).toContain('Guest')
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

      it('when changing status, the updated application status is reflected in the application list', async () => {
        const {page, adminPrograms} = ctx
        expect(
          await page.innerText(
            adminPrograms.selectApplicationCardForApplicant('Guest'),
          ),
        ).toContain(`Status: ${noEmailStatusName}`)
        const modal = await adminPrograms.setStatusOptionAndAwaitModal(
          emailStatusName,
        )
        await adminPrograms.confirmStatusUpdateModal(modal)
        expect(
          await page.innerText(
            adminPrograms.selectApplicationCardForApplicant('Guest'),
          ),
        ).toContain(`Status: ${emailStatusName}`)
      })

      describe('when email is configured for the status and applicant, a checkbox is shown to notify the applicant', () => {
        beforeEach(async () => {
          const {adminPrograms} = ctx
          await adminPrograms.viewApplications(programWithStatusesName)
          await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
        })

        it('checkbox is checked by default and email is sent', async () => {
          const {page, adminPrograms} = ctx
          const emailsBefore = supportsEmailInspection()
            ? await extractEmailsForRecipient(page, testUserDisplayName())
            : []
          const modal = await adminPrograms.setStatusOptionAndAwaitModal(
            emailStatusName,
          )
          const notifyCheckbox = await modal.$('input[type=checkbox]')
          if (!notifyCheckbox) {
            throw new Error('Expected a checkbox input')
          }
          expect(await notifyCheckbox.isChecked()).toBe(true)
          expect(await modal.innerText()).toContain(' of this change at ')
          await adminPrograms.confirmStatusUpdateModal(modal)
          expect(await adminPrograms.getStatusOption()).toBe(emailStatusName)
          await adminPrograms.expectUpdateStatusToast()

          if (supportsEmailInspection()) {
            const emailsAfter = await extractEmailsForRecipient(
              page,
              testUserDisplayName(),
            )
            expect(emailsAfter.length).toEqual(emailsBefore.length + 1)
            const sentEmail = emailsAfter[emailsAfter.length - 1]
            expect(sentEmail.Subject).toEqual(
              `An update on your application ${programWithStatusesName}`,
            )
            expect(sentEmail.Body.text_part).toContain(emailBody)
          }
        })

        it('choosing not to notify applicant changes status and does not send email', async () => {
          const {page, adminPrograms} = ctx
          const emailsBefore = supportsEmailInspection()
            ? await extractEmailsForRecipient(page, testUserDisplayName())
            : []
          const modal = await adminPrograms.setStatusOptionAndAwaitModal(
            emailStatusName,
          )
          const notifyCheckbox = await modal.$('input[type=checkbox]')
          if (!notifyCheckbox) {
            throw new Error('Expected a checkbox input')
          }
          await notifyCheckbox.uncheck()
          expect(await notifyCheckbox.isChecked()).toBe(false)
          await adminPrograms.confirmStatusUpdateModal(modal)
          expect(await adminPrograms.getStatusOption()).toBe(emailStatusName)
          await adminPrograms.expectUpdateStatusToast()

          if (supportsEmailInspection()) {
            const emailsAfter = await extractEmailsForRecipient(
              page,
              testUserDisplayName(),
            )
            expect(emailsAfter.length).toEqual(emailsBefore.length)
          }
        })
      })
    })

    it('allows editing a note and preserves the selected application', async () => {
      const {adminPrograms} = ctx
      await adminPrograms.editNote('Some note content')
      await adminPrograms.expectNoteUpdatedToast()

      // Confirm that the application is shown after reloading the page.
      const applicationText = await adminPrograms
        .applicationFrameLocator()
        .locator('#application-view')
        .innerText()
      expect(applicationText).toContain('Guest')
    })

    it('shows the current note content', async () => {
      const {adminPrograms} = ctx
      const noteText = 'Some note content'
      await adminPrograms.editNote(noteText)
      await adminPrograms.expectNoteUpdatedToast()

      // Reload the note editor.
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')

      expect(await adminPrograms.getNoteContent()).toBe(noteText)
    })

    it('allows updating a note', async () => {
      const {adminPrograms} = ctx
      const noteText = 'Some note content'
      await adminPrograms.editNote('first note content')
      await adminPrograms.expectNoteUpdatedToast()
      await adminPrograms.editNote(noteText)
      await adminPrograms.expectNoteUpdatedToast()

      // Reload the note editor.
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')

      expect(await adminPrograms.getNoteContent()).toBe(noteText)
    })

    it('preserves newlines in notes', async () => {
      const {adminPrograms} = ctx
      const noteText = 'Some note content\nwithseparatelines'
      await adminPrograms.editNote(noteText)
      await adminPrograms.expectNoteUpdatedToast()

      // Reload the note editor.
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')

      expect(await adminPrograms.getNoteContent()).toBe(noteText)
    })
  })

  describe('filtering list with program statuses', () => {
    const programForFilteringName = 'test-program-for-filtering-statuses'
    const approvedStatusName = 'Approved'
    const rejectedStatusName = 'Rejected'

    const favoriteColorAnswer = 'orange'

    beforeAll(async () => {
      const {
        page,
        adminPrograms,
        adminQuestions,
        applicantQuestions,
        adminProgramStatuses,
      } = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'application_status_tracking_enabled')

      // Add a program with a single question that is used for asserting downloaded content.
      await adminPrograms.addProgram(programForFilteringName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(
        programForFilteringName,
      )
      await adminProgramStatuses.createStatus(approvedStatusName)
      await adminProgramStatuses.createStatus(rejectedStatusName)
      await adminQuestions.addTextQuestion({
        questionName: 'statuses-fave-color-q',
      })
      await adminPrograms.editProgramBlock(
        programForFilteringName,
        'dummy description',
        ['statuses-fave-color-q'],
      )
      await adminPrograms.publishProgram(programForFilteringName)
      await adminPrograms.expectActiveProgram(programForFilteringName)
      await logout(page)

      // Submit an application as a guest.
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await applicantQuestions.applyProgram(programForFilteringName)
      await applicantQuestions.answerTextQuestion(favoriteColorAnswer)
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()
    })

    beforeEach(async () => {
      const {page} = ctx
      await loginAsProgramAdmin(page)
      await enableFeatureFlag(page, 'application_status_tracking_enabled')
    })

    it('application without status appears in default filter and without statuses filter', async () => {
      const {adminPrograms} = ctx
      await adminPrograms.viewApplications(programForFilteringName)
      // Default page shows all applications.
      await adminPrograms.expectApplicationCount(1)

      // Included when filtering to applications without statuses.
      await adminPrograms.filterProgramApplications({
        applicationStatusOption:
          AdminPrograms.NO_STATUS_APPLICATION_FILTER_OPTION,
      })
      await adminPrograms.expectApplicationCount(1)

      // Excluded when selecting specific statuses.
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: approvedStatusName,
      })
      await adminPrograms.expectApplicationCount(0)
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: rejectedStatusName,
      })
      await adminPrograms.expectApplicationCount(0)

      // Included when explicitly selecting the default option to show all applications.
      await adminPrograms.filterProgramApplications({
        applicationStatusOption:
          AdminPrograms.ANY_STATUS_APPLICATION_FILTER_OPTION,
      })
      await adminPrograms.expectApplicationCount(1)
    })

    it('applied application status filter is used when downloading', async () => {
      const {adminPrograms} = ctx
      const applyFilters = true
      // Ensure that the application is included if the filter includes it.
      await adminPrograms.viewApplications(programForFilteringName)
      await adminPrograms.filterProgramApplications({
        applicationStatusOption:
          AdminPrograms.NO_STATUS_APPLICATION_FILTER_OPTION,
      })
      const noStatusFilteredCsvContent = await adminPrograms.getCsv(
        applyFilters,
      )
      expect(noStatusFilteredCsvContent).toContain(favoriteColorAnswer)
      const noStatusFilteredJsonContent = await adminPrograms.getJson(
        applyFilters,
      )
      expect(noStatusFilteredJsonContent.length).toEqual(1)
      expect(
        noStatusFilteredJsonContent[0].application.statusesfavecolorq.text,
      ).toEqual(favoriteColorAnswer)

      // Ensure that the application is excluded if the filter excludes it.
      await adminPrograms.viewApplications(programForFilteringName)
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: approvedStatusName,
      })
      const approvedStatusFilteredCsvContent = await adminPrograms.getCsv(
        applyFilters,
      )
      expect(approvedStatusFilteredCsvContent).not.toContain(
        favoriteColorAnswer,
      )
      const approvedStatusFilteredJsonContent = await adminPrograms.getJson(
        applyFilters,
      )
      expect(approvedStatusFilteredJsonContent.length).toEqual(0)
    })

    it('application with status shows in default filter and status-specific filter', async () => {
      const {adminPrograms} = ctx
      // Explicitly set a status for the application.
      await adminPrograms.viewApplications(programForFilteringName)
      await adminPrograms.viewApplicationForApplicant('Guest')
      const modal = await adminPrograms.setStatusOptionAndAwaitModal(
        approvedStatusName,
      )
      await adminPrograms.confirmStatusUpdateModal(modal)

      // Excluded when filtering to applications without statuses.
      await adminPrograms.filterProgramApplications({
        applicationStatusOption:
          AdminPrograms.NO_STATUS_APPLICATION_FILTER_OPTION,
      })
      await adminPrograms.expectApplicationCount(0)

      // Included when selecting the "approved" status.
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: approvedStatusName,
      })
      await adminPrograms.expectApplicationCount(1)

      // Excluded when selecting the "rejected" status.
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: rejectedStatusName,
      })
      await adminPrograms.expectApplicationCount(0)

      // Included when explicitly selecting the default option to show all applications.
      await adminPrograms.filterProgramApplications({
        applicationStatusOption:
          AdminPrograms.ANY_STATUS_APPLICATION_FILTER_OPTION,
      })
      await adminPrograms.expectApplicationCount(1)
    })

    it('shows the application on reload after the status is updated to something no longer in the filter', async () => {
      const {adminPrograms} = ctx

      await adminPrograms.viewApplications(programForFilteringName)
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: approvedStatusName,
      })
      await adminPrograms.viewApplicationForApplicant('Guest')
      const modal = await adminPrograms.setStatusOptionAndAwaitModal(
        rejectedStatusName,
      )
      await adminPrograms.confirmStatusUpdateModal(modal)

      // The application should no longer be in the list, since its status is no longer "approved".
      // However, it should still be displayed in the viewer since admins may want to easily revert
      // the status update.
      await adminPrograms.expectApplicationCount(0)
      const applicationText = await adminPrograms
        .applicationFrameLocator()
        .locator('#application-view')
        .innerText()
      expect(applicationText).toContain('Guest')
      expect(applicationText).toContain(favoriteColorAnswer)
    })
  })
})
