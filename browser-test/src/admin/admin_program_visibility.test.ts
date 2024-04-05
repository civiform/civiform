import {test} from '@playwright/test'
import {
  ApplicantQuestions,
  ClientInformation,
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  loginAsTrustedIntermediary,
  logout,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'
import {TEST_USER_DISPLAY_NAME} from '../support/config'
import {ProgramVisibility} from '../support/admin_programs'

test.describe('Validate program visibility is correct for applicants and TIs', () => {
  const ctx = createTestContext()
  test('Create a new hidden program, verify applicants cannot see it on the home page', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)

    // Create a hidden program
    const programName = 'Hidden program'
    const programDescription = 'Description'
    await adminPrograms.addProgram(
      programName,
      programDescription,
      'https://usa.gov',
      ProgramVisibility.HIDDEN,
    )
    await adminPrograms.publishAllDrafts()

    // Login as applicant
    await logout(page)
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.validateHeader('en-US')

    // Verify the program cannot be seen
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(page, 'program-visibility-hidden')

    await logout(page)
  })

  test('create a public program, verify applicants can see it on the home page', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)

    const programName = 'Public program'
    const programDescription = 'Description'
    await adminPrograms.addProgram(
      programName,
      programDescription,
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
    )
    await adminPrograms.publishAllDrafts()

    // Login as applicant
    await logout(page)

    // Verify applicants can now see the program
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.expectProgramPublic(
      programName,
      programDescription,
    )
    await validateScreenshot(page, 'program-visibility-public')
  })

  test('create a program visible only to TIs, verify TIs can see it and other applicants cannot', async () => {
    const {page, tiDashboard, adminPrograms} = ctx

    await loginAsAdmin(page)

    const programName = 'TI-only program'
    const programDescription = 'Description'
    await adminPrograms.addProgram(
      programName,
      programDescription,
      'https://usa.gov',
      ProgramVisibility.TI_ONLY,
    )
    await adminPrograms.publishAllDrafts()

    // Login as applicant, verify program is hidden
    await logout(page)
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(
      page,
      'program-visibility-ti-only-hidden-from-applicant',
    )

    // Login as TI, verify program is visible
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
    await tiDashboard.clickOnViewApplications()
    await applicantQuestions.expectProgramPublic(
      programName,
      programDescription,
    )
    await validateScreenshot(page, 'program-visibility-ti-only-visible-to-ti')
  })
  test('create a program visible only for Selected TIs, verify those TIs can see it and other applicants/TIs cannot', async () => {
    const {page, tiDashboard, adminPrograms, adminTiGroups} = ctx

    await loginAsAdmin(page)
    await adminTiGroups.gotoAdminTIPage()
    await adminTiGroups.fillInGroupBasics('groupOne', 'groupOne description')
    await adminTiGroups.fillInGroupBasics('groupTwo', 'groupTwo description')
    await adminTiGroups.editGroup('groupOne')
    await adminTiGroups.addGroupMember('groupOne@bar.com')
    await adminTiGroups.gotoAdminTIPage()
    await adminTiGroups.editGroup('groupTwo')
    // Add the TEST_USER_DISPLAY_NAME user to GroupTwo, which is the user that gets logged into upon calling loginAsTestUser()
    await adminTiGroups.addGroupMember(TEST_USER_DISPLAY_NAME)
    await logout(page)

    await loginAsAdmin(page)
    const programName = 'Select TI program'
    const programDescription = 'Description'
    await adminPrograms.addProgram(
      programName,
      programDescription,
      'https://usa.gov',
      ProgramVisibility.SELECT_TI,
      'admin description',
      /* isCommonIntake= */ false,
      'groupTwo',
    )
    await adminPrograms.publishAllDrafts()

    // Login as applicant, verify program is hidden
    await logout(page)
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(
      page,
      'program-visibility-select-ti-hidden-from-applicant',
    )

    // Login as any TI, verify program is invisible
    await logout(page)
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    const client: ClientInformation = {
      emailAddress: 'fakeOne@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-05-10',
    }
    await tiDashboard.createClient(client)

    await tiDashboard.expectDashboardContainClient(client)
    await tiDashboard.clickOnViewApplications()
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(page, 'program-visibility-hidden-from-other-tis')
    await logout(page)

    await loginAsTestUser(page, 'a:has-text("Log in")', true)
    const clientTwo: ClientInformation = {
      emailAddress: 'fakeTwo@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-05-10',
    }
    await tiDashboard.createClient(clientTwo)
    await tiDashboard.expectDashboardContainClient(clientTwo)
    await tiDashboard.clickOnViewApplications()
    await applicantQuestions.expectProgramPublic(
      programName,
      programDescription,
    )
    await validateScreenshot(page, 'program-visibility-for-selected-tis')
  })
  test('create a program visible only for Selected TIs, then choose TI_Only, all TIs can see the program', async () => {
    const {page, tiDashboard, adminPrograms, adminTiGroups} = ctx

    await loginAsAdmin(page)
    await adminTiGroups.gotoAdminTIPage()
    await adminTiGroups.fillInGroupBasics('groupOne', 'groupOne description')
    await adminTiGroups.fillInGroupBasics('groupTwo', 'groupTwo description')
    await adminTiGroups.editGroup('groupOne')
    await adminTiGroups.addGroupMember('groupOne@bar.com')
    await adminTiGroups.gotoAdminTIPage()
    await adminTiGroups.editGroup('groupTwo')
    // Add the TEST_USER_DISPLAY_NAME user to GroupTwo, which is the user that gets logged into upon calling loginAsTestUser()
    await adminTiGroups.addGroupMember(TEST_USER_DISPLAY_NAME)
    await logout(page)

    await loginAsAdmin(page)
    const programName = 'Select TI to TI Only'
    const programDescription = 'Description'
    await adminPrograms.addProgram(
      programName,
      programDescription,
      'https://usa.gov',
      ProgramVisibility.SELECT_TI,
      'admin description',
      /* isCommonIntake= */ false,
      'groupTwo',
    )
    await adminPrograms.publishAllDrafts()

    // Login as applicant, verify program is hidden
    await logout(page)
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(
      page,
      'program-visibility-select-ti-hidden-from-applicant',
    )

    // Login as any TI, verify program is invisible
    await logout(page)
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'fakeThree@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-05-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardContainClient(client)
    await tiDashboard.clickOnViewApplications()
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(
      page,
      'program-visibility-hidden-from-other-tis-in-selectti-mode',
    )
    await logout(page)

    // login again as Admin and change the visibility to TI_Only, check if they can see the program
    await loginAsAdmin(page)
    await adminPrograms.editProgram(programName, ProgramVisibility.TI_ONLY)
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    await tiDashboard.expectDashboardContainClient(client)
    await tiDashboard.clickOnViewApplications()
    await applicantQuestions.expectProgramPublic(
      programName,
      programDescription,
    )
    await validateScreenshot(
      page,
      'program-visibility-changes-all-ti-can-see-program',
    )
  })
  test('create a program with disabled visibility, verify it is hidden from applicants and TIs', async () => {
    const {page, tiDashboard, adminPrograms} = ctx

    await enableFeatureFlag(page, 'disabled_visibility_condition_enabled')
    await loginAsAdmin(page)

    const programName = 'Disabled program'
    const programDescription = 'Description'
    await adminPrograms.addProgram(
      programName,
      programDescription,
      'https://usa.gov',
      ProgramVisibility.DISABLED,
    )
    await adminPrograms.publishAllDrafts()

    // Login as applicant, verify program is hidden
    await logout(page)
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(
      page,
      'program-visibility-disabled-hidden-from-applicant',
    )

    // Login as TI, verify program is hidden
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
    await tiDashboard.clickOnViewApplications()
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(page, 'program-visibility-disabled-hidden-from-ti')
  })
})
