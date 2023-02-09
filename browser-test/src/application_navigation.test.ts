import {
  AdminQuestions,
  createTestContext,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
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
        'Program application summary',
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
        'Program application summary',
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

    it('verify program details page', async () => {
      const {page} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await page.click(
        `.cf-application-card:has-text("${programName}") >> text='Program details'`,
      )
      const popupPromise = page.waitForEvent('popup')
      const popup = await popupPromise
      const popupURL = await popup.evaluate('location.href')

      // Verify if the program details page Url ends in "/programs/{program ID}"
      expect(popupURL).toMatch(/\/programs\/[0-9]+$/)
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
      const cardHtml = await page.innerHTML(
        '.cf-application-card:has-text("' + programWithExternalLink + '")',
      )
      expect(cardHtml).toContain('https://external.com')
      // there shouldn't be any external Links
      const cardText = await page.innerText(
        '.cf-application-card:has-text("' + programWithExternalLink + '")',
      )
      expect(cardText).not.toContain('External site')
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-list-page')
    })

    it('verify program preview page', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await applicantQuestions.clickApplyProgramButton(programName)

      // Verify we are on program preview page.
      expect(await page.innerText('h2')).toContain(
        'Program application summary',
      )
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-preview')
    })

    it('can answer third question directly', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await applicantQuestions.clickApplyProgramButton(programName)
      await page.click(
        '.cf-applicant-summary-row:has(div:has-text("address question text")) a:has-text("Answer")',
      )
      await waitForPageJsLoad(page)
      expect(await page.innerText('.cf-applicant-question-text')).toContain(
        'address question text',
      )
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.clickReview()
      await validateScreenshot(page, 'third-question-answered')
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
      expect(await page.innerText('h2')).toContain(
        'Program application summary',
      )
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

    it('shows error with incomplete submission', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await applicantQuestions.clickApplyProgramButton(programName)

      // The UI correctly won't let us submit because the application isn't complete.
      // To fake submitting an incomplete application add a submit button and click it.
      // Note the form already triggers for the submit action.
      // A clearer way to set this up would be to have two browser contexts but that isn't doable in our setup.
      await page.evaluate(() => {
        const buttonEl = document.createElement('button')
        buttonEl.id = 'test-form-submit'
        buttonEl.type = 'SUBMIT'
        const formEl = document.querySelector('.cf-debounced-form')!
        formEl.appendChild(buttonEl)
      })
      const submitButton = page.locator('#test-form-submit')!
      await submitButton.click()

      const toastMessages = await page.innerText('#toast-container')
      expect(toastMessages).toContain(
        "There's been an update to the application",
      )
      await validateScreenshot(page, 'program-out-of-date')
    })
  })

  describe('navigation with eligibility conditions', () => {
    // Create a program with 2 questions and an eligibility condition.
    const fullProgramName = 'Test program for eligibility navigation flows'
    const eligibilityQuestionId = 'nav-predicate-number-q'

    beforeAll(async () => {
      const {page, adminQuestions, adminPredicates, adminPrograms} = ctx
      await loginAsAdmin(page)
      await disableFeatureFlag(page, 'predicates_multiple_questions_enabled')
      await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')

      await adminQuestions.addNumberQuestion({
        questionName: eligibilityQuestionId,
      })
      await adminQuestions.addEmailQuestion({
        questionName: 'nav-predicate-email-q',
      })

      // Add the full program.
      await adminPrograms.addProgram(fullProgramName)
      await adminPrograms.editProgramBlock(
        fullProgramName,
        'first description',
        ['nav-predicate-number-q'],
      )
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        fullProgramName,
        'Screen 1',
      )
      await adminPredicates.addLegacyPredicate(
        'nav-predicate-number-q',
        /* action= */ null,
        'number',
        'is equal to',
        '5',
      )

      await adminPrograms.addProgramBlock(
        fullProgramName,
        'second description',
        ['nav-predicate-email-q'],
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(fullProgramName)
    })

    it('does not show Not Eligible when there is no answer', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')
      await applicantQuestions.clickApplyProgramButton(fullProgramName)

      await applicantQuestions.expectQuestionHasNoEligibilityIndicator(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
    })

    it('shows not eligible with ineligible answer', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.clickApplyProgramButton(fullProgramName)

      await applicantQuestions.validateToastMessage('may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(page, 'application-ineligible-same-application')
      await validateAccessibility(page)
    })

    it('shows not eligible with ineligible answer from another application', async () => {
      const {page, adminPrograms, applicantQuestions} = ctx
      const overlappingOneQProgramName =
        'Test program with one overlapping question for eligibility navigation flows'

      // Add the partial program.
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')
      await adminPrograms.addProgram(overlappingOneQProgramName)
      await adminPrograms.editProgramBlock(
        overlappingOneQProgramName,
        'first description',
        [eligibilityQuestionId],
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(overlappingOneQProgramName)
      await logout(page)

      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')

      await applicantQuestions.applyProgram(overlappingOneQProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await applicantQuestions.validateToastMessage('may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(page, 'application-ineligible-preexisting-data')
      await validateAccessibility(page)
    })

    it('shows not eligible upon submit with ineligible answer', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')
      await enableFeatureFlag(page, 'program_eligibility_conditions_enabled')
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )

      // Answer the other question.
      await applicantQuestions.clickContinue()
      await applicantQuestions.answerEmailQuestion('email@email.com')

      // Submit and expect to be told it's ineligible.
      await applicantQuestions.clickNext()
      await applicantQuestions.clickSubmit()
      await applicantQuestions.expectIneligiblePage()
    })
  })
  // TODO: Add tests for "next" navigation
})
