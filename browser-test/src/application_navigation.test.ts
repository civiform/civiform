import {
  AdminQuestions,
  ClientInformation,
  createTestContext,
  enableFeatureFlag,
  disableFeatureFlag,
  dropTables,
  loginAsAdmin,
  loginAsTestUser,
  loginAsTrustedIntermediary,
  logout,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
  waitForPageJsLoad,
  isLocalDevEnvironment,
} from './support'
import {ProgramVisibility} from './support/admin_programs'

describe('Applicant navigation flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('navigation with five blocks', () => {
    const programName = 'Test program for navigation flows'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await enableFeatureFlag(page, 'phone_question_type_enabled')
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
      await adminQuestions.addPhoneQuestion({
        questionName: 'nav-phone-q',
      })

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
      await adminPrograms.addProgramBlock(programName, 'fifth description', [
        'nav-phone-q',
      ])

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)
    })

    it('clicking previous on first block goes to summary page', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickPrevious()

      // Assert that we're on the preview page.
      await applicantQuestions.expectReviewPage()
    })

    it('clicking previous on later blocks goes to previous blocks', async () => {
      const {applicantQuestions} = ctx
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
      await applicantQuestions.expectReviewPage()
    })

    it('verify program details page', async () => {
      const {page} = ctx
      // Begin waiting for the popup before clicking the link, otherwise
      // the popup may fire before the wait is registered, causing the test to flake.
      const popupPromise = page.waitForEvent('popup')
      await page.click(
        `.cf-application-card:has-text("${programName}") >> text='Program details'`,
      )
      const popup = await popupPromise
      const popupURL = await popup.evaluate('location.href')

      // Verify that we are taken to the program details page
      expect(popupURL).toMatch('https://www.usa.gov')
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
      // Verify we are on program list page.
      expect(await page.innerText('h1')).toContain(
        'Save time when applying for benefits',
      )
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
      await applicantQuestions.clickApplyProgramButton(programName)

      // Verify we are on program preview page.
      await applicantQuestions.expectReviewPage()
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-preview')
    })

    it('can answer third question directly', async () => {
      const {page, applicantQuestions} = ctx
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

      await applicantQuestions.answerPhoneQuestion(
        'United States',
        '4256373270',
      )
      await applicantQuestions.clickNext()
      // Verify we are on program review page.
      await applicantQuestions.expectReviewPage()
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-review')
    })

    it('verify program submission page for guest', async () => {
      const {page, applicantQuestions} = ctx
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
      await applicantQuestions.answerPhoneQuestion(
        'United States',
        '4256373270',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Verify we are on program submission page.
      expect(await page.innerText('h1')).toContain('Application confirmation')
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-submission-guest')

      // Click the "Apply to another program" button while a guest, which triggers
      // a modal to prompt the guest to login or create an account. Note that
      // in this screenshot, the mouse ends up hovering on top of the first
      // button in the new modal that appears, which is why it is highlighted.
      await applicantQuestions.clickApplyToAnotherProgramButton()
      await validateScreenshot(
        page,
        'program-submission-guest-login-prompt-modal',
      )
    })

    it('verify program submission page for logged in user', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsTestUser(page)
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
      await applicantQuestions.answerPhoneQuestion(
        'United States',
        '4256373270',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Verify we are on program submission page.
      expect(await page.innerText('h1')).toContain('Application confirmation')
      await validateAccessibility(page)
      await validateScreenshot(page, 'program-submission-logged-in')
    })

    it('shows error with incomplete submission', async () => {
      const {page, applicantQuestions} = ctx
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

      await validateToastMessage(
        page,
        "Error: There's been an update to the application",
      )
      await validateScreenshot(page, 'program-out-of-date')
    })
  })

  describe('navigation with common intake', () => {
    // Create two programs, one is common intake
    const commonIntakeProgramName = 'Test Common Intake Form Program'
    const secondProgramName = 'Test Regular Program with Eligibility Conditions'
    const eligibilityQuestionId = 'nav-predicate-number-q'
    const secondProgramCorrectAnswer = '5'

    beforeAll(async () => {
      const {page} = ctx
      await dropTables(page)
    })

    // TODO(#4509): Once we can create different test users, change this to
    // beforeAll and use different users for each test, instead of wiping the
    // db after each test.
    beforeEach(async () => {
      const {page, adminQuestions, adminPredicates, adminPrograms} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'intake_form_enabled')
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      // Add questions
      await adminQuestions.addNumberQuestion({
        questionName: eligibilityQuestionId,
      })

      // Set up common intake form
      await adminPrograms.addProgram(
        commonIntakeProgramName,
        'program description',
        'https://usa.gov',
        ProgramVisibility.PUBLIC,
        'admin description',
        /* isCommonIntake= */ true,
      )

      await adminPrograms.editProgramBlock(
        commonIntakeProgramName,
        'first description',
        [eligibilityQuestionId],
      )

      // Set up another program
      await adminPrograms.addProgram(secondProgramName)

      await adminPrograms.editProgramBlock(
        secondProgramName,
        'first description',
        [eligibilityQuestionId],
      )

      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        secondProgramName,
        'Screen 1',
      )
      await adminPredicates.addPredicate(
        'nav-predicate-number-q',
        /* action= */ null,
        'number',
        'is equal to',
        secondProgramCorrectAnswer,
      )

      await adminPrograms.publishAllPrograms()
      // TODO(#4509): Once this is a beforeAll(), it'll automatically go back
      // to the home page when it's done and we can remove this line.
      await logout(page)
    })

    afterEach(async () => {
      // TODO(#4509): Once we can create different test users, we don't need to
      // wipe the db after each test
      const {page} = ctx
      await dropTables(page)
    })

    it('does not show eligible programs or upsell on confirmation page when no programs are eligible and signed in', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      await loginAsTestUser(page)
      // Fill out common intake form, with non-eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [],
      )

      await validateScreenshot(
        page,
        'cif-ineligible-signed-in-confirmation-page',
      )
      await validateAccessibility(page)
    })

    it('shows eligible programs and no upsell on confirmation page when programs are eligible and signed in', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      await loginAsTestUser(page)
      // Fill out common intake form, with eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )

      await validateScreenshot(page, 'cif-eligible-signed-in-confirmation-page')
      await validateAccessibility(page)
    })

    it('does not show eligible programs and shows upsell on confirmation page when no programs are eligible and a guest user', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      // Fill out common intake form, with non-eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [],
      )

      await validateScreenshot(page, 'cif-ineligible-guest-confirmation-page')
      await validateAccessibility(page)
    })

    it('shows eligible programs and upsell on confirmation page when programs are eligible and a guest user', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      // Fill out common intake form, with eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )

      await validateScreenshot(page, 'cif-eligible-guest-confirmation-page')
      await validateAccessibility(page)

      await page.click('button:has-text("Apply to programs")')
      await validateScreenshot(page, 'cif-submission-guest-login-prompt-modal')
    })

    it('does not show eligible programs and shows TI text on confirmation page when no programs are eligible and a TI', async () => {
      const {page, tiDashboard, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      // Create trusted intermediary client
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
      await tiDashboard.clickOnApplicantDashboard()

      // Fill out common intake form, with non-eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ true,
        /* wantEligiblePrograms= */ [],
      )

      await validateScreenshot(page, 'cif-ineligible-ti-confirmation-page')
    })

    it('shows eligible programs and TI text on confirmation page when programs are eligible and a TI', async () => {
      const {page, tiDashboard, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      // Create trusted intermediary client
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
      await tiDashboard.clickOnApplicantDashboard()

      // Fill out common intake form, with eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ true,
        /* wantEligiblePrograms= */ [secondProgramName],
      )
      await validateScreenshot(page, 'cif-eligible-ti-confirmation-page')
    })
  })

  describe('navigation with eligibility conditions', () => {
    // Create a program with 2 questions and an eligibility condition.
    const fullProgramName = 'Test program for eligibility navigation flows'
    const eligibilityQuestionId = 'nav-predicate-number-q'

    beforeAll(async () => {
      const {page, adminQuestions, adminPredicates, adminPrograms} = ctx
      await loginAsAdmin(page)

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
      await adminPredicates.addPredicate(
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
      const {applicantQuestions} = ctx
      await applicantQuestions.clickApplyProgramButton(fullProgramName)

      await applicantQuestions.expectQuestionHasNoEligibilityIndicator(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
    })

    it('shows not eligible with ineligible answer', async () => {
      const {page, applicantQuestions} = ctx
      await disableFeatureFlag(page, 'nongated_eligibility_enabled')
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ false,
      )
      await applicantQuestions.clickApplyProgramButton(fullProgramName)

      await validateToastMessage(page, 'may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(page, 'application-ineligible-same-application')
      await validateAccessibility(page)
    })

    it('shows may be eligible with an eligible answer', async () => {
      const {page, applicantQuestions} = ctx
      await disableFeatureFlag(page, 'nongated_eligibility_enabled')
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and without submitting.
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
      await validateScreenshot(page, 'eligible-toast')

      // Verify the question is marked eligible
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ true,
      )
      await validateScreenshot(page, 'eligible-home-page-program-tag')
      await validateAccessibility(page)

      // Go back to in progress application and submit.
      await applicantQuestions.applyProgram(fullProgramName)
      await applicantQuestions.answerEmailQuestion('test@test.com')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ true,
      )
    })

    it('shows not eligible with ineligible answer from another application', async () => {
      const {page, adminPrograms, applicantQuestions} = ctx
      const overlappingOneQProgramName =
        'Test program with one overlapping question for eligibility navigation flows'

      // Add the partial program.
      await loginAsAdmin(page)
      await disableFeatureFlag(page, 'nongated_eligibility_enabled')
      await adminPrograms.addProgram(overlappingOneQProgramName)
      await adminPrograms.editProgramBlock(
        overlappingOneQProgramName,
        'first description',
        [eligibilityQuestionId],
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(overlappingOneQProgramName)
      await logout(page)

      await applicantQuestions.applyProgram(overlappingOneQProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await validateScreenshot(page, 'ineligible-home-page-program-tag')
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ false,
      )
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await validateToastMessage(page, 'may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(page, 'application-ineligible-preexisting-data')
      await validateAccessibility(page)
    })

    it('shows not eligible upon submit with ineligible answer', async () => {
      const {page, applicantQuestions} = ctx
      await disableFeatureFlag(page, 'nongated_eligibility_enabled')
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ false,
      )
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

    it('shows not eligible upon submit with ineligible answer with gating eligibility', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ false,
      )
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

    it('shows may be eligible with nongating eligibility', async () => {
      const {page, adminPrograms, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      await loginAsAdmin(page)
      await adminPrograms.createNewVersion(fullProgramName)
      await adminPrograms.setProgramEligibilityToNongating(fullProgramName)
      await adminPrograms.publishProgram(fullProgramName)
      await logout(page)

      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application without submitting.
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()

      // Verify the question is marked eligible
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ true,
      )
    })

    it('does not show not eligible with nongating eligibility', async () => {
      const {page, adminPrograms, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'nongated_eligibility_enabled')

      await loginAsAdmin(page)
      await adminPrograms.createNewVersion(fullProgramName)
      await adminPrograms.setProgramEligibilityToNongating(fullProgramName)
      await adminPrograms.publishProgram(fullProgramName)
      await logout(page)

      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application without submitting.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()

      // Verify that there's no indication of eligibility.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeNoEligibilityTags(fullProgramName)

      // Go back to in progress application and submit.
      await applicantQuestions.applyProgram(fullProgramName)
      await applicantQuestions.answerEmailQuestion('test@test.com')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, '')
      await applicantQuestions.submitFromReviewPage()
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeNoEligibilityTags(fullProgramName)
    })
  })

  describe('navigation with address correction enabled', () => {
    const multiBlockMultiAddressProgram =
      'Address correction multi-block, multi-address program'
    const singleBlockMultiAddressProgram =
      'Address correction single-block, multi-address program'
    const singleBlockSingleAddressProgram =
      'Address correction single-block, single-address program'

    const addressWithCorrectionQuestionId = 'address-with-correction-q'
    const addressWithoutCorrectionQuestionId = 'address-without-correction-q'
    const textQuestionId = 'text-q'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      // Create all questions
      await adminQuestions.addAddressQuestion({
        questionName: addressWithCorrectionQuestionId,
        questionText: 'With Correction',
      })

      await adminQuestions.addAddressQuestion({
        questionName: addressWithoutCorrectionQuestionId,
        questionText: 'Without Correction',
      })

      await adminQuestions.addTextQuestion({
        questionName: textQuestionId,
        questionText: 'text',
      })

      // Create multi-block, multi-address program
      await adminPrograms.addProgram(multiBlockMultiAddressProgram)

      await adminPrograms.editProgramBlockWithOptional(
        multiBlockMultiAddressProgram,
        'first block',
        [addressWithCorrectionQuestionId],
        addressWithoutCorrectionQuestionId,
      )

      await adminPrograms.addProgramBlock(
        multiBlockMultiAddressProgram,
        'second block',
        [textQuestionId],
      )

      await adminPrograms.goToBlockInProgram(
        multiBlockMultiAddressProgram,
        'Screen 1',
      )
      await adminPrograms.clickAddressCorrectionToggleByName(
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(multiBlockMultiAddressProgram)

      // Create single-block, multi-address program
      await adminPrograms.addProgram(singleBlockMultiAddressProgram)

      await adminPrograms.editProgramBlockWithOptional(
        singleBlockMultiAddressProgram,
        'first block',
        [addressWithCorrectionQuestionId],
        addressWithoutCorrectionQuestionId,
      )

      await adminPrograms.goToBlockInProgram(
        singleBlockMultiAddressProgram,
        'Screen 1',
      )
      await adminPrograms.clickAddressCorrectionToggleByName(
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(singleBlockMultiAddressProgram)

      // Create single-block, single-address program
      await adminPrograms.addProgram(singleBlockSingleAddressProgram)

      await adminPrograms.editProgramBlock(
        singleBlockSingleAddressProgram,
        'first block',
        [addressWithCorrectionQuestionId],
      )

      await adminPrograms.goToBlockInProgram(
        singleBlockSingleAddressProgram,
        'Screen 1',
      )
      await adminPrograms.clickAddressCorrectionToggleByName(
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(singleBlockSingleAddressProgram)

      // Log out admin
      await logout(page)
    })
    if (isLocalDevEnvironment()) {
      it('can correct address multi-block, multi-address program', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await applicantQuestions.applyProgram(multiBlockMultiAddressProgram)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          '500 Harrison',
          '',
          'Seattle',
          'WA',
          '98109',
          0,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Seattle',
          'WA',
          '98109',
          1,
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)
        await applicantQuestions.clickNext()
        await applicantQuestions.answerTextQuestion('Some text')
        await applicantQuestions.clickNext()
        await applicantQuestions.expectAddressHasBeenCorrected(
          'With Correction',
          'Address In Area',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      it('can correct address single-block, multi-address program', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await applicantQuestions.applyProgram(singleBlockMultiAddressProgram)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          '500 Harrison',
          '',
          'Seattle',
          'WA',
          '98109',
          0,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Seattle',
          'WA',
          '98109',
          1,
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)
        await applicantQuestions.clickNext()
        await applicantQuestions.expectAddressHasBeenCorrected(
          'With Correction',
          'Address In Area',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      it('can correct address single-block, single-address program', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Only doing accessibility and screenshot checks for address correction page
        // once since they are all the same
        await validateAccessibility(page)
        await validateScreenshot(page, 'verify-address-page')

        await applicantQuestions.clickNext()
        await applicantQuestions.expectAddressHasBeenCorrected(
          'With Correction',
          'Address In Area',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      it('prompts user to edit if no suggestions are returned', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Bogus Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(false)

        await validateAccessibility(page)
        await validateScreenshot(page, 'no-suggestions-returned')

        // Can continue on anyway
        await applicantQuestions.clickNext()
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      it('prompts user to edit if an error is returned from the Esri service', async () => {
        // This is currently the same as when no suggestions are returend.
        // We may change this later.
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Error Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(false)

        await validateAccessibility(page)
        await validateScreenshot(page, 'esri-service-errored')

        // Can continue on anyway
        await applicantQuestions.clickNext()
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      it('skips the address correction screen if the user enters an address that exactly matches one of the returned suggestions', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)
        // Fill out application with address that is contained in findAddressCandidates.json (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
        await applicantQuestions.answerAddressQuestion(
          'Address In Area',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectReviewPage()

        await logout(page)
      })

      it('clicking previous on address correction page takes you back to address entry page', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)

        await applicantQuestions.clickPrevious()
        await applicantQuestions.expectAddressPage()

        await logout(page)
      })
    }

    it('address correction page does not show if feature is disabled', async () => {
      const {page, applicantQuestions} = ctx
      await disableFeatureFlag(page, 'esri_address_correction_enabled')
      await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

      // Fill out application and submit.
      await applicantQuestions.answerAddressQuestion(
        '305 Harrison',
        '',
        'Seattle',
        'WA',
        '98109',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.expectAddressHasBeenCorrected(
        'With Correction',
        '305 Harrison',
      )
      await applicantQuestions.clickSubmit()
      await logout(page)
    })
  })

  // TODO: Add tests for "next" navigation
})
