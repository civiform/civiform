import {
  createTestContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Applicant navigation flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('navigation with four blocks', () => {
    const programName = 'Test program for navigation flows'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

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
    })

    it('clicking previous on first block goes to summary page', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickPrevious()

      // Assert that we're on the preview page.
      expect(await page.innerText('h2')).toContain(
        'Program application preview',
      )
    })

    it('clicking previous on later blocks goes to previous blocks', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

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
        'WA',
        '54321',
      )
      await applicantQuestions.clickNext()

      // Click previous and see file upload page with address
      await applicantQuestions.clickPrevious()
      await applicantQuestions.checkAddressQuestionValue(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
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
      expect(await page.innerText('h2')).toContain(
        'Program application preview',
      )
    })

    it('verify login page', async () => {
      const {page} = ctx
      // Verify we are on login page.
      expect(await page.innerText('head')).toContain('Login')
      await validateAccessibility(page)
      await validateScreenshot(page, 'landing-page')
    })

    it('verify language selection page', async () => {
      const {page} = ctx
      await loginAsGuest(page)

      // Verify we are on language selection page.
      expect(await page.innerText('main')).toContain(
        'Please select your preferred language.',
      )
      await validateAccessibility(page)
      await validateScreenshot(page, 'language-selection')
    })

    it('verify program list page', async () => {
      const {page, adminPrograms} = ctx
      await loginAsAdmin(page)
      // create second program that has an external link.
      const programWithExternalLink = 'Program with external link'
      await adminPrograms.addProgram(
        programWithExternalLink,
        'Program description',
        'https://external.com',
      )
      await adminPrograms.publishProgram(programWithExternalLink)
      await logout(page)
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      // Verify we are on program list page.
      expect(await page.innerText('h1')).toContain('Get benefits')
      expect(
        await page.locator('a:has-text("External site")').getAttribute('href'),
      ).toEqual('https://external.com')
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-list-page')
    })

    it('verify program details page', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await applicantQuestions.clickProgramDetails(programName)

      // Verify we are on program details page. Url should end in "/programs/{program ID}"
      expect(page.url()).toMatch(/\/programs\/[0-9]+$/)
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-details-page')
    })

    it('verify program preview page', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await applicantQuestions.clickApplyProgramButton(programName)

      // Verify we are on program preview page.
      expect(await page.innerText('h2')).toContain(
        'Program application preview',
      )
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-preview')
    })

    it('verify program review page', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
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
        'WA',
        '54321',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.answerRadioButtonQuestion('one')
      await applicantQuestions.clickNext()

      // Verify we are on program review page.
      expect(await page.innerText('h2')).toContain('Program application review')
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-review')
    })

    it('verify program submission page', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
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
        'WA',
        '54321',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.answerRadioButtonQuestion('one')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Verify we are on program submission page.
      expect(await page.innerText('h1')).toContain('Application confirmation')
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-submission')
    })
  })

  // TODO: Add tests for "next" navigation
})
