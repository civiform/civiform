import {test} from '../../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
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
      await adminPrograms.addProgramBlockUsingSpec(
        programName,
        'second description',
        [{name: 'nav-static-q', isOptional: false}],
      )
      await adminPrograms.addProgramBlockUsingSpec(
        programName,
        'third description',
        [{name: 'nav-address-q', isOptional: false}],
      )
      await adminPrograms.addProgramBlockUsingSpec(
        programName,
        'fourth description',
        [{name: 'nav-radio-q', isOptional: true}],
      )
      await adminPrograms.addProgramBlockUsingSpec(
        programName,
        'fifth description',
        [
          {name: 'nav-phone-q', isOptional: false},
          {name: 'nav-currency-q', isOptional: true},
        ],
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })

    test.describe('next button', () => {
      test('next block progression', async ({page, applicantQuestions}) => {
        test.slow()

        await applicantQuestions.clickApplyProgramButton(programName)

        await validateAccessibility(page)
        await validateScreenshot(
          page,
          'program-preview',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )

        await test.step('empty screen 1', async () => {
          await applicantQuestions.clickContinue()
          await applicantQuestions.validateQuestionIsOnPage(dateQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)

          // This test is for issue #6987. If the user opens a block and doesn't
          // answer any questions then clicks "Review" or "Previous", we should
          // let them go to the review page / previous page and not show errors.
          // But if they click "Save & next", we *should* show the errors because
          // the next block may have a visibility conditions that depends on the
          // answers to this block.
          await applicantQuestions.answerDateQuestion('')
          await applicantQuestions.answerEmailQuestion('')
          await applicantQuestions.clickNext()

          // Expect we're still on the same page and see errors since the questions
          // weren't answered
          await applicantQuestions.validateQuestionIsOnPage(dateQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
          await applicantQuestions.expectRequiredQuestionError(
            '.cf-question-date',
          )
          await applicantQuestions.expectRequiredQuestionError(
            '.cf-question-email',
          )
        })

        await test.step('incomplete screen 1', async () => {
          await applicantQuestions.validateQuestionIsOnPage(dateQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
          await applicantQuestions.answerDateQuestion('2021-11-01')
          await applicantQuestions.answerEmailQuestion('')
          await applicantQuestions.clickNext()

          // Expect we're still on the same page and see errors since email wasn't answered
          await applicantQuestions.validateQuestionIsOnPage(dateQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
          await applicantQuestions.expectRequiredQuestionError(
            '.cf-question-email',
          )
        })

        await test.step('complete screen 1', async () => {
          await applicantQuestions.answerDateQuestion('2021-11-01')
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickNext()
        })

        await test.step('screen 2', async () => {
          await applicantQuestions.validateQuestionIsOnPage(staticQuestionText)
          await applicantQuestions.clickNext()
        })

        await test.step('screen 3', async () => {
          await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)
          await applicantQuestions.answerAddressQuestion(
            '1234 St',
            'Unit B',
            'Sim',
            'WA',
            '54321',
          )
          await applicantQuestions.clickNext()
        })

        await test.step('screen 4', async () => {
          await applicantQuestions.validateQuestionIsOnPage(radioQuestionText)
          await applicantQuestions.answerRadioButtonQuestion('two')
          await applicantQuestions.clickNext()
        })

        await test.step('screen 5', async () => {
          await applicantQuestions.validateQuestionIsOnPage(phoneQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(
            currencyQuestionText,
          )
          await applicantQuestions.answerPhoneQuestion('4256373270')
          await applicantQuestions.answerCurrencyQuestion('5')
        })

        await test.step('clicking next on last block goes to review page', async () => {
          await applicantQuestions.clickNext()
          await applicantQuestions.expectReviewPage()
        })

        await test.step('review page has all saved answers', async () => {
          await applicantQuestions.expectQuestionAnsweredOnReviewPage(
            dateQuestionText,
            '11/01/2021',
          )
          await applicantQuestions.expectQuestionAnsweredOnReviewPage(
            emailQuestionText,
            'test1@gmail.com',
          )
          await applicantQuestions.expectQuestionAnsweredOnReviewPage(
            addressQuestionText,
            '1234 St',
          )
          await applicantQuestions.expectQuestionAnsweredOnReviewPage(
            radioQuestionText,
            'two',
          )
          await applicantQuestions.expectQuestionAnsweredOnReviewPage(
            phoneQuestionText,
            '425-637-3270',
          )
          await applicantQuestions.expectQuestionAnsweredOnReviewPage(
            currencyQuestionText,
            '5',
          )
          await validateAccessibility(page)
          await validateScreenshot(
            page,
            'program-review',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('can submit application', async () => {
          await applicantQuestions.submitFromReviewPage()
          await applicantQuestions.expectConfirmationPage()
        })
      })

      test('can skip optional questions', async ({applicantQuestions}) => {
        test.slow()

        await applicantQuestions.applyProgram(programName)

        await test.step('screen 1', async () => {
          await applicantQuestions.answerDateQuestion('2021-11-01')
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickNext()
        })

        await test.step('screen 2', async () => {
          await applicantQuestions.validateQuestionIsOnPage(staticQuestionText)
          await applicantQuestions.clickNext()
        })

        await test.step('screen 3', async () => {
          await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)
          await applicantQuestions.answerAddressQuestion(
            '1234 St',
            'Unit B',
            'Sim',
            'WA',
            '54321',
          )
          await applicantQuestions.clickNext()
        })

        await test.step('screen 4 (optional)', async () => {
          await applicantQuestions.validateQuestionIsOnPage(radioQuestionText)
          // Don't answer the optional radio question
          await applicantQuestions.clickNext()
        })

        await test.step('screen 5 (currency optional)', async () => {
          await applicantQuestions.validateQuestionIsOnPage(phoneQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(
            currencyQuestionText,
          )

          await applicantQuestions.answerPhoneQuestion('4256373270')
          // Don't answer the optional currency question
          await applicantQuestions.clickNext()
        })

        await test.step('can submit application', async () => {
          await applicantQuestions.expectReviewPage()
          await applicantQuestions.submitFromReviewPage()
          await applicantQuestions.expectConfirmationPage()
        })
      })

      test('answering questions out of order', async ({
        page,
        applicantQuestions,
      }) => {
        test.slow()

        await applicantQuestions.clickApplyProgramButton(programName)

        await test.step('answer screen 4', async () => {
          await applicantQuestions.answerQuestionFromReviewPage(
            radioQuestionText,
          )
          await applicantQuestions.validateQuestionIsOnPage(radioQuestionText)
          await applicantQuestions.answerRadioButtonQuestion('one')
          await applicantQuestions.clickReview()

          await validateScreenshot(
            page,
            'fourth-question-answered',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('answer screen 1', async () => {
          await applicantQuestions.answerQuestionFromReviewPage(
            dateQuestionText,
          )
          await applicantQuestions.answerDateQuestion('2021-11-01')
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview()
        })

        // Re-answering questions by clicking "Edit" on the review page puts the application
        // in "review mode", which we'll test in the next step
        await test.step('re-answer screen 4', async () => {
          await applicantQuestions.editQuestionFromReviewPage(radioQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(radioQuestionText)
          await applicantQuestions.answerRadioButtonQuestion('two')
        })

        // In review mode, you're only taken to blocks you haven't yet answered.
        // Screen 2 is just a static question which applicants don't need to answer,
        // so the first unanswered screen is screen 3.
        await test.step('next screen is screen 3', async () => {
          await applicantQuestions.clickNext()
          await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)
          await applicantQuestions.answerAddressQuestion(
            '1234 St',
            'Unit B',
            'Sim',
            'WA',
            '54321',
          )
        })

        // The next unanswered block after screen 3 is screen 5
        await test.step('next screen is screen 5', async () => {
          await applicantQuestions.clickNext()
          await applicantQuestions.validateQuestionIsOnPage(phoneQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(
            currencyQuestionText,
          )
          await applicantQuestions.answerPhoneQuestion('4256373270')
        })

        // All the blocks are answered, so we should now be taken to the review page
        await test.step('next screen is review page', async () => {
          await applicantQuestions.clickNext()
          await applicantQuestions.expectReviewPage()
        })

        await test.step('cannot delete answers to required questions', async () => {
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
          )
          await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)
          await applicantQuestions.answerAddressQuestion('', '', '', '', '')
          await applicantQuestions.clickNext()

          // Verify we stay on the same page and the error is shown
          await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)
          await applicantQuestions.expectRequiredQuestionError(
            '.cf-question-address',
          )

          // Correctly answer the question
          await applicantQuestions.answerAddressQuestion(
            '5678 St',
            'Unit B',
            'Sim',
            'WA',
            '54321',
          )
          await applicantQuestions.clickNext()
        })

        await test.step('can submit application', async () => {
          await applicantQuestions.expectReviewPage()
          await applicantQuestions.submitFromReviewPage()
          await applicantQuestions.expectConfirmationPage()
        })
      })
    })

    test.describe('previous button', () => {
      test('clicking previous on first block goes to summary page', async ({
        applicantQuestions,
      }) => {
        test.slow()

        await applicantQuestions.applyProgram(programName)

        await applicantQuestions.clickPrevious()

        // Assert that we're on the preview page.
        await applicantQuestions.expectReviewPage()
      })

      test('clicking previous on later blocks goes to previous blocks', async ({
        applicantQuestions,
      }) => {
        test.slow()

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

      test('clicking previous with correct form shows previous page and saves answers', async ({
        applicantQuestions,
      }) => {
        test.slow()

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

      test('clicking previous with some missing answers shows error modal', async ({
        page,
        applicantQuestions,
      }) => {
        test.slow()

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickPrevious()

        // The date question is required, so we should see the error modal.
        await applicantQuestions.expectErrorOnPreviousModal()
        await validateScreenshot(page, 'error-on-previous-modal')
      })

      test('clicking previous with no answers does not show error modal', async ({
        applicantQuestions,
      }) => {
        test.slow()

        await applicantQuestions.applyProgram(programName)

        // If the applicant has never answered this block before and doesn't fill in any
        // answers now, we shouldn't show the error modal and should just go straight to
        // the previous page (which is the review page since this is the first block).
        // See issue #6987.
        await applicantQuestions.clickPrevious()

        await applicantQuestions.expectReviewPage()
      })

      test('error on previous modal > click stay and fix > shows block', async ({
        applicantQuestions,
      }) => {
        test.slow()

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

      test('error on previous modal > click previous without saving > answers not saved', async ({
        applicantQuestions,
      }) => {
        test.slow()

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

      test('error on previous modal > click previous without saving > shows previous block', async ({
        applicantQuestions,
      }) => {
        test.slow()

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('2021-11-01')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')
        await applicantQuestions.clickNext()
        // Nothing to fill in since this is the static question block
        await applicantQuestions.clickNext()

        // Only fill in half the address question, then try going to previous block
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          '',
          '',
          'WA',
          '',
        )
        await applicantQuestions.clickPrevious()
        await applicantQuestions.expectErrorOnPreviousModal()

        // Proceed to the previous page and verify the first block answers are present
        await applicantQuestions.clickPreviousWithoutSaving()
        // This is the static question block, so continue to the previous block
        await applicantQuestions.clickPrevious()

        await applicantQuestions.checkDateQuestionValue('2021-11-01')
        await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')
      })

      test('clicking previous after deleting answers to required questions shows error modal', async ({
        applicantQuestions,
      }) => {
        test.slow()

        await test.step('answer questions on first block', async () => {
          await applicantQuestions.applyProgram(programName)
          await applicantQuestions.answerDateQuestion('2021-11-01')
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview()
        })

        await test.step('delete answers on first block', async () => {
          await applicantQuestions.editQuestionFromReviewPage(
            'date question text',
          )
          await applicantQuestions.answerDateQuestion('')
          await applicantQuestions.answerEmailQuestion('')
        })

        await test.step('previous button should show error modal', async () => {
          // Because the questions were previously answered and the date and email questions are required,
          // we don't let the user save the deletion of answers.
          await applicantQuestions.clickPrevious()
          await applicantQuestions.expectErrorOnPreviousModal()
        })
      })

      test('previous saves blank optional answers', async ({
        applicantQuestions,
      }) => {
        test.slow()

        await test.step('answer blocks with all required questions', async () => {
          await applicantQuestions.applyProgram(programName)
          await applicantQuestions.answerDateQuestion('2021-11-01')
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview()

          await applicantQuestions.answerQuestionFromReviewPage(
            addressQuestionText,
          )
          await applicantQuestions.answerAddressQuestion(
            '1234 St',
            'Unit B',
            'Sim',
            'WA',
            '54321',
          )
          await applicantQuestions.clickReview()
        })

        // We require that all questions be seen before an applicant can submit an application,
        // so open the optional question but don't fill in any answer, then use the "Previous" button.
        await test.step('open optional question block but do not answer', async () => {
          await applicantQuestions.answerQuestionFromReviewPage(
            radioQuestionText,
          )
          await applicantQuestions.clickPrevious()
        })

        await test.step('answer only required questions on block with both required and optional', async () => {
          await applicantQuestions.clickReview()
          // This block has a required phone question and optional currency question.
          // Only answer the required question, then use the "Previous" button.
          await applicantQuestions.answerQuestionFromReviewPage(
            phoneQuestionText,
          )
          await applicantQuestions.answerPhoneQuestion('4256373270')
          await applicantQuestions.clickPrevious()
        })

        // Verify that the optional questions were marked as seen and we can now submit the application
        await test.step('can submit application', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.submitFromReviewPage()
          await applicantQuestions.expectConfirmationPage()
        })
      })
    })

    test.describe('review button', () => {
      test('clicking review with correct form shows review page with saved answers', async ({
        applicantQuestions,
      }) => {
        test.slow()

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

      test('clicking review with some missing answers shows modal', async ({
        page,
        applicantQuestions,
      }) => {
        test.slow()

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDateQuestion('')
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickReview()

        // The date question is required, so we should see the error modal.
        await applicantQuestions.expectErrorOnReviewModal()
        await validateScreenshot(page, 'error-on-review-modal')
      })

      test('clicking review with no answers does not show error modal', async ({
        applicantQuestions,
      }) => {
        test.slow()

        await applicantQuestions.applyProgram(programName)

        // If the applicant has never answered this block before and doesn't fill in any
        // answers now, we shouldn't show the error modal and should just go straight to
        // the review page -- see issue #6987.
        await applicantQuestions.clickReview()

        await applicantQuestions.expectReviewPage()
      })

      test('error on review modal > click stay and fix > shows block', async ({
        applicantQuestions,
      }) => {
        test.slow()

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

      test('error on review modal > click review without saving > shows review page without saved answers', async ({
        applicantQuestions,
      }) => {
        test.slow()

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

      test('clicking review after deleting answers to required questions shows error modal', async ({
        applicantQuestions,
      }) => {
        test.slow()

        await test.step('answer questions on first block', async () => {
          await applicantQuestions.applyProgram(programName)
          await applicantQuestions.answerDateQuestion('2021-11-01')
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview()
        })

        await test.step('delete answers on first block', async () => {
          await applicantQuestions.editQuestionFromReviewPage(
            'date question text',
          )
          await applicantQuestions.answerDateQuestion('')
          await applicantQuestions.answerEmailQuestion('')
        })

        await test.step('review button should show error modal', async () => {
          // Because the questions were previously answered and the date and email questions are required,
          // we don't let the user save the deletion of answers.
          await applicantQuestions.clickReview()
          await applicantQuestions.expectErrorOnReviewModal()
        })
      })

      test('review saves blank optional answers', async ({
        applicantQuestions,
      }) => {
        test.slow()

        await test.step('answer blocks with all required questions', async () => {
          await applicantQuestions.applyProgram(programName)
          await applicantQuestions.answerDateQuestion('2021-11-01')
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview()

          await applicantQuestions.answerQuestionFromReviewPage(
            addressQuestionText,
          )
          await applicantQuestions.answerAddressQuestion(
            '1234 St',
            'Unit B',
            'Sim',
            'WA',
            '54321',
          )
          await applicantQuestions.clickReview()
        })

        // We require that all questions be seen before an applicant can submit an application,
        // so open the optional question but don't fill in any answer, then use the "Review" button.
        await test.step('open optional question block but do not answer', async () => {
          await applicantQuestions.answerQuestionFromReviewPage(
            radioQuestionText,
          )
          await applicantQuestions.clickReview()
        })

        await test.step('answer only required questions on block with both required and optional', async () => {
          // This block has a required phone question and optional currency question.
          // Only answer the required question, then use the "Review" button.
          await applicantQuestions.answerQuestionFromReviewPage(
            phoneQuestionText,
          )
          await applicantQuestions.answerPhoneQuestion('4256373270')
          await applicantQuestions.clickReview()
        })

        // Verify that the optional questions were marked as seen and we can now submit the application
        await test.step('can submit application', async () => {
          await applicantQuestions.submitFromReviewPage()
          await applicantQuestions.expectConfirmationPage()
        })
      })
    })
  })
})
