import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  ClientInformation,
  loginAsAdmin,
  loginAsTrustedIntermediary,
  waitForPageJsLoad,
  validateScreenshot,
  validateToastMessage,
  logout,
  AdminQuestions,
  selectApplicantLanguage,
  disableFeatureFlag,
} from './support'

test.describe('Trusted intermediaries', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('expect Client Date Of Birth to be Updated', async ({
    page,
    tiDashboard,
  }) => {
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
  test('expect client info to be updated with empty emails', async ({
    page,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: '',
      firstName: 'Tony',
      middleName: '',
      lastName: 'Stark',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardContainClient(client)
    await tiDashboard.updateClientTiNoteAndPhone(
      client,
      'Technology',
      '4259746122',
    )
    await tiDashboard.expectSuccessAlertOnUpdate()

    await page.click('#ti-dashboard-link')
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardContainClient(client)
  })

  test('verify success toast screenshot on adding new client', async ({
    page,
    tiDashboard,
    applicantQuestions,
  }) => {
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
    await page.getByRole('link', {name: 'Start an application'}).click()
    await applicantQuestions.expectProgramsPage()
  })

  test('expect client cannot be added with invalid date of birth', async ({
    page,
    tiDashboard,
  }) => {
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

  test('expect Dashboard Contain New Client', async ({page, tiDashboard}) => {
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

  test('expect clients can be added without an email address', async ({
    page,
    tiDashboard,
  }) => {
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

  test('expect client email address to be updated', async ({
    page,
    tiDashboard,
  }) => {
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

  test('expect client ti notes and phone to be updated', async ({
    page,
    tiDashboard,
  }) => {
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

  test('expect client email to be updated to empty', async ({
    page,
    tiDashboard,
  }) => {
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

  test('expect back button to land in dashboard in the edit client page', async ({
    page,
    tiDashboard,
  }) => {
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
    await tiDashboard.expectEditHeadingToBeVisible()

    await page.click('text=Back to client list')
    await waitForPageJsLoad(page)
    await tiDashboard.expectSearchHeadingToBeVisible()
  })

  test('expect cancel button should not update client information', async ({
    page,
    tiDashboard,
  }) => {
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
    await tiDashboard.expectEditHeadingToBeVisible()
    // update client dob
    await page.fill('#date-of-birth-input', '2022-10-13')

    await page.click('text=Cancel')
    await waitForPageJsLoad(page)
    await tiDashboard.expectSearchHeadingToBeVisible()
    // dob should not be updated
    await tiDashboard.expectDashboardContainClient(client)
  })

  test('expect field errors', async ({page, tiDashboard}) => {
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
    await tiDashboard.expectEditHeadingToBeVisible()
    await page.fill('#date-of-birth-input', '2027-12-20')
    await page.click('text="Save"')

    await tiDashboard.expectSuccessAlertNotPresent()
    await validateScreenshot(page, 'edit-client-information-with-field-errors')
  })

  test('expect client cannot be added with invalid email address', async ({
    page,
    tiDashboard,
  }) => {
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

  test('expect client cannot be added with an existing email address', async ({
    page,
    tiDashboard,
  }) => {
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

  test('ti landing page is the TI Dashboard', async ({page, tiDashboard}) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.expectApplyingForBannerNotPresent()
    await validateScreenshot(page, 'ti')
  })

  test('ti client form contains required indicator note and optional marker', async ({
    page,
  }) => {
    await loginAsTrustedIntermediary(page)
    await page.getByRole('link', {name: 'Add new client'}).click()
    await waitForPageJsLoad(page)
    const content = await page.textContent('html')
    expect(content).toContain('Email (optional)')
    expect(content).toContain('Notes (optional)')
    expect(content).toContain('Middle name (optional)')
    expect(content).toContain('Phone number (optional)')
    expect(await page.textContent('html')).toContain(
      'Fields marked with a * are required.',
    )
  })

  test('Trusted intermediary sees the dashboard fully translated', async ({
    page,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    await selectApplicantLanguage(page, '繁體中文')

    await validateScreenshot(page, 'ti-dashboard-chinese')
  })

  test('Applicant sees the program review page fully translated', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
    adminTranslations,
    tiDashboard,
  }) => {
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

  test('search For Client In TI Dashboard', async ({page, tiDashboard}) => {
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

  test('incomplete dob and no name in the client search returns an error', async ({
    page,
    tiDashboard,
  }) => {
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

  test('incomplete dob with name in the client search returns client by name', async ({
    page,
    tiDashboard,
  }) => {
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

  test('managing trusted intermediary', async ({page, adminTiGroups}) => {
    await loginAsAdmin(page)
    await adminTiGroups.gotoAdminTIPage()
    await adminTiGroups.fillInGroupBasics('group name', 'group description')
    await adminTiGroups.expectGroupExist('group name', 'group description')
    await validateScreenshot(page, 'ti-groups-page')

    // validate error message if empty name
    await adminTiGroups.editGroup('group name')
    await page.click('text="Add"')
    await validateToastMessage(page, 'Must provide email address.')

    // validate adding valid email address works
    await adminTiGroups.editGroup('group name')
    await adminTiGroups.addGroupMember('foo@bar.com')
    await adminTiGroups.expectGroupMemberExist('<Unnamed User>', 'foo@bar.com')
    await validateScreenshot(page, 'manage-ti-group-members-page')
  })

  test('sort trusted intermediaries based on selection', async ({
    page,
    adminTiGroups,
  }) => {
    await loginAsAdmin(page)
    await test.step('set up group aaa with 3 memebers', async () => {
      await adminTiGroups.gotoAdminTIPage()
      await adminTiGroups.fillInGroupBasics('aaa', 'aaa')
      await adminTiGroups.editGroup('aaa')
      await adminTiGroups.addGroupMember('foo@bar.com')
      await adminTiGroups.addGroupMember('foo2@bar.com')
      await adminTiGroups.addGroupMember('foo3@bar.com')
    })

    await test.step('set up group bbb with 0 members', async () => {
      await adminTiGroups.gotoAdminTIPage()
      await adminTiGroups.fillInGroupBasics('bbb', 'bbb')
    })

    await test.step('set up group ccc with 1 members', async () => {
      await adminTiGroups.gotoAdminTIPage()
      await adminTiGroups.fillInGroupBasics('ccc', 'ccc')
      await adminTiGroups.editGroup('ccc')
      await adminTiGroups.addGroupMember('foo4@bar.com')
    })

    await adminTiGroups.gotoAdminTIPage()
    await page.locator('#cf-ti-list').selectOption('tiname-asc')
    const tiNamesAsc = await page.getByTestId('ti-info').allInnerTexts()
    console.log(page.getByTestId('ti-info'))
    expect(tiNamesAsc).toEqual(['aaa\naaa', 'bbb\nbbb', 'ccc\nccc'])
    await validateScreenshot(page, 'ti-list-sort-dropdown-tiname-asc')

    await page.locator('#cf-ti-list').selectOption('tiname-desc')
    const tiNamesDesc = await page.getByTestId('ti-info').allInnerTexts()
    expect(tiNamesDesc).toEqual(['ccc\nccc', 'bbb\nbbb', 'aaa\naaa'])
    await validateScreenshot(page, 'ti-list-sort-dropdown-tiname-desc')

    await page.locator('#cf-ti-list').selectOption('nummember-desc')
    const tiMemberDesc = await page.getByTestId('ti-member').allInnerTexts()
    expect(tiMemberDesc).toEqual([
      'Members: 3\nClients: 0',
      'Members: 1\nClients: 0',
      'Members: 0\nClients: 0',
    ])
    await validateScreenshot(page, 'ti-list-sort-dropdown-nummember-desc')

    await page.locator('#cf-ti-list').selectOption('nummember-asc')
    const tiMemberAsc = await page.getByTestId('ti-member').allInnerTexts()
    expect(tiMemberAsc).toEqual([
      'Members: 0\nClients: 0',
      'Members: 1\nClients: 0',
      'Members: 3\nClients: 0',
    ])
    await validateScreenshot(page, 'ti-list-sort-dropdown-nummember-asc')
  })

  test('logging in as a trusted intermediary', async ({page}) => {
    await loginAsTrustedIntermediary(page)
    expect(await page.innerText('#ti-dashboard-link')).toContain(
      'View and add clients',
    )
  })

  test('sees client name in sub-banner while applying for them', async ({
    page,
    tiDashboard,
  }) => {
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

  test('returns to TI dashboard from application when clicks the sub-banner link', async ({
    page,
    tiDashboard,
  }) => {
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

    test.beforeEach(
      async ({
        page,
        adminQuestions,
        adminPrograms,
        adminPredicates,
        tiDashboard,
      }) => {
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
        await adminPredicates.addPredicates({
          questionName: eligibilityQuestionId,
          scalar: 'number',
          operator: 'is equal to',
          value: '5',
        })

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
      },
    )

    test('correctly handles eligibility', async ({
      page,
      tiDashboard,
      applicantQuestions,
    }) => {
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
      await applicantQuestions.expectMayNotBeEligibileAlertToBeVisible()

      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(page, 'application-summary-page-not-eligible-ti')

      // Change answer to one that passes eligibility and verify 'may qualify' tag appears on home page and as a toast.
      await applicantQuestions.clickEdit()
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
      await validateScreenshot(page, 'eligible-toast')
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

    test.beforeEach(
      async ({page, adminQuestions, adminPrograms, tiDashboard}) => {
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
      },
    )

    test('shows correct number of submitted applications in the client list', async ({
      page,
      tiDashboard,
      applicantQuestions,
    }) => {
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
    test('shows name, email and account status', async ({
      page,
      tiDashboard,
    }) => {
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)

      await tiDashboard.goToAccountSettingsPage(page)
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

    test('displays multiple rows when there are several TIs in the group', async ({
      page,
      tiDashboard,
      adminTiGroups,
    }) => {
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

      await tiDashboard.goToAccountSettingsPage(page)
      await waitForPageJsLoad(page)

      await validateScreenshot(
        page.getByTestId('org-members-table'),
        'org-members-table-many',
      )
    })
  })

  test.describe('ti navigation', () => {
    test('marks the correct tab as current and matches screenshot', async ({
      page,
      tiDashboard,
    }) => {
      await test.step('Login as a TI and go to the dashboard', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.gotoTIDashboardPage(page)
        await waitForPageJsLoad(page)
      })

      await test.step('Validate that TI navigation renders correctly with aria-current on dashboard page', async () => {
        await validateScreenshot(
          page.getByTestId('ti-nav'),
          'ti-navigation-dashboard',
        )

        // When we're on the client list page (the dashboard), the current tab should be the client list tab.
        expect(
          await page.getAttribute('#account-settings-link', 'aria-current'),
        ).toBeNull()
        expect(
          await page.getAttribute('#client-list-link', 'aria-current'),
        ).not.toBeNull()
      })

      await test.step('Go to the TI Account Settings page', async () => {
        await tiDashboard.goToAccountSettingsPage(page)
        await waitForPageJsLoad(page)
      })

      await test.step('Validate that TI navigation renders correctly with aria-current on account settings page', async () => {
        await validateScreenshot(
          page.getByTestId('ti-nav'),
          'ti-navigation-account-settings',
        )

        // When we're on the account settings page, the current tab should be the account settings tab.
        expect(
          await page.getAttribute('#account-settings-link', 'aria-current'),
        ).not.toBeNull()
        expect(
          await page.getAttribute('#client-list-link', 'aria-current'),
        ).toBeNull()
      })
    })
  })

  test.describe('pre-populating TI client info with PAI questions', () => {
    const clientInfo: ClientInformation = {
      emailAddress: 'test@email.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2001-01-01',
      phoneNumber: '9178675309',
    }

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await test.step('create a program with PAI questions', async () => {
        await loginAsAdmin(page)
        await adminQuestions.addDateQuestion({
          questionName: 'dob',
          questionText: 'Date of birth',
          universal: true,
          primaryApplicantInfo: true,
        })
        await adminQuestions.addNameQuestion({
          questionName: 'name',
          questionText: 'Name',
          universal: true,
          primaryApplicantInfo: true,
        })
        await adminQuestions.addPhoneQuestion({
          questionName: 'phone',
          questionText: 'Phone',
          universal: true,
          primaryApplicantInfo: true,
        })
        await adminQuestions.addEmailQuestion({
          questionName: 'email',
          questionText: 'Email',
          universal: true,
          primaryApplicantInfo: true,
        })
        await adminPrograms.addAndPublishProgramWithQuestions(
          ['dob', 'name', 'phone', 'email'],
          'PAI Program',
        )
        await logout(page)
      })
    })

    test('client info is pre-populated in the application', async ({
      page,
      applicantQuestions,
      tiDashboard,
    }) => {
      await test.step('login as TI, add a client, and apply', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.createClientAndApply(clientInfo)
        await applicantQuestions.clickApplyProgramButton('PAI Program')
      })

      await test.step('verify client info is pre-populated in the application', async () => {
        expect(await page.innerText('#application-summary')).toContain(
          '01/01/2001',
        )
        expect(await page.innerText('#application-summary')).toContain(
          'first middle last',
        )
        expect(await page.innerText('#application-summary')).toContain(
          '+1 917-867-5309',
        )
        expect(await page.innerText('#application-summary')).toContain(
          'test@email.com',
        )
        await validateScreenshot(page, 'pai-program-application-preview')
      })

      await test.step('verify client info is pre-populated in the application after clicking continue', async () => {
        await applicantQuestions.clickContinue()

        await expect(
          page.getByRole('textbox', {name: 'Date of birth'}),
        ).toHaveValue('2001-01-01')
        await expect(
          page.getByRole('textbox', {name: 'First name'}),
        ).toHaveValue('first')
        await expect(
          page.getByRole('textbox', {name: 'Middle name'}),
        ).toHaveValue('middle')
        await expect(
          page.getByRole('textbox', {name: 'Last name'}),
        ).toHaveValue('last')
        await expect(
          page.getByRole('textbox', {name: 'Phone number'}),
        ).toHaveValue('(917) 867-5309')
        await expect(page.getByRole('textbox', {name: 'Email'})).toHaveValue(
          'test@email.com',
        )
        await validateScreenshot(page, 'pai-program-application')
      })

      await test.step('submitting the application without changing any values succeeds', async () => {
        await applicantQuestions.clickNext()
        await applicantQuestions.clickSubmit()
        await applicantQuestions.expectConfirmationPage()
      })
    })

    test('updating answers that are prefilled with PAI data works', async ({
      page,
      applicantQuestions,
      tiDashboard,
    }) => {
      await test.step('login as TI, add a client, and apply', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.createClientAndApply(clientInfo)
        await applicantQuestions.clickApplyProgramButton('PAI Program')
      })

      await test.step('fill in the name question with different values', async () => {
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerNameQuestion('newfirst', 'newlast')
        await applicantQuestions.clickNext()
      })

      await test.step('verify the new values for name are shown in the application and the other values are unchanged', async () => {
        expect(await page.innerText('#application-summary')).toContain(
          '01/01/2001',
        )
        expect(await page.innerText('#application-summary')).toContain(
          'newfirst middle newlast',
        )
        expect(await page.innerText('#application-summary')).toContain(
          '+1 917-867-5309',
        )
        expect(await page.innerText('#application-summary')).toContain(
          'test@email.com',
        )
      })

      await test.step('submitting the application with changed values succeeds', async () => {
        await applicantQuestions.clickSubmit()
        await applicantQuestions.expectConfirmationPage()
      })
    })

    test('data from PAI questions answered in the application shows up in the TI Dashboard', async ({
      page,
      applicantQuestions,
      tiDashboard,
    }) => {
      await test.step('login as TI and add a client with partial data', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.gotoTIDashboardPage(page)
        const partialClientInfo: ClientInformation = {
          emailAddress: '',
          firstName: 'first',
          middleName: 'middle',
          lastName: 'last',
          dobDate: '2001-01-01',
        }
        await tiDashboard.createClient(partialClientInfo)
        await waitForPageJsLoad(page)
      })

      await test.step('login as TI and apply to program on behalf of client', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.gotoTIDashboardPage(page)
        await tiDashboard.clickOnViewApplications()
        await applicantQuestions.clickApplyProgramButton('PAI Program')
        await applicantQuestions.clickContinue()
      })

      await test.step('fill out the phone and email questions and submit the application', async () => {
        await applicantQuestions.answerPhoneQuestion('7188675309')
        await applicantQuestions.answerEmailQuestion('email@example.com')
        await applicantQuestions.clickNext()
        await applicantQuestions.clickSubmit()
      })

      const newClientInfo: ClientInformation = {
        emailAddress: 'email@example.com',
        firstName: 'first',
        middleName: 'middle',
        lastName: 'last',
        dobDate: '2001-01-01',
        notes: 'Notes',
      }
      await test.step('verify the client info is shown in the TI Dashboard', async () => {
        await tiDashboard.gotoTIDashboardPage(page)
        await waitForPageJsLoad(page)
        await tiDashboard.expectDashboardContainClient(newClientInfo)
        await tiDashboard.expectDashboardClientContainsTiNoteAndFormattedPhone(
          newClientInfo,
          '(718) 867-5309',
        )
        await validateScreenshot(page, 'pai-ti-dash')
      })
    })
  })

  test.describe('ti can add suffix information with suffix feature flag enabled', () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'name_suffix_dropdown_enabled')
    })

    test('TI is able to fill out name suffix info for the applicant', async ({
      page,
      tiDashboard,
    }) => {
      await loginAsTrustedIntermediary(page)
      const client: ClientInformation = {
        emailAddress: 'test@sample.com',
        firstName: 'first',
        middleName: 'middle',
        lastName: 'last',
        nameSuffix: 'II',
        dobDate: '1995-06-10',
      }

      await test.step('adds an applicant with name suffix', async () => {
        await tiDashboard.createClient(client)
        await waitForPageJsLoad(page)
        await page.click('#ti-dashboard-link')
        await waitForPageJsLoad(page)

        await tiDashboard.expectDashboardContainClient(client)
      })

      await test.step('name suffix field is filled with preset value', async () => {
        await page.click('#edit-client')
        await waitForPageJsLoad(page)

        await expect(page.getByLabel('Suffix')).toHaveValue('II')
      })
    })
  })
})
