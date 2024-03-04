import {test, expect} from '@playwright/test'
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

test.describe('Applicant navigation flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  test.describe('navigation with five blocks', () => {
    const programName = 'Test program for navigation flows'
    const dateQuestionText = 'date question text'
    const emailQuestionText = 'email question text'
    const addressQuestionText = 'address question text'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
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
        options: [
          {adminName: 'one_admin', text: 'one'},
          {adminName: 'two_admin', text: 'two'},
          {adminName: 'three_admin', text: 'three'},
        ],
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

    test.describe('previous button', () => {
      test('clicking previous on first block goes to summary page', async () => {
        const {applicantQuestions} = ctx
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickPrevious()

        // Assert that we're on the preview page.
        await applicantQuestions.expectReviewPage()
      })

      test('clicking previous on later blocks goes to previous blocks', async () => {
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

        // Click previous and see previous page with address
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

        // Assert that we're on the review page.
        await applicantQuestions.clickPrevious()
        await applicantQuestions.expectReviewPage()
      })

      test('clicking previous does not save when flag off', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await disableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('2021-11-01')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickPrevious()

        await applicantQuestions.expectReviewPage()
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          dateQuestionText,
        )
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          emailQuestionText,
        )
      })

      test('clicking previous with correct form shows previous page and saves answers', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('2021-11-01')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickNext()
        // Nothing to fill in since this is the static question block
        await applicantQuestions.clickNext()

        // Fill out address question
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
        )

        // Click previous then go to the review page and verify the address question
        // answer was saved
        await applicantQuestions.clickPrevious()

        await applicantQuestions.clickReview()
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          '1234 St',
        )
      })

      test('clicking previous with missing answers shows modal', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickPrevious()

        // The date question is required, so we should see the error modal.
        await applicantQuestions.expectErrorOnPreviousModal()
        await validateScreenshot(page, 'error-on-previous-modal')
      })

      test('error on previous modal > click stay and fix > shows block', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickPrevious()
        await applicantQuestions.expectErrorOnPreviousModal()

        await applicantQuestions.clickStayAndFixAnswers()

        // Verify the previously filled in answers are present
        await applicantQuestions.checkDateQuestionValue('')
        await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')

        // Answer the date question correctly and try clicking "Review" again
        await applicantQuestions.answerDateQuestion('2021-11-01')

        await applicantQuestions.clickPrevious()

        // Verify we're taken to the previous page (which is the review page
        // since this was the first block) page and the answers were saved
        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          dateQuestionText,
          '11/01/2021',
        )
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          emailQuestionText,
          'test1@gmail.com',
        )
      })

      test('error on previous modal > click previous without saving > answers not saved', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('2021-11-01')
        await applicantQuestions.answerEmailQuestion('')

        await applicantQuestions.clickPrevious()
        await applicantQuestions.expectErrorOnPreviousModal()

        // Proceed to the previous page (which will be the review page,
        // since this is the first block), acknowledging that answers won't be saved
        await applicantQuestions.clickPreviousWithoutSaving()

        await applicantQuestions.expectReviewPage()
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          dateQuestionText,
        )
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          emailQuestionText,
        )
      })

      test('error on previous modal > click previous without saving > shows previous block', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('2021-11-01')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')
        await applicantQuestions.clickNext()
        // Nothing to fill in since this is the static question block
        await applicantQuestions.clickNext()

        // Don't fill in the address question, and try going to previous block
        await applicantQuestions.clickPrevious()
        await applicantQuestions.expectErrorOnPreviousModal()

        // Proceed to the previous page and verify the first block answers are present
        await applicantQuestions.clickPreviousWithoutSaving()
        // This is the static question block, so continue to the previous block
        await applicantQuestions.clickPrevious()

        await applicantQuestions.checkDateQuestionValue('2021-11-01')
        await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')
      })

      test.afterAll(async () => {
        const {page} = ctx
        await loginAsAdmin(page)
        await disableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)
      })
    })

    test.describe('review button', () => {
      test('clicking review does not save when flag off', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await disableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('2021-11-01')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickReview()

        await applicantQuestions.expectReviewPage()
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          dateQuestionText,
        )
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          emailQuestionText,
        )
      })

      test('clicking review with correct form shows review page with saved answers', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('2021-11-01')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickReview()

        await applicantQuestions.expectReviewPage()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          dateQuestionText,
          '11/01/2021',
        )
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          emailQuestionText,
          'test1@gmail.com',
        )
      })

      test('clicking review with missing answers shows modal', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickReview()

        // The date question is required, so we should see the error modal.
        await applicantQuestions.expectErrorOnReviewModal()
        await validateScreenshot(page, 'error-on-review-modal')
      })

      test('error on review modal > click stay and fix > shows block', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickReview()
        await applicantQuestions.expectErrorOnReviewModal()

        await applicantQuestions.clickStayAndFixAnswers()

        // Verify the previously filled in answers are present
        await applicantQuestions.checkDateQuestionValue('')
        await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')

        // Answer the date question correctly and try clicking "Review" again
        await applicantQuestions.answerDateQuestion('2021-11-01')

        await applicantQuestions.clickReview()

        // Verify we're taken to the review page and the answers were saved
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          dateQuestionText,
          '11/01/2021',
        )
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          emailQuestionText,
          'test1@gmail.com',
        )
      })

      test('error on review modal > click review without saving > shows review page without saved answers', async () => {
        const {page, applicantQuestions} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('2021-11-01')
        await applicantQuestions.answerEmailQuestion('')

        await applicantQuestions.clickReview()
        await applicantQuestions.expectErrorOnReviewModal()

        // Proceed to the Review page, acknowledging that answers won't be saved
        await applicantQuestions.clickReviewWithoutSaving()

        await applicantQuestions.expectReviewPage()
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          dateQuestionText,
        )
        await applicantQuestions.validateNoPreviouslyAnsweredText(
          emailQuestionText,
        )
      })

      test.afterAll(async () => {
        const {page} = ctx
        await loginAsAdmin(page)
        await disableFeatureFlag(page, 'save_on_all_actions')
        await logout(page)
      })
    })

    test('verify program details page', async () => {
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

    test('verify program list page', async () => {
      const {page, adminPrograms} = ctx
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

    test('verify program preview page', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.clickApplyProgramButton(programName)

      // Verify we are on program preview page.
      await applicantQuestions.expectReviewPage()
      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'program-preview',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('can answer third question directly', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.answerQuestionFromReviewPage(
        'address question text',
      )
      await waitForPageJsLoad(page)
      await applicantQuestions.validateQuestionIsOnPage('address question text')
      // Should focus on the question the applicant clicked on when answering for the first time
      expect(await page.innerHTML('.cf-address-street-1')).toContain(
        'autofocus',
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
      await validateScreenshot(
        page,
        'third-question-answered',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )

      await page.click(
        '.cf-applicant-summary-row:has(div:has-text("address question text")) a:has-text("Edit")',
      )
      await waitForPageJsLoad(page)
      await applicantQuestions.validateQuestionIsOnPage('address question text')
      // Should focus on the question the applicant clicked on when editing previous answer
      expect(await page.innerHTML('.cf-address-street-1')).toContain(
        'autofocus',
      )
    })

    test('verify program review page', async () => {
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

      await applicantQuestions.answerPhoneQuestion('4256373270')
      await applicantQuestions.clickNext()
      // Verify we are on program review page.
      await applicantQuestions.expectReviewPage()
      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'program-review',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('verify program submission page for guest', async () => {
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
      await applicantQuestions.answerPhoneQuestion('4256373270')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Verify we are on program submission page.
      expect(await page.innerText('h1')).toContain('Application confirmation')
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

    test('verify program submission page for logged in user', async () => {
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
      await applicantQuestions.answerPhoneQuestion('4256373270')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // Verify we are on program submission page.
      expect(await page.innerText('h1')).toContain('Application confirmation')
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

    test('verify program submission page for guest multiple programs', async () => {
      const {page, applicantQuestions, adminPrograms} = ctx

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
      expect(await page.innerText('h1')).toContain('Application confirmation')
      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'program-submission-guest-multiple-programs',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows error with incomplete submission', async () => {
      const {page, applicantQuestions} = ctx
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

    test('shows "no changes" page when a duplicate application is submitted', async () => {
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

  test.describe('navigation with common intake', () => {
    // Create two programs, one is common intake
    const commonIntakeProgramName = 'Test Common Intake Form Program'
    const secondProgramName = 'Test Regular Program with Eligibility Conditions'
    const eligibilityQuestionId = 'nav-predicate-number-q'
    const secondProgramCorrectAnswer = '5'

    test.beforeAll(async () => {
      const {page} = ctx
      await dropTables(page)
    })

    // TODO(#4509): Once we can create different test users, change this to
    // test.beforeAll and use different users for each test, instead of wiping the
    // db after each test.
    test.beforeEach(async () => {
      const {page, adminQuestions, adminPredicates, adminPrograms} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'intake_form_enabled')

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

      await adminPrograms.publishAllDrafts()
      // TODO(#4509): Once this is a test.beforeAll(), it'll automatically go back
      // to the home page when it's done and we can remove this line.
      await logout(page)
    })

    test.afterEach(async () => {
      // TODO(#4509): Once we can create different test users, we don't need to
      // wipe the db after each test
      const {page} = ctx
      await dropTables(page)
    })

    test('does not show eligible programs or upsell on confirmation page when no programs are eligible and signed in', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')

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
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('shows eligible programs and no upsell on confirmation page when programs are eligible and signed in', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')

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

      await validateScreenshot(
        page,
        'cif-eligible-signed-in-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('does not show eligible programs and shows upsell on confirmation page when no programs are eligible and a guest user', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')

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

      await validateScreenshot(
        page,
        'cif-ineligible-guest-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('shows eligible programs and upsell on confirmation page when programs are eligible and a guest user', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')

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

      await validateScreenshot(
        page,
        'cif-eligible-guest-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)

      await page.click('button:has-text("Apply to programs")')
      await validateScreenshot(
        page,
        'cif-submission-guest-login-prompt-modal',
        /* fullPage= */ false,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows intake form as submitted after completion', async () => {
      const {page, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')

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

      await page.click('button:has-text("Apply to programs")')
      await page.click('button:has-text("Continue without an account")')
      await validateScreenshot(
        page,
        'cif-shows-submitted',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('does not show eligible programs and shows TI text on confirmation page when no programs are eligible and a TI', async () => {
      const {page, tiDashboard, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')

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
      await tiDashboard.clickOnViewApplications()

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

      await validateScreenshot(
        page,
        'cif-ineligible-ti-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows eligible programs and TI text on confirmation page when programs are eligible and a TI', async () => {
      const {page, tiDashboard, applicantQuestions} = ctx
      await enableFeatureFlag(page, 'intake_form_enabled')

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
      await tiDashboard.clickOnViewApplications()

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
      await validateScreenshot(
        page,
        'cif-eligible-ti-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })
  })

  test.describe('navigation with eligibility conditions', () => {
    // Create a program with 2 questions and an eligibility condition.
    const fullProgramName = 'Test program for eligibility navigation flows'
    const eligibilityQuestionId = 'nav-predicate-number-q'

    test.beforeAll(async () => {
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

    test('does not show Not Eligible when there is no answer', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.clickApplyProgramButton(fullProgramName)

      await applicantQuestions.expectQuestionHasNoEligibilityIndicator(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
    })

    test('shows not eligible with ineligible answer', async () => {
      const {page, applicantQuestions} = ctx
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
      await validateScreenshot(
        page,
        'application-ineligible-same-application',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('shows may be eligible with an eligible answer', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and without submitting.
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
      await validateScreenshot(
        page,
        'eligible-toast',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )

      // Verify the question is marked eligible
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ true,
      )
      await validateScreenshot(
        page,
        'eligible-home-page-program-tag',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
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

    test('shows not eligible with ineligible answer from another application', async () => {
      const {page, adminPrograms, applicantQuestions} = ctx
      const overlappingOneQProgramName =
        'Test program with one overlapping question for eligibility navigation flows'

      // Add the partial program.
      await loginAsAdmin(page)
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
      await validateScreenshot(
        page,
        'ineligible-home-page-program-tag',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ false,
      )
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await validateToastMessage(page, 'may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(
        page,
        'application-ineligible-preexisting-data',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('shows not eligible upon submit with ineligible answer', async () => {
      const {applicantQuestions} = ctx
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

    test('shows not eligible upon submit with ineligible answer with gating eligibility', async () => {
      const {applicantQuestions} = ctx
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

    test('ineligible page renders markdown', async () => {
      const {
        page,
        adminQuestions,
        applicantQuestions,
        adminPredicates,
        adminPrograms,
      } = ctx
      const questionName = 'question-with-markdown'
      const programName = 'Program with markdown question'

      // Add a question with markdown in the question text
      await loginAsAdmin(page)
      await adminQuestions.addTextQuestion({
        questionName: questionName,
        questionText:
          'This is a _question_ with some [markdown](https://www.example.com) and \n line \n\n breaks',
        // Newline characters break the comparison, so pass in just the first part of the question text
        expectedQuestionText:
          'This is a _question_ with some [markdown](https://www.example.com)',
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, 'first description', [
        questionName,
      ])
      // Add an eligiblity condition on the markdown question
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
      )
      await adminPredicates.addPredicate(
        questionName,
        /* action= */ null,
        'text',
        'is equal to',
        'foo',
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)
      await logout(page)

      // Apply to the program and answer the eligibility question with an ineligible answer
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('bar')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await validateScreenshot(
        page,
        'ineligible-page-with-markdown',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows may be eligible with nongating eligibility', async () => {
      const {page, adminPrograms, applicantQuestions} = ctx

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

    test('does not show not eligible with nongating eligibility', async () => {
      const {page, adminPrograms, applicantQuestions} = ctx

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

  test.describe('address correction', () => {
    const multiBlockMultiAddressProgram =
      'Address correction multi-block, multi-address program'
    const singleBlockMultiAddressProgram =
      'Address correction single-block, multi-address program'
    const singleBlockSingleAddressProgram =
      'Address correction single-block, single-address program'

    const addressWithCorrectionQuestionId = 'address-with-correction-q'
    const addressWithoutCorrectionQuestionId = 'address-without-correction-q'
    const textQuestionId = 'text-q'

    const addressWithCorrectionText = 'With Correction'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      // Create all questions
      await adminQuestions.addAddressQuestion({
        questionName: addressWithCorrectionQuestionId,
        questionText: addressWithCorrectionText,
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
      test('can correct address multi-block, multi-address program (save flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await disableFeatureFlag(page, 'save_on_all_actions')
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
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressWithCorrectionText,
          'Address In Area',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('can correct address multi-block, multi-address program (save flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await enableFeatureFlag(page, 'save_on_all_actions')
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
        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.answerTextQuestion('Some text')
        await applicantQuestions.clickNext()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressWithCorrectionText,
          'Address In Area',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('can correct address single-block, multi-address program (save flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await disableFeatureFlag(page, 'save_on_all_actions')
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
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressWithCorrectionText,
          'Address In Area',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('can correct address single-block, multi-address program (save flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await enableFeatureFlag(page, 'save_on_all_actions')
        await applicantQuestions.applyProgram(singleBlockMultiAddressProgram)

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

        await applicantQuestions.selectAddressSuggestion(
          'Address With No Service Area Features',
        )
        await applicantQuestions.clickConfirmAddress()

        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressWithCorrectionText,
          'Address With No Service Area Features',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('can correct address single-block, single-address program (save flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await disableFeatureFlag(page, 'save_on_all_actions')
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
        await validateScreenshot(
          page,
          'verify-address-with-suggestions-save-on-all-flag-off',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )

        await applicantQuestions.clickNext()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressWithCorrectionText,
          'Address In Area',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('can correct address single-block, single-address program (save flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await enableFeatureFlag(page, 'save_on_all_actions')
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

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
        await validateScreenshot(
          page,
          'verify-address-with-suggestions-save-on-all-flag-on',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )

        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressWithCorrectionText,
          'Address In Area',
        )
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('prompts user to edit if no suggestions are returned (save flag off)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await disableFeatureFlag(page, 'save_on_all_actions')
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
        await validateScreenshot(
          page,
          'verify-address-no-suggestions-save-on-all-flag-off',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )

        // Can continue on anyway
        await applicantQuestions.clickNext()
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('prompts user to edit if no suggestions are returned (save flag on)', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await enableFeatureFlag(page, 'save_on_all_actions')
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
        await validateScreenshot(
          page,
          'verify-address-no-suggestions-save-on-all-flag-on',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )

        // Can continue on anyway
        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('prompts user to edit if an error is returned from the Esri service (save flag off)', async () => {
        // This is currently the same as when no suggestions are returned.
        // We may change this later.
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await disableFeatureFlag(page, 'save_on_all_actions')
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
        await validateScreenshot(
          page,
          'verify-address-esri-service-error-save-on-all-flag-off',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )

        // Can continue on anyway
        await applicantQuestions.clickNext()
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('prompts user to edit if an error is returned from the Esri service (save flag on)', async () => {
        // This is currently the same as when no suggestions are returned.
        // We may change this later.
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await enableFeatureFlag(page, 'save_on_all_actions')
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
        await validateScreenshot(
          page,
          'verify-address-esri-service-error-save-on-all-flag-on',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )

        // Can continue on anyway
        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('skips the address correction screen if the user enters an address that exactly matches one of the returned suggestions', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)
        // Fill out application with address that is contained in findAddressCandidates.json
        // (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
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

      test('clicking previous on address correction page takes you back to address entry page', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await disableFeatureFlag(page, 'save_on_all_actions')

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

      test('clicking previous on address correction page does not save selection when flag off', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Opt for one of the suggested addresses
        await applicantQuestions.selectAddressSuggestion(
          'Address With No Service Area Features',
        )

        await applicantQuestions.clickPrevious()

        // When the Previous button doesn't save answers, the original address should be
        // the answer because the suggested address selection wasn't saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressWithCorrectionText,
          'Legit Address',
        )
        await logout(page)
      })

      test('clicking review on address correction page does not save selection when flag off', async () => {
        const {page, applicantQuestions} = ctx
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Opt for one of the suggested addresses
        await applicantQuestions.selectAddressSuggestion(
          'Address With No Service Area Features',
        )

        await applicantQuestions.clickReview()

        // When the Review button doesn't save answers, the original address should be
        // the answer because the suggested address selection wasn't saved
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressWithCorrectionText,
          'Legit Address',
        )
        await logout(page)
      })

      /** Tests for the buttons on a block with an address question and buttons on the subsequent address correction screen when the SAVE_ON_ALL_ACTIONS flag is enabled. */
      test.describe('address buttons, save_on_all_actions flag on', () => {
        const programName = 'Test program for file upload buttons'
        const emailQuestionText = 'Test email question'
        const addressQuestionText = 'Test address question'
        const numberQuestionText = 'Test number question'

        test.beforeAll(async () => {
          const {page, adminQuestions, adminPrograms} = ctx
          await loginAsAdmin(page)
          await enableFeatureFlag(page, 'esri_address_correction_enabled')
          await enableFeatureFlag(page, 'save_on_all_actions')

          // Create a program with 3 blocks:
          // - Block 1: Optional email question
          // - Block 2: Required address question
          // - Block 3: Optional number question
          // Having blocks before and after the address question lets us verify
          // the previous and next buttons work correctly.
          // Making the questions optional lets us click "Review" and "Previous"
          // without seeing the "error saving answers" modal, since that modal will
          // trigger if there are validation errors (like missing required questions).
          await adminQuestions.addEmailQuestion({
            questionName: 'email-test-q',
            questionText: emailQuestionText,
          })
          await adminQuestions.addAddressQuestion({
            questionName: 'address-question-test-q',
            questionText: addressQuestionText,
          })
          await adminQuestions.addNumberQuestion({
            questionName: 'number-test-q',
            questionText: numberQuestionText,
          })

          await adminPrograms.addProgram(programName)
          await adminPrograms.editProgramBlockWithOptional(
            programName,
            'Email block',
            [],
            'email-test-q',
          )

          await adminPrograms.addProgramBlock(programName, 'Address block', [
            'address-question-test-q',
          ])
          await adminPrograms.goToBlockInProgram(programName, 'Screen 2')
          await adminPrograms.clickAddressCorrectionToggleByName(
            addressQuestionText,
          )

          await adminPrograms.addProgramBlock(programName)
          await adminPrograms.goToBlockInProgram(programName, 'Screen 3')
          await adminPrograms.editProgramBlockWithOptional(
            programName,
            'Number block',
            [],
            'number-test-q',
          )

          await adminPrograms.publishAllDrafts()
          await logout(page)
        })

        test.describe('previous button', () => {
          test('clicking previous on page with address question redirects to address correction (no suggestions)', async () => {
            const {applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )

            await applicantQuestions.answerAddressQuestion(
              'Bogus Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )

            await applicantQuestions.clickPrevious()

            await applicantQuestions.expectVerifyAddressPage(false)
          })

          test('clicking previous on page with address question redirects to address correction (has suggestions)', async () => {
            const {applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )

            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )

            await applicantQuestions.clickPrevious()

            await applicantQuestions.expectVerifyAddressPage(true)
          })

          test('address correction page saves original address when selected and redirects to previous', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
            await applicantQuestions.clickPrevious()

            await applicantQuestions.expectVerifyAddressPage(true)

            // Opt to keep the original address entered
            await applicantQuestions.selectAddressSuggestion('Legit Address')
            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the previous page, which has the email question
            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(emailQuestionText)

            // Verify the original address was saved
            await applicantQuestions.clickReview()
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Legit Address',
            )
          })

          test('address correction page saves suggested address when selected and redirects to previous', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
            await applicantQuestions.clickPrevious()
            await applicantQuestions.expectVerifyAddressPage(true)

            // Opt for one of the suggested addresses
            await applicantQuestions.selectAddressSuggestion(
              'Address With No Service Area Features',
            )
            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the previous page, which has the email question
            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(emailQuestionText)

            // Verify the suggested address was saved
            await applicantQuestions.clickReview()
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Address With No Service Area Features',
            )
          })

          test('address correction page saves original address when no suggestions offered and redirects to previous', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Bogus Address',
              '',
              'Seattle',
              'WA',
              '98109',
            )
            await applicantQuestions.clickPrevious()
            await applicantQuestions.expectVerifyAddressPage(false)

            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the previous page, which has the email question
            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(emailQuestionText)

            // Verify the suggested address was saved
            await applicantQuestions.clickReview()
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Bogus Address',
            )
          })

          test('clicking previous saves address and goes to previous block if the user enters an address that exactly matches suggestions', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            // Fill out application with address that is contained in findAddressCandidates.json
            // (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
            await applicantQuestions.answerAddressQuestion(
              'Address In Area',
              '',
              'Redlands',
              'CA',
              '92373',
            )

            await applicantQuestions.clickPrevious()

            // Verify we're taken to the previous page, which has the email question
            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(emailQuestionText)

            // Verify the address was saved
            await applicantQuestions.clickReview()
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Address In Area',
            )

            await logout(page)
          })
        })

        test.describe('review button', () => {
          test('clicking review on page with address question redirects to address correction (no suggestions)', async () => {
            const {applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )

            await applicantQuestions.answerAddressQuestion(
              'Bogus Address',
              '',
              'Seattle',
              'WA',
              '98109',
            )

            await applicantQuestions.clickReview()

            await applicantQuestions.expectVerifyAddressPage(false)
          })

          test('clicking review on page with address question redirects to address correction (has suggestions)', async () => {
            const {page, applicantQuestions} = ctx
            await enableFeatureFlag(page, 'esri_address_correction_enabled')
            await enableFeatureFlag(page, 'save_on_all_actions')

            await applicantQuestions.applyProgram(
              singleBlockSingleAddressProgram,
            )
            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )

            await applicantQuestions.clickReview()

            await applicantQuestions.expectVerifyAddressPage(true)
          })

          test('address correction page saves original address when selected and redirects to review', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
            await applicantQuestions.clickReview()
            await applicantQuestions.expectVerifyAddressPage(true)

            // Opt to keep the original address entered
            await applicantQuestions.selectAddressSuggestion('Legit Address')

            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the review page
            await applicantQuestions.expectReviewPage()
            // Verify the original address was saved
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Legit Address',
            )

            await logout(page)
          })

          test('address correction page saves suggested address when selected and redirects to review', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
            await applicantQuestions.clickReview()
            await applicantQuestions.expectVerifyAddressPage(true)

            // Opt for one of the suggested addresses
            await applicantQuestions.selectAddressSuggestion(
              'Address With No Service Area Features',
            )

            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the review page
            await applicantQuestions.expectReviewPage()
            // Verify the original address was saved
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Address With No Service Area Features',
            )
            await logout(page)
          })

          test('address correction page saves original address when no suggestions offered and redirects to review', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Bogus Address',
              '',
              'Seattle',
              'WA',
              '98109',
            )
            await applicantQuestions.clickReview()
            await applicantQuestions.expectVerifyAddressPage(false)

            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the review page
            await applicantQuestions.expectReviewPage()
            // Verify the original address was saved
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Bogus Address',
            )

            await logout(page)
          })

          test('clicking review saves address and goes to review page if the user enters an address that exactly matches suggestions', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            // Fill out application with address that is contained in findAddressCandidates.json
            // (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
            await applicantQuestions.answerAddressQuestion(
              'Address In Area',
              '',
              'Redlands',
              'CA',
              '92373',
            )

            await applicantQuestions.clickReview()

            await applicantQuestions.expectReviewPage()
            // Verify the applicant's answer is saved
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Address In Area',
            )

            await logout(page)
          })
        })

        test.describe('save & next button', () => {
          test('clicking next on page with address question redirects to address correction (no suggestions)', async () => {
            const {applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )

            await applicantQuestions.answerAddressQuestion(
              'Bogus Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )

            await applicantQuestions.clickNext()

            await applicantQuestions.expectVerifyAddressPage(false)
          })

          test('clicking next on page with address question redirects to address correction (has suggestions)', async () => {
            const {applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )

            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )

            await applicantQuestions.clickNext()

            await applicantQuestions.expectVerifyAddressPage(true)
          })

          test('address correction page saves original address when selected and redirects to next', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
            await applicantQuestions.clickNext()
            await applicantQuestions.expectVerifyAddressPage(true)

            // Opt to keep the original address entered
            await applicantQuestions.selectAddressSuggestion('Legit Address')
            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the next page, which has the number question
            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(numberQuestionText)

            // Verify the original address was saved
            await applicantQuestions.clickReview()
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Legit Address',
            )
          })

          test('address correction page saves suggested address when selected and redirects to next', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
            await applicantQuestions.clickNext()
            await applicantQuestions.expectVerifyAddressPage(true)

            // Opt for one of the suggested addresses
            await applicantQuestions.selectAddressSuggestion(
              'Address With No Service Area Features',
            )
            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the next page, which has the number question
            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(numberQuestionText)

            // Verify the suggested address was saved
            await applicantQuestions.clickReview()
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Address With No Service Area Features',
            )
          })

          test('address correction page saves original address when no suggestions offered and redirects to next', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            await applicantQuestions.answerAddressQuestion(
              'Bogus Address',
              '',
              'Seattle',
              'WA',
              '98109',
            )
            await applicantQuestions.clickNext()
            await applicantQuestions.expectVerifyAddressPage(false)

            await applicantQuestions.clickConfirmAddress()

            // Verify we're taken to the next page, which has the number question
            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(numberQuestionText)

            // Verify the original address was saved
            await applicantQuestions.clickReview()
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Bogus Address',
            )
          })

          test('clicking next saves address and goes to next block if the user enters an address that exactly matches suggestions', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )
            // Fill out application with address that is contained in findAddressCandidates.json
            // (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
            await applicantQuestions.answerAddressQuestion(
              'Address In Area',
              '',
              'Redlands',
              'CA',
              '92373',
            )

            await applicantQuestions.clickNext()

            // Verify we're taken to the next page, which has the number question
            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(numberQuestionText)

            // Verify the address was saved
            await applicantQuestions.clickReview()
            await applicantQuestions.expectQuestionAnsweredOnReviewPage(
              addressQuestionText,
              'Address In Area',
            )

            await logout(page)
          })
        })

        test.describe('go back and edit button', () => {
          test('clicking go back and edit on address correction goes back to page with address question', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )

            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
            await applicantQuestions.clickNext()
            await applicantQuestions.expectVerifyAddressPage(true)

            await applicantQuestions.clickGoBackAndEdit()

            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(addressQuestionText)
          })

          test('go back and edit does not save address selection', async () => {
            const {page, applicantQuestions} = ctx

            await applicantQuestions.clickApplyProgramButton(programName)
            await applicantQuestions.answerQuestionFromReviewPage(
              addressQuestionText,
            )

            await applicantQuestions.answerAddressQuestion(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
            await applicantQuestions.clickNext()
            await applicantQuestions.expectVerifyAddressPage(true)

            // Select an address suggestion, but then click "Go back and edit", which shouldn't save the suggestion
            await applicantQuestions.selectAddressSuggestion(
              'Address With No Service Area Features',
            )

            await applicantQuestions.clickGoBackAndEdit()

            expect(
              await page.innerText('.cf-applicant-question-text'),
            ).toContain(addressQuestionText)
            // Verify the original address (not the suggested address) is filled in on the block page
            await applicantQuestions.checkAddressQuestionValue(
              'Legit Address',
              '',
              'Redlands',
              'CA',
              '92373',
            )
          })
        })
      })
    }

    test('address correction page does not show if feature is disabled', async () => {
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
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        addressWithCorrectionText,
        '305 Harrison',
      )
      await applicantQuestions.clickSubmit()
      await logout(page)
    })
  })

  if (isLocalDevEnvironment()) {
    test.describe('using address as visibility condition', () => {
      const programName = 'Test program for address as visibility condition'
      const questionAddress = 'address-test-q'
      const questionText1 = 'text-test-q-one'
      const questionText2 = 'text-test-q-two'
      const screen1 = 'Screen 1'
      const screen2 = 'Screen 2'
      const screen3 = 'Screen 3'

      test.beforeAll(async () => {
        const {page, adminQuestions, adminPrograms, adminPredicates} = ctx
        await loginAsAdmin(page)
        await enableFeatureFlag(page, 'esri_address_correction_enabled')
        await enableFeatureFlag(
          page,
          'esri_address_service_area_validation_enabled',
        )

        // Create Questions
        await adminQuestions.addAddressQuestion({
          questionName: questionAddress,
          questionText: questionAddress,
        })

        await adminQuestions.addTextQuestion({
          questionName: questionText1,
          questionText: questionText1,
        })

        await adminQuestions.addTextQuestion({
          questionName: questionText2,
          questionText: questionText2,
        })

        // Create Program
        await adminPrograms.addProgram(programName)

        // Attach questions to program
        await adminPrograms.editProgramBlock(programName, screen1, [
          questionAddress,
        ])

        await adminPrograms.addProgramBlock(programName, screen2, [
          questionText1,
        ])

        await adminPrograms.addProgramBlock(programName, screen3, [
          questionText2,
        ])

        await adminPrograms.goToBlockInProgram(programName, screen1)

        await adminPrograms.clickAddressCorrectionToggleByName(questionAddress)

        const addressCorrectionInput =
          adminPrograms.getAddressCorrectionToggleByName(questionAddress)

        await expect(addressCorrectionInput).toHaveValue('true')

        // Set thing to soft eligibilty
        await adminPrograms.toggleEligibilityGating()

        // Add address eligibility predicate
        await adminPrograms.goToEditBlockEligibilityPredicatePage(
          programName,
          screen1,
        )

        await adminPredicates.addPredicates([
          {
            questionName: questionAddress,
            scalar: 'service_area',
            operator: 'in service area',
            values: ['Seattle'],
          },
        ])

        // Add the address visibility predicate
        await adminPrograms.goToBlockInProgram(programName, screen2)

        await adminPrograms.goToEditBlockVisibilityPredicatePage(
          programName,
          screen2,
        )

        await adminPredicates.addPredicates([
          {
            questionName: questionAddress,
            action: 'shown if',
            scalar: 'service_area',
            operator: 'in service area',
            values: ['Seattle'],
          },
        ])

        // Publish Program
        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(programName)

        await logout(page)
      })

      test('when address is eligible show hidden screen', async () => {
        const {page, applicantQuestions} = ctx
        await disableFeatureFlag(page, 'save_on_all_actions')

        await applicantQuestions.applyProgram(programName)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Seattle',
          'WA',
          '98109',
          0,
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)
        await applicantQuestions.clickNext()
        // Screen 1 will only be visible when the address is validated as being eligible. This test case uses an valid address.
        await applicantQuestions.answerTextQuestion('answer 1')
        await applicantQuestions.clickNext()
        await applicantQuestions.answerTextQuestion('answer 2')
        await applicantQuestions.clickNext()

        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          questionAddress,
          'Address In Area',
        )

        await applicantQuestions.clickSubmit()
        await logout(page)
      })

      test('when address is not eligible do not show hidden screen', async () => {
        const {page, applicantQuestions} = ctx
        await applicantQuestions.applyProgram(programName)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Nonlegit Address',
          '',
          'Seattle',
          'WA',
          '98109',
          0,
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(false)
        await applicantQuestions.clickNext()
        // Screen 1 will only be visible when the address is validated as being eligible. This test case uses an invalid address.
        await applicantQuestions.answerTextQuestion('answer 2')
        await applicantQuestions.clickNext()

        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          questionAddress,
          'Nonlegit Address',
        )

        await applicantQuestions.clickSubmit()
        await logout(page)
      })
    })
  }

  // TODO: Add tests for "next" navigation
})
