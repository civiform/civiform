import {Page} from 'playwright'
import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  logout,
  resetSession,
  selectApplicantLanguage,
  startSession,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Applicant navigation flow', () => {
  let pageObject: Page

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('navigation with four blocks', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for navigation flows'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addDateQuestion({questionName: 'nav-date-q'})
      await adminQuestions.addEmailQuestion({questionName: 'nav-email-q'})
      await adminQuestions.addAddressQuestion({
        questionName: 'nav-address-q',
      })
      await adminQuestions.addRadioButtonQuestion({
        questionName: 'nav-radio-q',
        options: ['one', 'two', 'three'],
      })
      await adminQuestions.addStaticQuestion({questionName: 'nav-static-q'})

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, 'first description', [
        'nav-date-q',
        'nav-email-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'second description', [
        'nav-static-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'third description', [
        'nav-address-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'fourth description', [
        'nav-radio-q',
      ])

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)

      await logout(pageObject)
    })

    it('clicking previous on first block goes to summary page', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickPrevious()

      // Assert that we're on the preview page.
      expect(await pageObject.innerText('h1')).toContain(
        'Program application preview',
      )
      await validateScreenshot(pageObject)
    })

    it('clicking previous on later blocks goes to previous blocks', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      // Fill out the first block and click next
      await applicantQuestions.answerDateQuestion('2021-11-01')
      await applicantQuestions.answerEmailQuestion('test1@gmail.com')
      await applicantQuestions.clickNext()

      // Nothing to fill in since this is the static question block
      await applicantQuestions.clickNext()

      // Fill out address question and click next
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
      )
      await applicantQuestions.clickNext()

      // Click previous and see file upload page with address
      await applicantQuestions.clickPrevious()
      await applicantQuestions.checkAddressQuestionValue(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
      )

      // Click previous and see static question page
      await applicantQuestions.clickPrevious()
      await applicantQuestions.seeStaticQuestion('static question text')

      // Click previous and see date and name questions
      await applicantQuestions.clickPrevious()
      await applicantQuestions.checkDateQuestionValue('2021-11-01')
      await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')

      // Assert that we're on the preview page.
      await applicantQuestions.clickPrevious()
      expect(await pageObject.innerText('h1')).toContain(
        'Program application preview',
      )
      await validateAccessibility(pageObject)
    })

    it('login page has no accessiblity violations', async () => {
      // Verify we are on login page.
      expect(await pageObject.innerText('head')).toContain('Login')
      await validateAccessibility(pageObject)
    })

    it('language selection page has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)

      // Verify we are on language selection page.
      expect(await pageObject.innerText('main')).toContain(
        'Please select your preferred language.',
      )
      await validateAccessibility(pageObject)
    })

    it('program list page has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Verify we are on program list page.
      expect(await pageObject.innerText('h1')).toContain('Get benefits')
      await validateAccessibility(pageObject)
    })

    it('program details page has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')
      await applicantQuestions.clickProgramDetails(programName)

      // Verify we are on program details page. Url should end in "/programs/{program ID}"
      expect(pageObject.url()).toMatch(/\/programs\/[0-9]+$/)
      await validateAccessibility(pageObject)
    })

    it('program preview page has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')
      await applicantQuestions.clickApplyProgramButton(programName)

      // Verify we are on program preview page.
      expect(await pageObject.innerText('h1')).toContain(
        'Program application preview',
      )
      await validateAccessibility(pageObject)
    })

    it('program review page has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')
      await applicantQuestions.applyProgram(programName)

      // Answer all program questions
      await applicantQuestions.answerDateQuestion('2021-11-01')
      await applicantQuestions.answerEmailQuestion('test1@gmail.com')
      await applicantQuestions.clickNext()
      await applicantQuestions.clickNext()
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.answerRadioButtonQuestion('one')
      await applicantQuestions.clickNext()

      // Verify we are on program review page.
      expect(await pageObject.innerText('h1')).toContain(
        'Program application review',
      )
      await validateScreenshot(pageObject)
      await validateAccessibility(pageObject)
    })

    it('program submission page has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')
      await applicantQuestions.applyProgram(programName)

      // Fill out application and submit.
      await applicantQuestions.answerDateQuestion('2021-11-01')
      await applicantQuestions.answerEmailQuestion('test1@gmail.com')
      await applicantQuestions.clickNext()
      await applicantQuestions.clickNext()
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.answerRadioButtonQuestion('one')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage(programName)

      // Verify we are on program submission page.
      expect(await pageObject.innerText('h1')).toContain(
        'Application confirmation',
      )
      await validateScreenshot(pageObject)
      await validateAccessibility(pageObject)
    })
  })

  // TODO: Add tests for "next" navigation
})
