import {Page} from 'playwright'
import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  startSession,
  resetSession,
} from './support'

describe('Dropdown question for applicant flow', () => {
  let pageObject: Page

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single dropdown question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single dropdown'

    beforeAll(async () => {
      // As admin, create program with single dropdown question.
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addDropdownQuestion({
        questionName: 'dropdown-color-q',
        options: ['red', 'green', 'orange', 'blue'],
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['dropdown-color-q'],
        programName,
      )

      await logout(pageObject)
    })

    it('with selected option submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDropdownQuestion('green')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with no selection does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      // Click next without selecting anything.
      await applicantQuestions.clickNext()

      const dropdownId = '.cf-question-dropdown'
      expect(await pageObject.innerText(dropdownId)).toContain(
        'This question is required.',
      )
    })
  })

  describe('multiple dropdown questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple dropdowns'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addDropdownQuestion({
        questionName: 'dropdown-fave-vacation-q',
        options: ['beach', 'mountains', 'city', 'cruise'],
      })
      await adminQuestions.addDropdownQuestion({
        questionName: 'dropdown-fave-color-q',
        options: ['red', 'green', 'orange', 'blue'],
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['dropdown-fave-color-q'],
        'dropdown-fave-vacation-q', // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with selected options submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDropdownQuestion('beach', 0)
      await applicantQuestions.answerDropdownQuestion('blue', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDropdownQuestion('red', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })
  })
})
