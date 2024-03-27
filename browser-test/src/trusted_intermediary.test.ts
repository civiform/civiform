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
    await tiDashboard.expectSuccessAlertOnUpdate()
    await validateScreenshot(page.locator('main'), 'edit-client-success-alert')

    // The 'You are applying for...' banner should only be present when the TI
    // is actively applying for a client
    await tiDashboard.expectApplyingForBannerNotPresent()

    await page.click('#ti-dashboard-link')
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardContainClient(updatedClient)
  })

  test('verify success toast screenshot on adding new client', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'abc@abc.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2022-07-11',
    }
    await page.getByRole('link', {name: 'Add new client'}).click()
    await waitForPageJsLoad(page)

    // The 'You are applying for...' banner should only be present when the TI
    // is actively applying for a client
    await tiDashboard.expectApplyingForBannerNotPresent()

    await page.fill('#email-input', client.emailAddress)
    await page.fill('#first-name-input', client.firstName)
    await page.fill('#middle-name-input', client.middleName)
    await page.fill('#last-name-input', client.lastName)
    await page.fill('#date-of-birth-input', client.dobDate)

    await page.getByRole('button', {name: 'Save'}).click()
    await waitForPageJsLoad(page)
    await tiDashboard.expectSuccessAlertOnAddNewClient()
    await validateScreenshot(page, 'verify-success-toast-on-new-client')
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
    await page.getByRole('link', {name: 'Add new client'}).click()
    await waitForPageJsLoad(page)

    await page.fill('#email-input', client.emailAddress)
    await page.fill('#first-name-input', client.firstName)
    await page.fill('#middle-name-input', client.middleName)
    await page.fill('#last-name-input', client.lastName)
    await page.fill('#date-of-birth-input', client.dobDate)

    await page.getByRole('button', {name: 'Save'}).click()
    await validateScreenshot(page, 'add-client-invalid-dob')
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardNotContainClient(client)
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
    await validateScreenshot(page, 'add-clients-no-email')
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

    await page.click('#ti-dashboard-link')
    await waitForPageJsLoad(page)
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
      phoneNumber: '4256007121',
      notes: 'Housing Assistance',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardClientContainsTiNoteAndFormattedPhone(
      client,
      '(425) 600-7121',
    )
    await tiDashboard.expectEditFormContainsTiNoteAndPhone(client)
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
    await page.click('#ti-dashboard-link')
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
    await page.waitForSelector('h4:has-text("Search")')
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
    await page.fill('#date-of-birth-input', '2022-10-13')

    await page.click('text=Cancel')
    await waitForPageJsLoad(page)
    await page.waitForSelector('h4:has-text("Search")')
    // dob should not be updated
    await tiDashboard.expectDashboardContainClient(client)
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
    await page.fill('#date-of-birth-input', '2027-12-20')
    await page.click('text="Save"')

    await tiDashboard.expectSuccessAlertNotPresent()
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
    await page.getByRole('link', {name: 'Add new client'}).click()
    await waitForPageJsLoad(page)

    await page.fill('#email-input', client.emailAddress)
    await page.fill('#first-name-input', client.firstName)
    await page.fill('#middle-name-input', client.middleName)
    await page.fill('#last-name-input', client.lastName)
    await page.fill('#date-of-birth-input', client.dobDate)

    await page.getByRole('button', {name: 'Save'}).click()
    await validateScreenshot(page, 'cannot-add-client-with-existing-email')
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardNotContainClient(client)
  })

  test('expect client cannot be added with an existing email address', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)

    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client1: ClientInformation = {
      emailAddress: 'mail@test.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2023-07-11',
    }
    const client2: ClientInformation = {
      emailAddress: 'mail@test.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2023-07-11',
    }
    await tiDashboard.createClient(client1)

    await page.getByRole('link', {name: 'Add new client'}).click()
    await waitForPageJsLoad(page)

    await page.fill('#email-input', client2.emailAddress)
    await page.fill('#first-name-input', client2.firstName)
    await page.fill('#middle-name-input', client2.middleName)
    await page.fill('#last-name-input', client2.lastName)
    await page.fill('#date-of-birth-input', client2.dobDate)

    await page.getByRole('button', {name: 'Save'}).click()
    await validateScreenshot(page, 'cannot-add-client-invalid-email')
  })

  test('ti landing page is the TI Dashboard', async () => {
    const {page, tiDashboard} = ctx
    await loginAsTrustedIntermediary(page)
    await tiDashboard.expectApplyingForBannerNotPresent()
    await validateScreenshot(page, 'ti')
  })

  test('ti client form contains required indicator note and optional marker', async () => {
    const {page} = ctx
    await loginAsTrustedIntermediary(page)
    await page.getByRole('link', {name: 'Add new client'}).click()
    await waitForPageJsLoad(page)
    const content = await page.textContent('html')
    expect(content).toContain('Email (optional)')
    expect(content).toContain('Notes (optional)')
    expect(content).toContain('Middle name (optional)')
    expect(content).toContain('Enter phone number (optional)')
    expect(await page.textContent('html')).toContain(
      'Fields marked with a * are required.',
    )
  })

  test('Trusted intermediary sees the dashboard fully translated', async () => {
    const {page, tiDashboard} = ctx

    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    await selectApplicantLanguage(page, '繁體中文')

    await validateScreenshot(page, 'ti-dashboard-chinese')
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
      dobDate: '2021-12-07',
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
    await expect(
      page.getByRole('heading', {name: 'Displaying all clients'}),
    ).toBeVisible()

    await tiDashboard.searchByDateOfBirth('07', '12', '2021')
    await waitForPageJsLoad(page)
    await expect(
      page.getByRole('heading', {name: 'Displaying 2 clients'}),
    ).toBeVisible()
    await tiDashboard.expectDashboardContainClient(client2)
    await tiDashboard.expectDashboardContainClient(client3)
    await tiDashboard.expectDashboardNotContainClient(client1)

    // If the day is a single digit, the search still works
    await tiDashboard.searchByDateOfBirth('7', '12', '2021')
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardContainClient(client2)
    await tiDashboard.expectDashboardContainClient(client3)
    await tiDashboard.expectDashboardNotContainClient(client1)

    // We can clear the search and see all clients again
    await page.getByText('Clear search').click()
    await page.getByRole('button', {name: 'Search'}).click()
    await waitForPageJsLoad(page)
    await expect(
      page.getByRole('heading', {name: 'Displaying all clients'}),
    ).toBeVisible()
    await tiDashboard.expectDashboardContainClient(client1)
    await tiDashboard.expectDashboardContainClient(client2)
    await tiDashboard.expectDashboardContainClient(client3)
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
    await expect(page.getByTestId('displaying-clients')).toBeHidden()
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

    await expect(
      page.getByRole('heading', {name: 'Displaying 1 client'}),
    ).toBeVisible()
    await tiDashboard.expectDashboardContainClient(client1)
    await tiDashboard.expectDashboardNotContainClient(client2)
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
    await page.getByRole('link', {name: 'Select a new client'}).click()
    expect(await page.innerHTML('body')).toContain('id="name-search"')
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
    const program3 = 'Test program 3'
    const emailQuestionId = 'ti-email-question'
    const numberQuestionId = 'ti-number-question'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms, tiDashboard} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addEmailQuestion({
        questionName: emailQuestionId,
      })

      await adminQuestions.addNumberQuestion({
        questionName: numberQuestionId,
      })

      // Create program 1
      await adminPrograms.addProgram(program1)
      await adminPrograms.editProgramBlock(program1, 'description', [
        emailQuestionId,
      ])

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(program1)

      // Create program 2
      await adminPrograms.addProgram(program2)
      await adminPrograms.editProgramBlock(program2, 'description', [
        emailQuestionId,
      ])

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(program2)

      // Create program 3
      await adminPrograms.addProgram(program3)
      await adminPrograms.editProgramBlock(program3, 'description', [
        numberQuestionId,
      ])

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(program3)

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

      // Start application to third program, but don't submit
      await tiDashboard.clickOnViewApplications()

      await applicantQuestions.clickApplyProgramButton(program3)
      await applicantQuestions.clickContinue()
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()

      await tiDashboard.gotoTIDashboardPage(page)

      // Should only show submitted applications
      await tiDashboard.expectClientContainsNumberOfApplications('2')
      await tiDashboard.expectClientContainsProgramNames([
        'Test program 1',
        'Test program 2',
      ])
    })
  })

  test.describe('organization members table', () => {
    test('shows name, email and account status', async () => {
      const {page, tiDashboard} = ctx
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await validateScreenshot(
        page.getByTestId('org-members-table'),
        'org-members-table',
      )

      // Verifying the column headers
      expect(page.getByTestId('org-members-name')).not.toBeNull()
      expect(page.getByTestId('org-members-email')).not.toBeNull()
      expect(page.getByTestId('org-members-status')).not.toBeNull()
    })
    test('displays multiple rows when there are several TIs in the group', async () => {
      const {page, tiDashboard, adminTiGroups} = ctx
      await loginAsAdmin(page)
      await adminTiGroups.gotoAdminTIPage()
      await adminTiGroups.fillInGroupBasics('TI group', 'test group')
      await waitForPageJsLoad(page)
      await adminTiGroups.expectGroupExist('TI group')

      await adminTiGroups.editGroup('TI group')

      // Note that these emails will be replaced by 'fake-email@example.com'
      // to normalize the table contents (see src/support/index.ts).
      await adminTiGroups.addGroupMember('testti2@test.com')
      await adminTiGroups.addGroupMember('testti3@test.com')
      await adminTiGroups.addGroupMember('testti4@test.com')

      await logout(page)

      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await validateScreenshot(
        page.getByTestId('org-members-table'),
        'org-members-table-many',
      )
    })
  })
})
