import {
  ClientInformation,
  createTestContext,
  loginAsAdmin,
  loginAsTrustedIntermediary,
  waitForPageJsLoad,
  validateScreenshot,
  validateToastMessage,
  logout,
  AdminQuestions,
  dismissToast,
  selectApplicantLanguage,
} from './support'

describe('Trusted intermediaries', () => {
  const ctx = createTestContext()

  it('expect Client Date Of Birth to be Updated', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardContainClient(client)
    await tiDashboard.updateClientDateOfBirth(client, '2021-12-12')
    const updatedClient: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-12-12',
    }
    await tiDashboard.expectDashboardContainClient(updatedClient)
  })

  it('expect client cannot be added with invalid date of birth', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'abc@abc.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '1870-07-11',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardNotContainClient(client)
    await validateScreenshot(page, 'dashboard-add-client-invalid-dob')
  })

  it('expect Dashboard Contain New Client', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'fake@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-05-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardContainClient(client)
    await validateScreenshot(page, 'dashboard-with-one-client')
  })

  it('expect clients can be added without an email address', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    const client1: ClientInformation = {
      emailAddress: '',
      firstName: 'Jean-Luc',
      middleName: '',
      lastName: 'Picard',
      dobDate: '1940-07-13',
    }
    await tiDashboard.createClient(client1)
    await tiDashboard.expectDashboardContainClient(client1)

    const client2: ClientInformation = {
      emailAddress: '',
      firstName: 'William',
      middleName: 'Thomas',
      lastName: 'Riker',
      dobDate: '1952-08-19',
    }
    await tiDashboard.createClient(client2)
    await tiDashboard.expectDashboardContainClient(client2)
    await tiDashboard.expectSuccessToast(
      `Successfully added new client: ${client2.firstName} ${client2.lastName}`,
    )

    await validateScreenshot(page, 'dashboard-add-clients-no-email')
  })

  it('expect client email address to be updated', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await waitForPageJsLoad(page)
    await tiDashboard.updateClientEmailAddress(client, 'new@email.com')

    const updatedClient: ClientInformation = {
      emailAddress: 'new@email.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.expectDashboardContainClient(updatedClient)
  })

  it('expect client ti notes and phone to be updated', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await waitForPageJsLoad(page)
    await tiDashboard.updateClientTiNoteAndPhone(
      client,
      'Housing Assistance',
      '4256007121',
    )
    await waitForPageJsLoad(page)
    await tiDashboard.expectClientContainsTiNoteAndPhone(
      client,
      'Housing Assistance',
      '4256007121',
    )
    await validateScreenshot(page, 'edit-client-information-with-all-fields')
  })

  it('expect client email to be updated to empty', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'Jane',
      middleName: 'middle',
      lastName: 'Doe',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await waitForPageJsLoad(page)
    await tiDashboard.updateClientEmailAddress(client, '')
    await waitForPageJsLoad(page)

    const row = page.locator(
      `.cf-admin-question-table-row:has-text("${client.lastName}, ${client.firstName}")`,
    )
    const rowText = await row.innerText()
    expect(rowText).toContain('(no email address)')
    expect(rowText).toContain(client.dobDate)
  })

  it('expect back button to land in dashboard in the edit client page', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'tes@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await waitForPageJsLoad(page)
    await page
      .getByRole('row')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(page)
    await page.waitForSelector('h2:has-text("Edit Client")')
    await page.click('text=Back to client list')
    await waitForPageJsLoad(page)
    await page.waitForSelector('h2:has-text("Add Client")')
    await validateScreenshot(page, 'back-link-leads-to-ti-dashboard')
  })

  it('expect cancel button should not update client information', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'tes@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await waitForPageJsLoad(page)
    await page
      .getByRole('row')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(page)
    await page.waitForSelector('h2:has-text("Edit Client")')
    // update client dob
    await page.fill('#edit-date-of-birth-input', '2022-10-13')

    await page.click('text=Cancel')
    await waitForPageJsLoad(page)
    await page.waitForSelector('h2:has-text("Add Client")')
    // dob should not be updated
    await tiDashboard.expectDashboardContainClient(client)
    await validateScreenshot(page, 'cancel-leads-to-dashboard')
  })

  it('expect field errors', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await waitForPageJsLoad(page)
    await page
      .getByRole('row')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(page)
    await page.waitForSelector('h2:has-text("Edit Client")')
    await page.fill('#edit-date-of-birth-input', '2027-12-20')
    await page.click('text="Save"')
    await validateScreenshot(page, 'edit-client-information-with-field-errors')
  })

  it('expect client cannot be added with invalid email address', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'bademail',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2023-07-11',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardNotContainClient(client)
    // In an email-type input field, when the text is not formatted as a valid
    // email address, there is a popup that shows and disappears after a period
    // of time or when you move focus away from the field. Move focus away
    // from the field in order to get a stable snapshot.
    await page.focus('label:has-text("First Name")')
    await validateScreenshot(page, 'dashboard-add-client-invalid-email')
  })

  it('ti landing page is the TI Dashboard', async () => {
    const {page} = ctx
    await loginAsTrustedIntermediary(page)
    await validateScreenshot(page, 'ti')
  })

  it('dashboard contains required indicator note and optional marker', async () => {
    const {page} = ctx
    await loginAsTrustedIntermediary(page)
    expect(await page.textContent('html')).toContain('Email Address (optional)')
    expect(await page.textContent('html')).toContain(
      'Fields marked with a * are required.',
    )
  })

  it('Applicant sees the program review page fully translated', async () => {
    const {
      page,
      adminQuestions,
      adminPrograms,
      applicantQuestions,
      adminTranslations,
      tiDashboard,
    } = ctx

    // Add a new program with one non-translated question
    await loginAsAdmin(page)

    const programName = 'TI Client Translation program'
    await adminPrograms.addProgram(programName)

    const questionName = 'name-translated'
    await adminQuestions.addNameQuestion({questionName})
    // Go to the question translation page and add a translation for Spanish

    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')
    await validateScreenshot(page, 'question-translation')
    await adminTranslations.editQuestionTranslations(
      'Spanish question text',
      'Spanish help text',
    )
    await adminPrograms.editProgramBlock(programName, 'block', [questionName])
    await adminPrograms.publishProgram(programName)
    await logout(page)

    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'fake12@sample.com',
      firstName: 'first1',
      middleName: 'middle',
      lastName: 'last1',
      dobDate: '2021-07-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.clickOnApplicantDashboard()

    await applicantQuestions.applyProgram(programName)
    await selectApplicantLanguage(page, 'Español')

    await validateScreenshot(page, 'applicant-program-spanish')
  })

  it('search For Client In TI Dashboard', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client1: ClientInformation = {
      emailAddress: 'fake@sample.com',
      firstName: 'first1',
      middleName: 'middle',
      lastName: 'last1',
      dobDate: '2021-07-07',
    }
    await tiDashboard.createClient(client1)
    const client2: ClientInformation = {
      emailAddress: 'fake2@sample.com',
      firstName: 'first2',
      middleName: 'middle',
      lastName: 'last2',
      dobDate: '2021-11-07',
    }
    await tiDashboard.createClient(client2)
    const client3: ClientInformation = {
      emailAddress: 'fake3@sample.com',
      firstName: 'first3',
      middleName: 'middle',
      lastName: 'last3',
      dobDate: '2021-12-07',
    }
    await tiDashboard.createClient(client3)

    await tiDashboard.searchByDateOfBirth('07', '12', '2021')
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardContainClient(client3)
    await tiDashboard.expectDashboardNotContainClient(client1)
    await tiDashboard.expectDashboardNotContainClient(client2)

    // If the day is a single digit, the search still works
    await tiDashboard.searchByDateOfBirth('7', '12', '2021')
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardContainClient(client3)
    await tiDashboard.expectDashboardNotContainClient(client1)
    await tiDashboard.expectDashboardNotContainClient(client2)
  })

  it('incomplete dob and no name in the client search returns an error', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client1: ClientInformation = {
      emailAddress: 'fake@sample.com',
      firstName: 'first1',
      middleName: 'middle',
      lastName: 'last1',
      dobDate: '1980-07-10',
    }
    await tiDashboard.createClient(client1)
    const client2: ClientInformation = {
      emailAddress: 'fake2@sample.com',
      firstName: 'first2',
      middleName: 'middle',
      lastName: 'last2',
      dobDate: '2021-11-10',
    }
    await tiDashboard.createClient(client2)

    await tiDashboard.searchByDateOfBirth('', '', '2021')
    await waitForPageJsLoad(page)

    await tiDashboard.expectDateSearchError()
    tiDashboard.expectRedDateFieldOutline(true, true, false)
    await tiDashboard.expectDashboardNotContainClient(client1)
    await tiDashboard.expectDashboardNotContainClient(client2)
    await validateScreenshot(page, 'incomplete-dob')
  })

  it('incomplete dob with name in the client search returns client by name', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client1: ClientInformation = {
      emailAddress: 'fake@sample.com',
      firstName: 'first1',
      middleName: 'middle',
      lastName: 'last1',
      dobDate: '1980-07-10',
    }
    await tiDashboard.createClient(client1)
    const client2: ClientInformation = {
      emailAddress: 'fake2@sample.com',
      firstName: 'first2',
      middleName: 'middle',
      lastName: 'last2',
      dobDate: '2021-11-10',
    }
    await tiDashboard.createClient(client2)

    await tiDashboard.searchByNameAndDateOfBirth('first1', '', '', '2021')
    await waitForPageJsLoad(page)

    await tiDashboard.expectDashboardContainClient(client1)
    await tiDashboard.expectDashboardNotContainClient(client2)
  })

  it('empty search parameters returns all clients', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client1: ClientInformation = {
      emailAddress: 'fake@sample.com',
      firstName: 'first1',
      middleName: 'middle',
      lastName: 'last1',
      dobDate: '2021-07-10',
    }
    await tiDashboard.createClient(client1)
    const client2: ClientInformation = {
      emailAddress: 'fake2@sample.com',
      firstName: 'first2',
      middleName: 'middle',
      lastName: 'last2',
      dobDate: '2021-11-10',
    }
    await tiDashboard.createClient(client2)

    await tiDashboard.searchByNameAndDateOfBirth('', '', '', '')
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardContainClient(client1)
    await tiDashboard.expectDashboardContainClient(client2)
  })

  it('managing trusted intermediary ', async () => {
    const {page, adminTiGroups} = ctx
    await loginAsAdmin(page)
    await adminTiGroups.gotoAdminTIPage()
    await adminTiGroups.fillInGroupBasics('group name', 'group description')
    await adminTiGroups.expectGroupExist('group name', 'group description')
    await validateScreenshot(page, 'ti-groups-page')

    await adminTiGroups.editGroup('group name')
    await adminTiGroups.addGroupMember('foo@bar.com')
    await adminTiGroups.expectGroupMemberExist('<Unnamed User>', 'foo@bar.com')
    await validateScreenshot(page, 'manage-ti-group-members-page')
  })

  it('logging in as a trusted intermediary', async () => {
    const {page} = ctx
    await loginAsTrustedIntermediary(page)
    expect(await page.innerText('#ti-dashboard-link')).toContain(
      'View and Add Clients',
    )
  })

  it('sees client name in sub-banner while applying for them', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'fake12@sample.com',
      firstName: 'first1',
      middleName: 'middle',
      lastName: 'last1',
      dobDate: '2021-07-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.clickOnApplicantDashboard()
    expect(await page.innerText('#ti-clients-link')).toContain(
      'Select a new client',
    )
    expect(await page.innerText('#ti-banner')).toContain(
      'You are applying for last1, first1. Are you trying to apply for a different client?',
    )
  })

  it('returns to TI dashboard from application when clicks the sub-banner link', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'fake12@sample.com',
      firstName: 'first1',
      middleName: 'middle',
      lastName: 'last1',
      dobDate: '2021-07-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.clickOnApplicantDashboard()
    await page.click('#ti-clients-link')

    expect(await page.innerText('#add-client')).toContain('Add Client')
  })

  describe('application flow with eligibility conditions', () => {
    // Create a program with 2 questions and an eligibility condition.
    const fullProgramName = 'Test program for eligibility navigation flows'
    const eligibilityQuestionId = 'ti-eligibility-number-q'

    beforeAll(async () => {
      const {
        page,
        adminQuestions,
        adminPrograms,
        adminPredicates,
        tiDashboard,
      } = ctx
      await loginAsAdmin(page)

      await adminQuestions.addNumberQuestion({
        questionName: eligibilityQuestionId,
      })
      await adminQuestions.addEmailQuestion({
        questionName: 'ti-eligibility-email-q',
      })

      // Add the full program.
      await adminPrograms.addProgram(fullProgramName)
      await adminPrograms.editProgramBlock(
        fullProgramName,
        'first description',
        [eligibilityQuestionId],
      )
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        fullProgramName,
        'Screen 1',
      )
      await adminPredicates.addPredicate(
        eligibilityQuestionId,
        /* action= */ null,
        'number',
        'is equal to',
        '5',
      )

      await adminPrograms.addProgramBlock(
        fullProgramName,
        'second description',
        ['ti-eligibility-email-q'],
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(fullProgramName)

      await logout(page)

      await loginAsTrustedIntermediary(page)

      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)
      const client: ClientInformation = {
        emailAddress: 'fake@sample.com',
        firstName: 'first',
        middleName: 'middle',
        lastName: 'last',
        dobDate: '2021-05-10',
      }
      await tiDashboard.createClient(client)
      await tiDashboard.expectDashboardContainClient(client)
    })

    it('correctly handles eligibility', async () => {
      const {page, tiDashboard, applicantQuestions} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.clickOnApplicantDashboard()

      // Verify TI gets navigated to the ineligible page with TI text.
      await applicantQuestions.applyProgram(fullProgramName)
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await tiDashboard.expectIneligiblePage()
      await validateScreenshot(page, 'not-eligible-page-ti')

      // Verify the 'may not qualify' tag shows on the program page
      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.clickOnApplicantDashboard()
      await applicantQuestions.seeEligibilityTag(fullProgramName, false)
      await validateScreenshot(page, 'program-page-not-eligible-ti')
      await applicantQuestions.clickApplyProgramButton(fullProgramName)

      // Verify the summary page shows the ineligible toast and the correct question is marked ineligible.
      await validateToastMessage(page, 'may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(page, 'application-summary-page-not-eligible-ti')

      // Change answer to one that passes eligibility and verify 'may qualify' tag appears on home page and as a toast.
      await applicantQuestions.clickEdit()
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
      await validateScreenshot(page, 'eligible-toast')
      await dismissToast(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.clickOnApplicantDashboard()
      await applicantQuestions.seeEligibilityTag(fullProgramName, true)
      await validateScreenshot(page, 'program-page-eligible-ti')
    })
  })
})
