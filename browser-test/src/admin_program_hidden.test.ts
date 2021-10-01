import { startSession, loginAsProgramAdmin, loginAsAdmin, AdminQuestions, AdminPrograms, endSession, logout, loginAsTestUser, selectApplicantLanguage, ApplicantQuestions, userDisplayName } from './support'

describe('Hide a program that should not be public yet', () => {
  it('Create a new hidden program, verify applicants cannot see it on the home page, unhide the program, verify applicant access', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(5000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    // Create a hidden program
    const programName = 'Hidden Program';
    const programDescription = 'Description';
    await adminPrograms.addProgram(programName, programDescription, "", true);
    await adminPrograms.publishAllPrograms();

    // Login as applicant
    await logout(page);
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');
    const applicantQuestions = new ApplicantQuestions(page);
    await applicantQuestions.validateHeader('en-US');

    // Verify the program cannot be seen
    await applicantQuestions.expectProgramHidden(programName);

    await logout(page);
    await loginAsAdmin(page);

    // Unhide the program and publish
    await adminPrograms.createPublicVersion(programName);
    await adminPrograms.publishAllPrograms();

    await logout(page);

    // Verify applicants can now see the program
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');
    await applicantQuestions.expectProgramPublic(programName, programDescription);

    await endSession(browser);
  })
})
