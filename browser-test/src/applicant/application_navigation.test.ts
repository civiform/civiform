import {test, expect} from '../support/civiform_fixtures'
import {
  AdminQuestions,
  ClientInformation,
  disableFeatureFlag,
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  loginAsTestUser,
  loginAsTrustedIntermediary,
  logout,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
  waitForPageJsLoad,
} from '../support'
import {Eligibility, ProgramVisibility} from '../support/admin_programs'

test.describe('Applicant navigation flow', {tag: ['@uses-fixtures']}, () => {
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

    test.describe('next button', () => {
      test('next block progression', async ({page, applicantQuestions}) => {
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
        await applicantQuestions.applyProgram(programName)

        await applicantQuestions.clickPrevious()

        // Assert that we're on the preview page.
        await applicantQuestions.expectReviewPage()
      })

      test('clicking previous on later blocks goes to previous blocks', async ({
        applicantQuestions,
      }) => {
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

  test.describe('navigation with common intake', () => {
    // Create two programs, one is common intake
    const commonIntakeProgramName = 'Test Common Intake Form Program'
    const secondProgramName = 'Test Regular Program with Eligibility Conditions'
    const eligibilityQuestionId = 'nav-predicate-number-q'
    const secondProgramCorrectAnswer = '5'

    test.beforeEach(
      async ({page, adminQuestions, adminPredicates, adminPrograms}) => {
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
        await logout(page)
      },
    )

    test('does not show eligible programs or upsell on confirmation page when no programs are eligible and signed in', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('shows eligible programs and no upsell on confirmation page when programs are eligible and signed in', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('does not show eligible programs and shows upsell on confirmation page when no programs are eligible and a guest user', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('shows eligible programs and upsell on confirmation page when programs are eligible and a guest user', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('shows intake form as submitted after completion', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('does not show eligible programs and shows TI text on confirmation page when no programs are eligible and a TI', async ({
      page,
      tiDashboard,
      applicantQuestions,
    }) => {
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

    test('shows eligible programs and TI text on confirmation page when programs are eligible and a TI', async ({
      page,
      tiDashboard,
      applicantQuestions,
    }) => {
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

    test.beforeEach(
      async ({page, adminQuestions, adminPredicates, adminPrograms}) => {
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
        await logout(page)
      },
    )

    test('does not show Not Eligible when there is no answer', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.clickApplyProgramButton(fullProgramName)

      await applicantQuestions.expectQuestionHasNoEligibilityIndicator(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
    })

    test('shows not eligible with ineligible answer', async ({
      page,
      applicantQuestions,
    }) => {
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

    test(
      'North Star shows ineligible on home page',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await applicantQuestions.applyProgram(fullProgramName)

        await test.step('fill out application and submit', async () => {
          await applicantQuestions.answerNumberQuestion('1')
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectIneligiblePage()
        })

        await test.step('verify question is marked ineligible', async () => {
          await applicantQuestions.gotoApplicantHomePage()
          await applicantQuestions.seeEligibilityTag(
            fullProgramName,
            /* isEligible= */ false,
          )

          await validateScreenshot(
            page,
            'ineligible-home-page-program-tagnorthstar',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })
        await disableFeatureFlag(page, 'north_star_applicant_ui')
      },
    )

    test(
      'North Star shows eligible on home page',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await applicantQuestions.applyProgram(fullProgramName)

        await test.step('fill out application and submit', async () => {
          await applicantQuestions.answerNumberQuestion('5')
          await applicantQuestions.clickContinue()
        })

        await test.step('verify question is marked eligible', async () => {
          await applicantQuestions.gotoApplicantHomePage()
          await applicantQuestions.seeEligibilityTag(
            fullProgramName,
            /* isEligible= */ true,
          )

          await validateScreenshot(
            page,
            'eligible-home-page-program-tagnorthstar',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })
        await disableFeatureFlag(page, 'north_star_applicant_ui')
      },
    )

    test('shows may be eligible with an eligible answer', async ({
      page,
      applicantQuestions,
    }) => {
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

    test(
      'North Star shows may be eligible toast with an eligible answer',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await applicantQuestions.applyProgram(fullProgramName)

        // Fill out application and without submitting.
        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.clickContinue()
        await validateToastMessage(page, 'may qualify')
        await validateScreenshot(
          page,
          'north-star-eligible-toast',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
        await disableFeatureFlag(page, 'north_star_applicant_ui')
      },
    )

    test('shows not eligible with ineligible answer from another application', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
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

    test('shows not eligible upon submit with ineligible answer', async ({
      applicantQuestions,
    }) => {
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

    test('shows not eligible upon submit with ineligible answer with gating eligibility', async ({
      applicantQuestions,
    }) => {
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

    test('ineligible page renders markdown', async ({
      page,
      adminQuestions,
      applicantQuestions,
      adminPredicates,
      adminPrograms,
    }) => {
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

    test('shows may be eligible with nongating eligibility', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await loginAsAdmin(page)
      await adminPrograms.createNewVersion(fullProgramName)
      await adminPrograms.setProgramEligibility(
        fullProgramName,
        Eligibility.IS_NOT_GATING,
      )
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

    test('does not show not eligible with nongating eligibility', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await loginAsAdmin(page)
      await adminPrograms.createNewVersion(fullProgramName)
      await adminPrograms.setProgramEligibility(
        fullProgramName,
        Eligibility.IS_NOT_GATING,
      )
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

  if (isLocalDevEnvironment()) {
    test.describe('using address as visibility condition', () => {
      const programName = 'Test program for address as visibility condition'
      const questionAddress = 'address-test-q'
      const questionText1 = 'text-test-q-one'
      const questionText2 = 'text-test-q-two'
      const screen1 = 'Screen 1'
      const screen2 = 'Screen 2'
      const screen3 = 'Screen 3'

      test.beforeEach(
        async ({page, adminQuestions, adminPrograms, adminPredicates}) => {
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

          await adminPrograms.clickAddressCorrectionToggleByName(
            questionAddress,
          )

          const addressCorrectionInput =
            adminPrograms.getAddressCorrectionToggleByName(questionAddress)

          await expect(addressCorrectionInput).toHaveValue('true')

          await adminPrograms.setProgramEligibility(
            programName,
            Eligibility.IS_NOT_GATING,
          )

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
        },
      )

      test('when address is eligible show hidden screen', async ({
        page,
        applicantQuestions,
      }) => {
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
        await applicantQuestions.clickConfirmAddress()
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

      test('when address is not eligible do not show hidden screen', async ({
        page,
        applicantQuestions,
      }) => {
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
        await applicantQuestions.clickConfirmAddress()
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
})
