import {expect, test} from '../../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
} from '../../support'

test.describe('Applicant navigation flow', () => {
  test.describe('navigation with five blocks', () => {
    const programName = 'Test program for navigation flows'
    const dateQuestionText = 'date question text'
    const emailQuestionText = 'email question text'
    const staticQuestionText = 'static question text'
    const addressQuestionText = 'address question text'
    const radioQuestionText = 'radio question text'
    const phoneQuestionText = 'phone question text'
    const currencyQuestionText = 'currency question text'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(
        page,
        'suggest_programs_on_application_confirmation_page',
      )

      await adminQuestions.addDateQuestion({
        questionName: 'nav-date-q',
        questionText: dateQuestionText,
      })
      await adminQuestions.addEmailQuestion({
        questionName: 'nav-email-q',
        questionText: emailQuestionText,
      })
      await adminQuestions.addAddressQuestion({
        questionName: 'nav-address-q',
        questionText: addressQuestionText,
      })
      await adminQuestions.addRadioButtonQuestion({
        questionName: 'nav-radio-q',
        questionText: radioQuestionText,
        options: [
          {adminName: 'one_admin', text: 'one'},
          {adminName: 'two_admin', text: 'two'},
          {adminName: 'three_admin', text: 'three'},
        ],
      })
      await adminQuestions.addStaticQuestion({
        questionName: 'nav-static-q',
        questionText: staticQuestionText,
      })
      await adminQuestions.addPhoneQuestion({
        questionName: 'nav-phone-q',
        questionText: phoneQuestionText,
      })
      await adminQuestions.addCurrencyQuestion({
        questionName: 'nav-currency-q',
        questionText: currencyQuestionText,
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, 'first description', [
        'nav-date-q',
        'nav-email-q',
      ])
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: 'second description',
        questions: [{name: 'nav-static-q', isOptional: false}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: 'third description',
        questions: [{name: 'nav-address-q', isOptional: false}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: 'fourth description',
        questions: [{name: 'nav-radio-q', isOptional: true}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: 'fifth description',
        questions: [
          {name: 'nav-phone-q', isOptional: false},
          {name: 'nav-currency-q', isOptional: true},
        ],
      })

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })

    test.describe(
      'review page with North Star enabled',
      {tag: ['@northstar']},
      () => {
        test('validate screenshot', async ({page, applicantQuestions}) => {
          await enableFeatureFlag(page, 'north_star_applicant_ui')
          await applicantQuestions.clickApplyProgramButton(programName)

          await validateScreenshot(
            page,
            'north-star-program-preview',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })
      },
    )

    test('verify program details page', async ({page}) => {
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

    test('verify program list page', async ({page, adminPrograms}) => {
      await loginAsAdmin(page)
      // create second program that has an external link and markdown in the program description.
      const programWithExternalLink = 'Program with external link'
      const programDescriptionWithMarkdown =
        '# Program description\n' +
        'Some things to know:\n' +
        '* Thing 1\n' +
        '* Thing 2\n' +
        '\n' +
        'For more info go to our [website](https://www.example.com)\n'
      await adminPrograms.addProgram(
        programWithExternalLink,
        programDescriptionWithMarkdown,
        'https://external.com',
      )
      await adminPrograms.publishProgram(programWithExternalLink)
      await logout(page)
      // Verify we are on program list page.
      expect(await page.innerText('h1')).toContain(
        'Save time applying for programs and services',
      )

      const cardHtml = await page.innerHTML(
        '.cf-application-card:has-text("' + programWithExternalLink + '")',
      )
      expect(cardHtml).toContain('https://external.com')

      // Verify markdown was parsed correctly
      // h1 set in markdown should be changed to h2
      expect(cardHtml).toContain('<h2>Program description</h2>')
      // lists are formatted correctly
      expect(cardHtml).toContain(
        '<ul class="list-disc mx-8"><li>Thing 1</li><li>Thing 2</li></ul>',
      )
      // text links are formatted correctly with an icon
      expect(cardHtml).toContain(
        '<a href="https://www.example.com" class="text-blue-900 font-bold opacity-75 underline hover:opacity-100" target="_blank" aria-label="opens in a new tab" rel="nofollow noopener noreferrer">website<svg',
      )

      // there shouldn't be any external Links
      const cardText = await page.innerText(
        '.cf-application-card:has-text("' + programWithExternalLink + '")',
      )
      expect(cardText).not.toContain('External site')
      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'program-list-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('verify program submission page for guest', async ({
      page,
      applicantQuestions,
    }) => {
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
      await applicantQuestions.answerPhoneQuestion('4256373270')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Verify we are on program submission page.
      await applicantQuestions.expectConfirmationPage()
      expect(
        await page.locator('.cf-application-id + div').textContent(),
      ).toContain('This is the custom confirmation message with markdown')
      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'program-submission-guest',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )

      // Click the "Apply to another program" button while a guest, which triggers
      // a modal to prompt the guest to login or create an account. Note that
      // in this screenshot, the mouse ends up hovering on top of the first
      // button in the new modal that appears, which is why it is highlighted.
      await applicantQuestions.clickApplyToAnotherProgramButton()
      await validateScreenshot(
        page,
        'program-submission-guest-login-prompt-modal',
        /* fullPage= */ false,
        /* mobileScreenshot= */ true,
      )
    })

    test('verify program submission page for logged in user', async ({
      page,
      applicantQuestions,
    }) => {
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
      await applicantQuestions.answerPhoneQuestion('4256373270')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Verify we are on program submission page.
      await applicantQuestions.expectConfirmationPage()
      expect(
        await page.locator('.cf-application-id + div').textContent(),
      ).toContain('This is the custom confirmation message with markdown')
      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'program-submission-logged-in',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('verify program submission page for guest multiple programs', async ({
      page,
      applicantQuestions,
      adminPrograms,
    }) => {
      // Login as an admin and add a bunch of programs
      await loginAsAdmin(page)
      await adminPrograms.addProgram('program 1')
      await adminPrograms.addProgram('program 2')
      await adminPrograms.addProgram('program 3')
      await adminPrograms.addProgram('program 4')
      await adminPrograms.publishAllDrafts()
      await logout(page)

      // Fill out application as a guest and submit.
      await applicantQuestions.applyProgram(programName)
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
      await applicantQuestions.answerPhoneQuestion('4256373270')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Verify we are on program submission page.
      await applicantQuestions.expectConfirmationPage()
      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'program-submission-guest-multiple-programs',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows error with incomplete submission', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.clickApplyProgramButton(programName)

      // The UI correctly won't let us submit because the application isn't complete.
      // To fake submitting an incomplete application add a submit button and click it.
      // Note the form already triggers for the submit action.
      // A clearer way to set this up would be to have two browser contexts but that isn't doable in our setup.
      await page.evaluate(() => {
        const buttonEl = document.createElement('button')
        buttonEl.id = 'test-form-submit'
        buttonEl.type = 'submit'
        const formEl = document.querySelector('.cf-debounced-form')!
        formEl.appendChild(buttonEl)
      })
      const submitButton = page.locator('#test-form-submit')
      await submitButton.click()

      await validateToastMessage(
        page,
        "Error: There's been an update to the application",
      )
      await validateScreenshot(
        page,
        'program-out-of-date',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows "no changes" page when a duplicate application is submitted', async ({
      page,
      applicantQuestions,
    }) => {
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
      await applicantQuestions.answerPhoneQuestion('4256373270')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Submit the application again without editing it
      await applicantQuestions.returnToProgramsFromSubmissionPage()
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage()

      // See the duplicate submissions page
      await applicantQuestions.expectDuplicatesPage()
      await validateScreenshot(
        page,
        'duplicate-submission-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)

      // Click the "Continue editing" button to return to the review page
      await page.click('#continue-editing-button')
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.clickEdit()

      // Edit the application but insert the same values as before and submit.
      await applicantQuestions.answerDateQuestion('2021-11-01')
      await applicantQuestions.answerEmailQuestion('test1@gmail.com')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // See the duplicate submissions page
      await applicantQuestions.expectDuplicatesPage()

      // Click the "Exit application" link to return to the programs page
      await page.click('text="Exit application"')
      await applicantQuestions.expectProgramsPage()
    })
  })
})
