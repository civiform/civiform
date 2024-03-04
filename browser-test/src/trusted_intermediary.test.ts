import {test, expect} from '@playwright/test'
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

test.describe('Trusted intermediaries', () => {
  const ctx = createTestContext()

  test('expect Client Date Of Birth to be Updated', async () => {
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

  test('expect client cannot be added with invalid date of birth', async () => {
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

  test('expect Dashboard Contain New Client', async () => {
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

  test('expect clients can be added without an email address', async () => {
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

  test('expect client email address to be updated', async () => {
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

  test('expect client ti notes and phone to be updated', async () => {
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
    const phoneNumber: string = '4256007121'
    const notes: string = 'Housing Assistance'
    await tiDashboard.createClient(client)
    await waitForPageJsLoad(page)
    await tiDashboard.updateClientTiNoteAndPhone(client, notes, phoneNumber)
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardClientContainsTiNoteAndFormattedPhone(
      client,
      notes,
      '(425) 600-7121',
    )
    await tiDashboard.expectEditFormContainsTiNoteAndPhone(
      client,
      notes,
      phoneNumber,
    )
    await validateScreenshot(page, 'edit-client-information-with-all-fields')
  })

  test('expect client email to be updated to empty', async () => {
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

    const card = page.locator(
      `.usa-card__container:has-text("${client.lastName}, ${client.firstName}")`,
    )
    const cardText = await card.innerText()
    expect(cardText).not.toContain('test@sample.com')
    expect(cardText).toContain(client.dobDate)
  })

  test('expect back button to land in dashboard in the edit client page', async () => {
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
      .getByRole('listitem')
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

  test('expect cancel button should not update client information', async () => {
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
      .getByRole('listitem')
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

  test('expect field errors', async () => {
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
      .getByRole('listitem')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(page)
    await page.waitForSelector('h2:has-text("Edit Client")')
    await page.fill('#edit-date-of-birth-input', '2027-12-20')
    await page.click('text="Save"')
    await validateScreenshot(page, 'edit-client-information-with-field-errors')
  })

  test('expect client cannot be added with invalid email address', async () => {
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

  test('ti landing page is the TI Dashboard', async () => {
    const {page} = ctx
    await loginAsTrustedIntermediary(page)
    await validateScreenshot(page, 'ti')
  })

  test('dashboard contains required indicator note and optional marker', async () => {
    const {page} = ctx
    await loginAsTrustedIntermediary(page)
    expect(await page.textContent('html')).toContain('Email address (optional)')
    expect(await page.textContent('html')).toContain(
      'Fields marked with a * are required.',
    )
  })

  test('TI sees the dashboard fully translated', async () => {
    const {
      page,
      tiDashboard,
    } = ctx

    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    await selectApplicantLanguage(page, 'Español')

    await validateScreenshot(page, 'ti-dashboard-spanish')
  })

  test('Applicant sees the program review page fully translated', async () => {
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
    await tiDashboard.clickOnViewApplications()

    await applicantQuestions.applyProgram(programName)
    await selectApplicantLanguage(page, 'Español')

    await validateScreenshot(page, 'applicant-program-spanish')
  })

  test('search For Client In TI Dashboard', async () => {
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

  test('incomplete dob and no name in the client search returns an error', async () => {
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

  test('incomplete dob with name in the client search returns client by name', async () => {
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

  test('empty search parameters returns all clients', async () => {
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

  test('managing trusted intermediary', async () => {
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

  test('logging in as a trusted intermediary', async () => {
    const {page} = ctx
    await loginAsTrustedIntermediary(page)
    expect(await page.innerText('#ti-dashboard-link')).toContain(
      'View and add clients',
    )
  })

  test('sees client name in sub-banner while applying for them', async () => {
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
    await tiDashboard.clickOnViewApplications()
    expect(await page.innerText('#ti-clients-link')).toContain(
      'Select a new client',
    )
    expect(await page.innerText('#ti-banner')).toContain(
      'You are applying for last1, first1. Are you trying to apply for a different client?',
    )
  })

  test('returns to TI dashboard from application when clicks the sub-banner link', async () => {
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
    await tiDashboard.clickOnViewApplications()
    await page.click('#ti-clients-link')

    expect(await page.innerText('#add-client')).toContain('Add Client')
  })

  test.describe('application flow with eligibility conditions', () => {
    // Create a program with 2 questions and an eligibility condition.
    const fullProgramName = 'Test program for eligibility navigation flows'
    const eligibilityQuestionId = 'ti-eligibility-number-q'

    test.beforeAll(async () => {
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

    test('correctly handles eligibility', async () => {
      const {page, tiDashboard, applicantQuestions} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.clickOnViewApplications()

      // Verify TI gets navigated to the ineligible page with TI text.
      await applicantQuestions.applyProgram(fullProgramName)
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await tiDashboard.expectIneligiblePage()
      await validateScreenshot(page, 'not-eligible-page-ti')

      // Verify the 'may not qualify' tag shows on the program page
      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.clickOnViewApplications()
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
      await tiDashboard.clickOnViewApplications()
      await applicantQuestions.seeEligibilityTag(fullProgramName, true)
      await validateScreenshot(page, 'program-page-eligible-ti')
    })
  })

  test.describe('application flow', () => {
    // Create a program with 1 question.
    const program1 = 'Test program 1'
    const program2 = 'Test program 2'
    const emailQuestionId = 'ti-email-question'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms, tiDashboard} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addEmailQuestion({
        questionName: emailQuestionId,
      })

      // Create program 1
      await adminPrograms.addProgram(program1)
      await adminPrograms.editProgramBlock(program1, 'first description', [
        emailQuestionId,
      ])

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(program1)

      // Create program 2
      await adminPrograms.addProgram(program2)
      await adminPrograms.editProgramBlock(program2, 'first description', [
        emailQuestionId,
      ])

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(program2)

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

    test('shows correct number of submitted applications in the client list', async () => {
      const {page, tiDashboard, applicantQuestions} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.expectClientContainsNumberOfApplications('0')

      // Apply to first program
      await tiDashboard.clickOnViewApplications()

      await applicantQuestions.applyProgram(program1)
      await applicantQuestions.answerEmailQuestion('fake@sample.com')
      await applicantQuestions.clickNext()
      await applicantQuestions.clickSubmit()

      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.expectClientContainsNumberOfApplications('1')
      await tiDashboard.expectClientContainsProgramNames(['Test program 1'])

      // Apply to second program
      await tiDashboard.clickOnViewApplications()

      await applicantQuestions.clickApplyProgramButton(program2)
      await applicantQuestions.clickSubmit()

      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.expectClientContainsNumberOfApplications('2')
      await tiDashboard.expectClientContainsProgramNames([
        'Test program 1',
        'Test program 2',
      ])
    })
  })

  test.describe('client list pagination', () => {
    test('shows 1 page and no previous or next buttons when there are 10 clients', async () => {
      const {page, tiDashboard} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await tiDashboard.createMultipleClients('myname', 10)
      const cardCount = await page.locator('.usa-card__container').count()
      expect(cardCount).toBe(10)

      // No 'Previous' button
      expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
        'usa-pagination__previous-page',
      )

      // No 'Next' button
      expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
        'usa-pagination__next-page',
      )

      // There should be a page 1 button
      await tiDashboard.expectPageNumberButton('1')

      // There should be no page 2 button
      await tiDashboard.expectPageNumberButtonNotPresent('2')

      // The page 1 button should be the current page
      expect(await page.innerHTML('.usa-current')).toContain('1')

      // There should be no ellipses
      expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
        'usa-pagination__overflow',
      )
    })

    test('shows 2 pages when there are 11 clients', async () => {
      const {page, tiDashboard} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await tiDashboard.createMultipleClients('myname', 11)

      // Page 1 should still only show 10 clients
      const cardCount = await page.locator('.usa-card__container').count()
      expect(cardCount).toBe(10)

      // No 'Previous' button because we're on the 1st page
      expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
        'usa-pagination__previous-page',
      )

      // There should be a 'Next' button
      expect(await page.innerHTML('.usa-pagination__list')).toContain(
        'usa-pagination__next-page',
      )

      await tiDashboard.expectPageNumberButton('1')
      await tiDashboard.expectPageNumberButton('2')

      expect(await page.innerHTML('.usa-current')).toContain('1')

      // There should be no ellipses
      expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
        'usa-pagination__overflow',
      )

      // Going to page 2
      await page.click('[aria-label=Page2]')

      const page2CardCount = await page.locator('.usa-card__container').count()
      expect(page2CardCount).toBe(1)

      // Now there should be a 'Previous' button
      expect(await page.innerHTML('.usa-pagination__list')).toContain(
        'usa-pagination__previous-page',
      )

      expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
        'usa-pagination__next-page',
      )

      await tiDashboard.expectPageNumberButton('1')
      await tiDashboard.expectPageNumberButton('2')

      expect(await page.innerHTML('.usa-current')).toContain('2')
    })

    test('shows 7 pages and no ellipses when there are 65 clients', async () => {
      const {page, tiDashboard} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await tiDashboard.createMultipleClients('myname', 65)

      await tiDashboard.expectPageNumberButton('1')
      await tiDashboard.expectPageNumberButton('2')
      await tiDashboard.expectPageNumberButton('3')
      await tiDashboard.expectPageNumberButton('4')
      await tiDashboard.expectPageNumberButton('5')
      await tiDashboard.expectPageNumberButton('6')
      await tiDashboard.expectPageNumberButton('7')
      await tiDashboard.expectPageNumberButtonNotPresent('8')

      // Going to page 7
      await page.click('[aria-label=Page7]')
      expect(await page.innerHTML('.usa-current')).toContain('7')

      // There should be no ellipses
      expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
        'usa-pagination__overflow',
      )

      expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
        'usa-pagination__next-page',
      )

      await validateScreenshot(
        page.locator('.usa-pagination'),
        'ti-pagination-no-ellipses',
      )
    })

    test('shows one ellipses on the right when more than 7 pages and current page is < 5', async () => {
      const {page, tiDashboard} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await tiDashboard.createMultipleClients('myname', 75)

      await tiDashboard.expectPageNumberButton('1')
      await tiDashboard.expectPageNumberButton('2')
      await tiDashboard.expectPageNumberButton('3')
      await tiDashboard.expectPageNumberButton('4')
      await tiDashboard.expectPageNumberButton('5')
      // The ellipses takes the place of 6 and 7 when current page is < 5
      await tiDashboard.expectPageNumberButtonNotPresent('6')
      await tiDashboard.expectPageNumberButtonNotPresent('7')
      await tiDashboard.expectPageNumberButton('8')

      // There should be an ellipses
      expect(await page.innerHTML('.usa-pagination__list')).toContain(
        'usa-pagination__overflow',
      )

      // Going to page 4
      await page.click('[aria-label=Page4]')
      expect(await page.innerHTML('.usa-current')).toContain('4')

      await tiDashboard.expectPageNumberButtonNotPresent('6')
      await tiDashboard.expectPageNumberButtonNotPresent('7')

      expect(await page.innerHTML('.usa-pagination__list')).toContain(
        'usa-pagination__overflow',
      )

      await validateScreenshot(
        page.locator('.usa-pagination'),
        'ti-pagination-ellipses-right',
      )
    })

    test('shows two ellipses when there are 9 pages and there is overflow on both sides', async () => {
      const {page, tiDashboard} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await tiDashboard.createMultipleClients('myname', 85)

      // Going to page 5
      await page.click('[aria-label=Page5]')
      expect(await page.innerHTML('.usa-current')).toContain('5')

      await tiDashboard.expectPageNumberButton('1')
      // An ellipses takes the place of 2 and 3 when current page is 5
      await tiDashboard.expectPageNumberButtonNotPresent('2')
      await tiDashboard.expectPageNumberButtonNotPresent('3')
      await tiDashboard.expectPageNumberButton('4')
      await tiDashboard.expectPageNumberButton('5')
      await tiDashboard.expectPageNumberButton('6')
      // An ellipses takes the place of 7 and 8 when current page is 5
      await tiDashboard.expectPageNumberButtonNotPresent('7')
      await tiDashboard.expectPageNumberButtonNotPresent('8')
      await tiDashboard.expectPageNumberButton('9')

      // There should be an ellipses
      expect(await page.innerHTML('.usa-pagination__list')).toContain(
        'usa-pagination__overflow',
      )

      await validateScreenshot(
        page.locator('.usa-pagination'),
        'ti-pagination-two-ellipses',
      )
    })

    test('shows one ellipses on the left when more than 7 pages and current page is one of the last 4 pages', async () => {
      const {page, tiDashboard} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await tiDashboard.createMultipleClients('myname', 85)

      // Going to page 6 via page 5
      await page.click('[aria-label=Page5]')
      await page.click('.usa-pagination__next-page')
      expect(await page.innerHTML('.usa-current')).toContain('6')

      await tiDashboard.expectPageNumberButton('1')
      // The ellipses is on the left
      await tiDashboard.expectPageNumberButtonNotPresent('2')
      await tiDashboard.expectPageNumberButtonNotPresent('3')
      await tiDashboard.expectPageNumberButtonNotPresent('4')
      await tiDashboard.expectPageNumberButton('5')
      await tiDashboard.expectPageNumberButton('6')
      await tiDashboard.expectPageNumberButton('7')
      await tiDashboard.expectPageNumberButton('8')
      await tiDashboard.expectPageNumberButton('9')

      // There should be an ellipses
      expect(await page.innerHTML('.usa-pagination__list')).toContain(
        'usa-pagination__overflow',
      )

      await validateScreenshot(
        page.locator('.usa-pagination'),
        'ti-pagination-ellipses-left',
      )
    })
  })
})
