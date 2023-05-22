import {
  ApplicantQuestions,
  ClientInformation,
  createTestContext,
  loginAsAdmin,
  loginAsTrustedIntermediary,
  logout,
  selectApplicantLanguage,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {ProgramVisibility} from './support/admin_programs'

describe('Validate program visibility is correct for applicants and TIs', () => {
  const ctx = createTestContext()
  it('Create a new hidden program, verify applicants cannot see it on the home page', async () => {
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
    await adminPrograms.publishAllPrograms()

    // Login as applicant
    await logout(page)
    await selectApplicantLanguage(page, 'English')
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.validateHeader('en-US')

    // Verify the program cannot be seen
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(page, 'program-visibility-hidden')

    await logout(page)
  })

  it('create a public program, verify applicants can see it on the home page', async () => {
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
    await adminPrograms.publishAllPrograms()

    // Login as applicant
    await logout(page)

    // Verify applicants can now see the program
    const applicantQuestions = new ApplicantQuestions(page)
    await selectApplicantLanguage(page, 'English')
    await applicantQuestions.expectProgramPublic(
      programName,
      programDescription,
    )
    await validateScreenshot(page, 'program-visibility-public')
  })

  it('create a program visible only to TIs, verify TIs can see it and other applicants cannot', async () => {
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
    await adminPrograms.publishAllPrograms()

    // Login as applicant, verify program is hidden
    await logout(page)
    const applicantQuestions = new ApplicantQuestions(page)
    await selectApplicantLanguage(page, 'English')
    await applicantQuestions.expectProgramHidden(programName)
    await validateScreenshot(
      page,
      'program-visibility-ti-only-hidden-from-applicant',
    )

    // Login as TI, verify program is visible
    await logout(page)
    await loginAsTrustedIntermediary(page)
    await selectApplicantLanguage(page, 'English')
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
    await tiDashboard.clickOnApplicantDashboard()
    await applicantQuestions.expectProgramPublic(
      programName,
      programDescription,
    )
    await validateScreenshot(page, 'program-visibility-ti-only-visible-to-ti')
  })
})
