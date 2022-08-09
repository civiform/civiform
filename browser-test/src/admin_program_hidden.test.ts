import { AdminPrograms, ApplicantQuestions, endSession, loginAsAdmin, loginAsGuest, logout, selectApplicantLanguage, startSession, } from './support'

describe('Hide a program that should not be public yet', () => {
  it('Create a new hidden program, verify applicants cannot see it on the home page', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(5000)

    await loginAsAdmin(page)
    const adminPrograms = new AdminPrograms(page)

    // Create a hidden program
    const programName = 'Hidden Program'
    const programDescription = 'Description'
    await adminPrograms.addProgram(programName, programDescription, '', true)
    await adminPrograms.publishAllPrograms()

    // Login as applicant
    await logout(page)
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.validateHeader('en-US')

    // Verify the program cannot be seen
    await applicantQuestions.expectProgramHidden(programName)

    await logout(page)
    await endSession(browser)
  })

  it('create a public program, verify applicants can see it on the home page', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(5000)

    await loginAsAdmin(page)
    const adminPrograms = new AdminPrograms(page)

    // Create a hidden program
    const programName = 'Public Program'
    const programDescription = 'Description'
    await adminPrograms.addProgram(programName, programDescription, '', false)
    await adminPrograms.publishAllPrograms()

    // Login as applicant
    await logout(page)

    // Verify applicants can now see the program
    await loginAsGuest(page)
    const applicantQuestions = new ApplicantQuestions(page)
    await selectApplicantLanguage(page, 'English')
    await applicantQuestions.expectProgramPublic(
      programName,
      programDescription
    )

    await endSession(browser)
  })
})
