import {
  ClientInformation,
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTrustedIntermediary,
  waitForPageJsLoad,
  selectApplicantLanguage,
  validateScreenshot,
  logout,
  AdminQuestions,
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

  it('ti landing page is the TI Dashboard', async () => {
    const {page} = ctx
    await loginAsTrustedIntermediary(page)
    await selectApplicantLanguage(page, 'English')
    await validateScreenshot(page, 'ti')
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
    const client3: ClientInformation = {
      emailAddress: 'fake3@sample.com',
      firstName: 'first3',
      middleName: 'middle',
      lastName: 'last3',
      dobDate: '2021-12-10',
    }
    await tiDashboard.createClient(client3)

    await tiDashboard.searchByDateOfBirth(client3.dobDate)
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardContainClient(client3)
    await tiDashboard.expectDashboardNotContainClient(client1)
    await tiDashboard.expectDashboardNotContainClient(client2)
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
      'VIEW AND ADD CLIENTS',
    )
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
      await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')

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
      await adminPredicates.addLegacyPredicate(
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
      await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')
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
      await applicantQuestions.validateToastMessage('may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(page, 'application-summary-page-not-eligible-ti')

      // Change answer to one that passes eligibility and verify 'may qualify' tag appears on home page
      await applicantQuestions.clickEdit()
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()
      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.clickOnApplicantDashboard()
      await applicantQuestions.seeEligibilityTag(fullProgramName, true)
      await validateScreenshot(page, 'program-page-eligible-ti')
    })
  })
})
