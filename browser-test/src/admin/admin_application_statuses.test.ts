import {Page} from 'playwright'
import {test, expect} from '../support/civiform_fixtures'
import {
  AdminPrograms,
  ApplicantQuestions,
  dismissModal,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  supportsEmailInspection,
  testUserDisplayName,
  extractEmailsForRecipient,
  validateScreenshot,
  AdminProgramStatuses,
} from '../support'

test.describe('view program statuses', () => {
  const programWithStatusesName = 'Test program with statuses'
  const noEmailStatusName = 'No email status'
  const emailStatusName = 'Email status'
  const emailBody = 'Some email content'

  test.describe('without program statuses', () => {
    const programWithoutStatusesName = 'Test program without statuses'
    test.beforeEach(async ({page, adminPrograms, applicantQuestions}) => {
      await loginAsAdmin(page)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithoutStatusesName)
      await adminPrograms.publishProgram(programWithoutStatusesName)
      await adminPrograms.expectActiveProgram(programWithoutStatusesName)
      await logout(page)

      // Submit an application as a guest.
      await applicantQuestions.clickApplyProgramButton(
        programWithoutStatusesName,
      )
      await applicantQuestions.submitFromReviewPage()

      // Navigate to the submitted application as the program admin.
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programWithoutStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
    })

    test('does not show status options', async ({adminPrograms}) => {
      expect(await adminPrograms.isStatusSelectorVisible()).toBe(false)
    })

    test('does not show application status in table', async ({
      page,
      adminPrograms,
    }) => {
      await page.getByRole('link', {name: 'Back'}).click()
      await adminPrograms.expectApplicationStatusDoesntContain(
        'Guest',
        'Status: ',
      )
    })

    test('does not show edit note', async ({page, adminPrograms}) => {
      expect(await adminPrograms.isEditNoteVisible()).toBe(false)
      await page.getByRole('link', {name: 'Back'}).click()
    })

    test('does not show pagination when there is only 1 page of applications', async ({
      adminPrograms,
    }) => {
      expect(await adminPrograms.isPaginationVisibleForApplicationTable()).toBe(
        false,
      )
    })

    /* See trusted_intermediary.test.ts for more comprehensive pagination testing */
    test.skip('shows pagination if there are more than 100 applications', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      test.slow()
      await page.getByRole('link', {name: 'Back'}).click()
      // There is already 1 application from the beforeEach, so apply to 100 more programs.
      for (let i = 0; i < 100; i++) {
        await logout(page)

        // Submit an application as a guest.
        await applicantQuestions.clickApplyProgramButton(
          programWithoutStatusesName,
        )
        await applicantQuestions.submitFromReviewPage()

        await loginAsProgramAdmin(page)
      }

      // Navigate to the applications list
      await adminPrograms.viewApplications(programWithoutStatusesName)

      expect(await adminPrograms.isPaginationVisibleForApplicationTable()).toBe(
        true,
      )

      await validateScreenshot(page, 'application-table-pagination')
    })
  })

  test.describe('with program statuses', () => {
    test.beforeEach(
      async ({
        page,
        adminPrograms,
        applicantQuestions,
        adminProgramStatuses,
      }) => {
        await setupProgramsWithStatuses(
          page,
          adminPrograms,
          applicantQuestions,
          adminProgramStatuses,
        )

        await loginAsProgramAdmin(page)
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant('Guest')
      },
    )

    test('shows status selector', async ({adminPrograms}) => {
      expect(await adminPrograms.isStatusSelectorVisible()).toBe(true)
    })

    test('shows placeholder option', async ({adminPrograms}) => {
      expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
    })

    test('renders', async ({page}) => {
      await validateScreenshot(page, 'application-view-with-statuses')
    })

    test('shows "None" value in application table if no status is set', async ({
      adminPrograms,
    }) => {
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.expectApplicationHasStatusString('Guest', 'None')
    })

    test.describe('when a status is changed, a confirmation dialog is shown', () => {
      test('renders', async ({page, adminPrograms}) => {
        await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        await validateScreenshot(page, 'change-status-modal')
      })

      test('when rejecting, the selected status is not changed', async ({
        page,
        adminPrograms,
      }) => {
        await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        await dismissModal(page)
        expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
      })

      test('when confirmed, the page is shown a success toast', async ({
        adminPrograms,
      }) => {
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        expect(await modal.innerText()).toContain(
          `Status Change: Unset -> ${noEmailStatusName}`,
        )
        await adminPrograms.confirmStatusUpdateModal(modal)
        expect(await adminPrograms.getStatusOption()).toBe(noEmailStatusName)
        await adminPrograms.expectUpdateStatusToast()
      })

      test('when no email is configured for the status, a warning is shown', async ({
        page,
        adminPrograms,
      }) => {
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
        expect(await modal.innerText()).toContain(
          'will not receive an email because there is no email content set for this status. Connect with your CiviForm Admin to add an email to this status',
        )
        await dismissModal(page)
      })

      test('when no email is configured for the applicant, a warning is shown', async ({
        page,
        adminPrograms,
      }) => {
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(emailStatusName)
        expect(await modal.innerText()).toContain(
          'will not receive an email for this change since they have not provided an email address.',
        )
        await dismissModal(page)
      })

      test('when changing status, the previous status is shown', async ({
        page,
        adminPrograms,
      }) => {
        await test.step('Set initial status', async () => {
          const modal =
            await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
          await adminPrograms.confirmStatusUpdateModal(modal)
          expect(await adminPrograms.getStatusOption()).toBe(noEmailStatusName)
        })

        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(emailStatusName)
        expect(await modal.innerText()).toContain(
          `Status Change: ${noEmailStatusName} -> ${emailStatusName}`,
        )
        await dismissModal(page)
      })

      test('when changing status, the updated application status is reflected in the application table', async ({
        page,
        adminPrograms,
      }) => {
        await test.step('Set initial status', async () => {
          const modal =
            await adminPrograms.setStatusOptionAndAwaitModal(noEmailStatusName)
          await adminPrograms.confirmStatusUpdateModal(modal)
          expect(await adminPrograms.getStatusOption()).toBe(noEmailStatusName)
        })

        await page.getByRole('link', {name: 'Back'}).click()

        await adminPrograms.expectApplicationHasStatusString(
          'Guest',
          noEmailStatusName,
        )
        await adminPrograms.viewApplicationForApplicant('Guest')
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(emailStatusName)
        await adminPrograms.confirmStatusUpdateModal(modal)
        await page.getByRole('link', {name: 'Back'}).click()
        await adminPrograms.expectApplicationHasStatusString(
          'Guest',
          emailStatusName,
        )
      })

      test.describe('when email is configured for the status and applicant, a checkbox is shown to notify the applicant', () => {
        test.beforeEach(async ({adminPrograms}) => {
          await adminPrograms.viewApplications(programWithStatusesName)
          await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
        })

        test('choosing not to notify applicant changes status and does not send email', async ({
          page,
          adminPrograms,
        }) => {
          const emailsBefore = supportsEmailInspection()
            ? await extractEmailsForRecipient(page, testUserDisplayName())
            : []
          const modal =
            await adminPrograms.setStatusOptionAndAwaitModal(emailStatusName)
          const notifyCheckbox = await modal.$('input[type=checkbox]')
          expect(notifyCheckbox).not.toBeNull()
          await notifyCheckbox!.uncheck()
          expect(await notifyCheckbox!.isChecked()).toBe(false)
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

        test('checkbox is checked by default and email is sent', async ({
          page,
          adminPrograms,
        }) => {
          const emailsBefore = supportsEmailInspection()
            ? await extractEmailsForRecipient(page, testUserDisplayName())
            : []
          const modal =
            await adminPrograms.setStatusOptionAndAwaitModal(emailStatusName)
          const notifyCheckbox = await modal.$('input[type=checkbox]')
          expect(notifyCheckbox).not.toBeNull()
          expect(await notifyCheckbox!.isChecked()).toBe(true)
          expect(await modal.innerText()).toContain(' of this change at ')
          await adminPrograms.confirmStatusUpdateModal(modal)
          expect(await adminPrograms.getStatusOption()).toBe(emailStatusName)
          await adminPrograms.expectUpdateStatusToast()

          if (supportsEmailInspection()) {
            await adminPrograms.expectEmailSent(
              emailsBefore.length,
              testUserDisplayName(),
              emailBody,
              programWithStatusesName,
            )
          }
        })
      })
    })

    test('allows editing a note and preserves the selected application', async ({
      page,
      adminPrograms,
    }) => {
      await adminPrograms.editNote('Some note content')
      await adminPrograms.expectNoteUpdatedToast()

      // Confirm that the application is shown after reloading the page.
      const applicationText = await page
        .locator('#application-view')
        .innerText()
      expect(applicationText).toContain('Guest')
    })

    test('renders the note dialog', async ({page, adminPrograms}) => {
      await adminPrograms.awaitEditNoteModal()
      await page.evaluate(() => {
        window.scrollTo(0, 0)
      })
      await validateScreenshot(page, 'edit-note-modal')
    })

    test('shows the current note content', async ({adminPrograms}) => {
      const noteText = 'Some note content'
      await adminPrograms.editNote(noteText)
      await adminPrograms.expectNoteUpdatedToast()

      // Reload the note editor.
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')

      expect(await adminPrograms.getNoteContent()).toBe(noteText)
    })

    test('allows updating a note', async ({adminPrograms}) => {
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

    test('allow notes to be exported', async ({page, adminPrograms}) => {
      await adminPrograms.editNote('Note is exported')
      await adminPrograms.expectNoteUpdatedToast()
      const noApplyFilters = false
      await page.getByRole('link', {name: 'Back'}).click()

      const csvContent = await adminPrograms.getCsv(noApplyFilters)
      expect(csvContent).toContain('Note is exported')
    })

    test('export only the latest note', async ({page, adminPrograms}) => {
      await adminPrograms.editNote('Note is exported')
      await adminPrograms.expectNoteUpdatedToast()
      const noApplyFilters = false

      // Update note only gets exported
      await adminPrograms.editNote('Note is updated')
      await adminPrograms.expectNoteUpdatedToast()
      await page.getByRole('link', {name: 'Back'}).click()
      const csvContent = await adminPrograms.getCsv(noApplyFilters)
      expect(csvContent).toContain('Note is updated')

      await adminPrograms.viewApplicationForApplicant('Guest')
      await adminPrograms.editNote('Note is finalized')
      await page.getByRole('link', {name: 'Back'}).click()
      const csvContentFinal = await adminPrograms.getCsv(noApplyFilters)
      expect(csvContentFinal).toContain('Note is finalized')
    })

    test('preserves newlines in notes', async ({adminPrograms}) => {
      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant('Guest')
      const noteText = 'Some note content\nwithseparatelines'
      await adminPrograms.editNote(noteText)
      await adminPrograms.expectNoteUpdatedToast()

      expect(await adminPrograms.getNoteContent()).toBe(noteText)
    })
  })

  test.describe('with program statuses including a default status', () => {
    const programWithDefaultStatusName = 'Test program with a default status'
    const waitingStatus = 'Waiting'
    const approvedStatus = 'Approved'
    const emailBody = 'Some email content'

    test.beforeEach(
      async ({
        page,
        adminPrograms,
        applicantQuestions,
        adminProgramStatuses,
      }) => {
        await loginAsAdmin(page)

        // Add a program, no questions are needed.
        await adminPrograms.addProgram(programWithDefaultStatusName)
        await adminPrograms.gotoDraftProgramManageStatusesPage(
          programWithDefaultStatusName,
        )
        await adminProgramStatuses.createStatus(waitingStatus)
        await adminProgramStatuses.createStatus(approvedStatus, {
          emailBody: emailBody,
        })
        await adminProgramStatuses.editStatusDefault(
          waitingStatus,
          true,
          adminProgramStatuses.newDefaultStatusMessage(waitingStatus),
        )
        await adminPrograms.publishProgram(programWithDefaultStatusName)
        await adminPrograms.expectActiveProgram(programWithDefaultStatusName)
        await logout(page)

        // Submit an application as a guest.
        await applicantQuestions.clickApplyProgramButton(
          programWithDefaultStatusName,
        )
        await applicantQuestions.submitFromReviewPage()
        await logout(page)

        // Submit an application as the logged in test user.
        await loginAsTestUser(page)
        await applicantQuestions.clickApplyProgramButton(
          programWithDefaultStatusName,
        )
        await applicantQuestions.submitFromReviewPage()
        await logout(page)

        await loginAsProgramAdmin(page)
        await adminPrograms.viewApplications(programWithDefaultStatusName)
      },
    )

    test('when a default status is set, applications with that status show (default)', async ({
      page,
      adminPrograms,
    }) => {
      await adminPrograms.expectApplicationHasStatusString(
        'Guest',
        `${waitingStatus} (default)`,
      )
      await adminPrograms.expectApplicationHasStatusString(
        testUserDisplayName(),
        `${waitingStatus} (default)`,
      )

      // Approve guest application
      await adminPrograms.viewApplicationForApplicant('Guest')
      const modal =
        await adminPrograms.setStatusOptionAndAwaitModal(approvedStatus)
      expect(await modal.innerText()).toContain(
        `Status Change: ${waitingStatus} -> ${approvedStatus}`,
      )
      await adminPrograms.confirmStatusUpdateModal(modal)
      expect(await adminPrograms.getStatusOption()).toBe(approvedStatus)
      await adminPrograms.expectUpdateStatusToast()

      await page.getByRole('link', {name: 'Back'}).click()

      await adminPrograms.expectApplicationStatusDoesntContain(
        'Guest',
        '(default)',
      )
      await adminPrograms.expectApplicationHasStatusString(
        testUserDisplayName(),
        `${waitingStatus} (default)`,
      )

      await validateScreenshot(page, 'application-view-with-default')
    })
  })

  test.describe('filtering list with program statuses', () => {
    const programForFilteringName = 'Test program for filtering statuses'
    const approvedStatusName = 'Approved'
    const rejectedStatusName = 'Rejected'

    const favoriteColorAnswer = 'orange'

    test.beforeEach(
      async ({
        page,
        adminPrograms,
        adminQuestions,
        applicantQuestions,
        adminProgramStatuses,
      }) => {
        await loginAsAdmin(page)

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
        await applicantQuestions.applyProgram(programForFilteringName)
        await applicantQuestions.answerTextQuestion(favoriteColorAnswer)
        await applicantQuestions.clickNext()
        await applicantQuestions.submitFromReviewPage()
        await loginAsProgramAdmin(page)
      },
    )

    test('application without status appears in default filter and without statuses filter', async ({
      adminPrograms,
    }) => {
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

    test('applied application status filter is used when downloading', async ({
      adminPrograms,
    }) => {
      const applyFilters = true
      // Ensure that the application is included if the filter includes it.
      await adminPrograms.viewApplications(programForFilteringName)
      await adminPrograms.filterProgramApplications({
        applicationStatusOption:
          AdminPrograms.NO_STATUS_APPLICATION_FILTER_OPTION,
      })
      const noStatusFilteredCsvContent =
        await adminPrograms.getCsv(applyFilters)
      expect(noStatusFilteredCsvContent).toContain(favoriteColorAnswer)
      const noStatusFilteredJsonContent =
        await adminPrograms.getJson(applyFilters)
      expect(noStatusFilteredJsonContent.length).toEqual(1)
      expect(
        noStatusFilteredJsonContent[0].application.statusesfavecolorq.text,
      ).toEqual(favoriteColorAnswer)

      // Ensure that the application is excluded if the filter excludes it.
      await adminPrograms.viewApplications(programForFilteringName)
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: approvedStatusName,
      })
      const approvedStatusFilteredCsvContent =
        await adminPrograms.getCsv(applyFilters)
      expect(approvedStatusFilteredCsvContent).not.toContain(
        favoriteColorAnswer,
      )
      const approvedStatusFilteredJsonContent =
        await adminPrograms.getJson(applyFilters)
      expect(approvedStatusFilteredJsonContent.length).toEqual(0)
    })

    test('application with status shows in default filter and status-specific filter', async ({
      page,
      adminPrograms,
    }) => {
      await test.step('explicitly set a status for the application', async () => {
        await adminPrograms.viewApplications(programForFilteringName)
        await adminPrograms.viewApplicationForApplicant('Guest')
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(approvedStatusName)
        await adminPrograms.confirmStatusUpdateModal(modal)
        await page.getByRole('link', {name: 'Back'}).click()
      })

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

    test('shows the application on reload after the status is updated to something no longer in the filter', async ({
      page,
      adminPrograms,
    }) => {
      await test.step('explicitly set a status for the application', async () => {
        await adminPrograms.viewApplications(programForFilteringName)
        await adminPrograms.viewApplicationForApplicant('Guest')
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(approvedStatusName)
        await adminPrograms.confirmStatusUpdateModal(modal)
        await page.getByRole('link', {name: 'Back'}).click()
      })

      await adminPrograms.filterProgramApplications({
        applicationStatusOption: approvedStatusName,
      })
      await adminPrograms.expectApplicationCount(1)

      await adminPrograms.viewApplicationForApplicant('Guest')
      const modal =
        await adminPrograms.setStatusOptionAndAwaitModal(rejectedStatusName)
      await adminPrograms.confirmStatusUpdateModal(modal)
      await page.getByRole('link', {name: 'Back'}).click()

      // The application should no longer be in the list, since its status is no longer "approved".
      // However, it should still be displayed in the viewer since admins may want to easily revert
      // the status update.
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: approvedStatusName,
      })
      await adminPrograms.expectApplicationCount(0)
      await adminPrograms.filterProgramApplications({
        applicationStatusOption: rejectedStatusName,
      })
      await adminPrograms.viewApplicationForApplicant('Guest')
      const applicationText = await page
        .locator('#application-view')
        .innerText()
      expect(applicationText).toContain('Guest')
      expect(applicationText).toContain(favoriteColorAnswer)

      await test.step('allow status last modified time to be exported', async () => {
        await page.getByRole('link', {name: 'Back'}).click()
        const csvContent = await adminPrograms.getCsv(false)
        expect(csvContent).toContain('Status Last Modified Time')
      })
    })
  })

  test.describe('correctly shows eligibility', () => {
    const eligibilityProgramName = 'Test program for eligibility status'
    const eligibilityQuestionId = 'eligibility-number-q'

    test.beforeEach(
      async ({
        page,
        adminQuestions,
        adminPredicates,
        applicantQuestions,
        adminPrograms,
      }) => {
        await loginAsAdmin(page)

        // Create a program without eligibility
        await adminQuestions.addNameQuestion({
          questionName: 'NameQuestion',
        })
        await adminQuestions.addNumberQuestion({
          questionName: eligibilityQuestionId,
        })
        await adminQuestions.addTextQuestion({
          questionName: 'statuses-fave-color-q',
        })
        await adminPrograms.addProgram(eligibilityProgramName)
        await adminPrograms.editProgramBlock(
          eligibilityProgramName,
          'first description',
          [eligibilityQuestionId, 'statuses-fave-color-q', 'NameQuestion'],
        )
        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(eligibilityProgramName)
        await logout(page)

        // Before eligibility conditions are added, submit ineligible app
        await applicantQuestions.applyProgram(eligibilityProgramName)

        // Fill out application and submit.
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.answerTextQuestion('Red')
        await applicantQuestions.answerNameQuestion('Robin', 'Hood')
        await applicantQuestions.clickNext()
        await applicantQuestions.submitFromReviewPage()
        await logout(page)

        // Add eligibility conditions to existing program
        await loginAsAdmin(page)
        await adminPrograms.createNewVersion(eligibilityProgramName)
        await adminPrograms.gotoEditDraftProgramPage(eligibilityProgramName)
        await adminPrograms.goToEditBlockEligibilityPredicatePage(
          eligibilityProgramName,
          'Screen 1',
        )
        await adminPredicates.addPredicates({
          questionName: eligibilityQuestionId,
          scalar: 'number',
          operator: 'is equal to',
          value: '5',
        })
        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(eligibilityProgramName)
        await logout(page)

        // Submit eligible app
        await applicantQuestions.applyProgram(eligibilityProgramName)
        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.answerTextQuestion('Red')
        await applicantQuestions.answerNameQuestion('Sonny', 'Hood')
        await applicantQuestions.clickNext()
        await applicantQuestions.submitFromReviewPage()

        await logout(page)
      },
    )

    test('application table shows eligibility statuses', async ({
      page,
      adminPrograms,
    }) => {
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(eligibilityProgramName)
      await validateScreenshot(
        page,
        'application-table-view-with-eligibility-statuses',
      )
    })
  })
  test.describe('email status updates work correctly with PAI flag on', () => {
    test.beforeEach(
      async ({
        page,
        adminPrograms,
        applicantQuestions,
        adminProgramStatuses,
        adminQuestions,
      }) => {
        await setupProgramsWithStatuses(
          page,
          adminPrograms,
          applicantQuestions,
          adminProgramStatuses,
        )
        await loginAsAdmin(page)
        await adminQuestions.addEmailQuestion({
          questionName: 'Email',
          universal: true,
          primaryApplicantInfo: true,
        })
        await adminPrograms.editProgram(programWithStatusesName)
        await adminPrograms.editProgramBlock(
          programWithStatusesName,
          'block description',
          ['Email'],
        )
        await adminPrograms.publishAllDrafts()
        await logout(page)
      },
    )
    test('email is displayed and sent for guest user that has answered the PAI email question', async ({
      page,
      applicantQuestions,
      adminPrograms,
    }) => {
      const guestEmail = 'guestemail@example.com'

      await test.step('submit application as guest', async () => {
        await applicantQuestions.applyProgram(programWithStatusesName)
        await applicantQuestions.answerEmailQuestion(guestEmail)
        await applicantQuestions.clickNext()
        await applicantQuestions.submitFromReviewPage()
        const id = await adminPrograms.getApplicationId()
        await logout(page)
        await loginAsProgramAdmin(page)
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant(`${guestEmail} (${id})`)
      })

      const emailsBefore =
        await test.step('get count of emails before status change', async () => {
          return supportsEmailInspection()
            ? await extractEmailsForRecipient(page, guestEmail)
            : []
        })

      await test.step('set new status and confirm change via modal', async () => {
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(emailStatusName)

        const notifyCheckbox = await modal.$('input[type=checkbox]')
        expect(notifyCheckbox).not.toBeNull()
        expect(await notifyCheckbox!.isChecked()).toBe(true)
        expect(await modal.innerText()).toContain(
          ' of this change at ' + guestEmail,
        )
        await adminPrograms.confirmStatusUpdateModal(modal)
        expect(await adminPrograms.getStatusOption()).toBe(emailStatusName)
        await adminPrograms.expectUpdateStatusToast()
      })
      await test.step('verify status update email was sent to applicant', async () => {
        if (supportsEmailInspection()) {
          await adminPrograms.expectEmailSent(
            emailsBefore.length,
            guestEmail,
            emailBody,
            programWithStatusesName,
          )
        }
      })
    })
    test('both email addresses are displayed and two emails are sent for a logged in user that has answered the PAI email question with a different email', async ({
      page,
      applicantQuestions,
      adminPrograms,
    }) => {
      const otherTestUserEmail = 'other@example.com'

      await test.step('submit application as a logged in user with a different email address', async () => {
        await loginAsTestUser(page)
        await applicantQuestions.applyProgram(programWithStatusesName)
        await applicantQuestions.answerEmailQuestion(otherTestUserEmail)
        await applicantQuestions.clickNext()
        await applicantQuestions.submitFromReviewPage()
        const id = await adminPrograms.getApplicationId()
        await logout(page)
        await loginAsProgramAdmin(page)
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant(
          `${otherTestUserEmail} (${id})`,
        )
      })

      const [acccountEmailsBefore, applicantEmailsBefore] =
        await test.step('get count of emails before status change', async () => {
          const acccountEmailsBefore = supportsEmailInspection()
            ? await extractEmailsForRecipient(page, testUserDisplayName())
            : []
          const applicantEmailsBefore = supportsEmailInspection()
            ? await extractEmailsForRecipient(page, otherTestUserEmail)
            : []
          return [acccountEmailsBefore, applicantEmailsBefore]
        })

      await test.step('set new status and confirm change via modal', async () => {
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(emailStatusName)
        expect(await modal.innerText()).toContain(
          ' of this change at ' +
            testUserDisplayName() +
            ' and ' +
            otherTestUserEmail,
        )

        await adminPrograms.confirmStatusUpdateModal(modal)
        await adminPrograms.expectUpdateStatusToast()
      })
      await test.step('verify status update email was sent to both email addresses', async () => {
        if (supportsEmailInspection()) {
          await adminPrograms.expectEmailSent(
            acccountEmailsBefore.length,
            testUserDisplayName(),
            emailBody,
            programWithStatusesName,
          )
          await adminPrograms.expectEmailSent(
            applicantEmailsBefore.length,
            otherTestUserEmail,
            emailBody,
            programWithStatusesName,
          )
        }
      })
    })
    test('only one email is displayed and sent for a logged in user that has answered the PAI email question with the same email they used to login', async ({
      page,
      applicantQuestions,
      adminPrograms,
    }) => {
      await test.step('submit application as a logged in user with the same email address', async () => {
        await loginAsTestUser(page)
        await applicantQuestions.applyProgram(programWithStatusesName)
        await applicantQuestions.answerEmailQuestion(testUserDisplayName())
        await applicantQuestions.clickNext()
        await applicantQuestions.submitFromReviewPage()
        const id = await adminPrograms.getApplicationId()
        await logout(page)
        await loginAsProgramAdmin(page)
        await adminPrograms.viewApplications(programWithStatusesName)
        await adminPrograms.viewApplicationForApplicant(
          `${testUserDisplayName()} (${id})`,
        )
      })

      const emailsBefore =
        await test.step('get count of emails before status change', async () => {
          return supportsEmailInspection()
            ? await extractEmailsForRecipient(page, testUserDisplayName())
            : []
        })

      await test.step('set new status and confirm change via modal', async () => {
        const modal =
          await adminPrograms.setStatusOptionAndAwaitModal(emailStatusName)
        expect(await modal.innerText()).toContain(
          ' of this change at ' + testUserDisplayName(),
        )
        expect(await modal.innerText()).not.toContain(' and ')

        await adminPrograms.confirmStatusUpdateModal(modal)
        await adminPrograms.expectUpdateStatusToast()
      })
      await test.step('verify status update email was sent to the applicant', async () => {
        if (supportsEmailInspection()) {
          await adminPrograms.expectEmailSent(
            emailsBefore.length,
            testUserDisplayName(),
            emailBody,
            programWithStatusesName,
          )
        }
      })
    })
  })

  const setupProgramsWithStatuses = async (
    page: Page,
    adminPrograms: AdminPrograms,
    applicantQuestions: ApplicantQuestions,
    adminProgramStatuses: AdminProgramStatuses,
  ) => {
    await test.step('login as admin and create program with statuses', async () => {
      await loginAsAdmin(page)
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
    })

    await test.step('submit an application as a guest', async () => {
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromReviewPage()
      await logout(page)
    })

    await test.step('submit an application as a logged in user', async () => {
      await loginAsTestUser(page)
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromReviewPage()
      await logout(page)
    })
  }
})
