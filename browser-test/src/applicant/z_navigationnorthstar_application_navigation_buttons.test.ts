import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('Applicant navigation flow', {tag: ['@northstar']}, () => {
  const programName = 'Test program for navigation flows'
  const dateQuestionText = 'date question text'
  const emailQuestionText = 'email question text'
  const staticQuestionText = 'static question text'
  const addressQuestionText = 'address question text'
  const radioQuestionText = 'radio question text'
  const phoneQuestionText = 'phone question text'
  const currencyQuestionText = 'currency question text'

  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test.describe('navigation with five blocks', () => {
    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(
        page,
        'suggest_programs_on_application_confirmation_page',
      )

      await test.step('Set up program with questions', async () => {
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
    })

    test.describe('previous button', () => {
      test('clicking previous on first block goes to summary page', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.clickBack()

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })

      test('clicking previous on later blocks goes to previous blocks', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        // Fill out the first block and click next
        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')
        await applicantQuestions.clickContinue()

        // Nothing to fill in since this is the static question block
        await applicantQuestions.clickContinue()

        // Fill out address question and click next
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
        )
        await applicantQuestions.clickContinue()

        // Click previous and see previous page with address
        await applicantQuestions.clickBack()
        await applicantQuestions.checkAddressQuestionValue(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
        )

        // Click previous and see static question page
        await applicantQuestions.clickBack()
        await applicantQuestions.seeStaticQuestion('static question text')

        // Click previous and see date and name questions
        await applicantQuestions.clickBack()
        await applicantQuestions.checkMemorableDateQuestionValue(
          '2021',
          '11',
          '1',
        )
        await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')

        // Assert that we're on the review page.
        await applicantQuestions.clickBack()
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })

      test('clicking previous with correct form shows previous page and saves answers', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickContinue()
        // Nothing to fill in since this is the static question block
        await applicantQuestions.clickContinue()

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
        await applicantQuestions.clickBack()

        await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          '1234 St',
        )
      })

      test('clicking previous with some missing answers shows error modal', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        // There is also a date question, and it's intentionally not answered
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickBack()

        // The date question is required, so expect the error modal.
        await applicantQuestions.expectErrorOnPreviousModal(
          /* northStarEnabled= */ true,
        )

        await validateAccessibility(page)
        await validateScreenshot(
          page,
          'northstar-error-on-previous-modal',
          /* fullPage= */ false,
        )
      })

      test('clicking previous with no answers does not show error modal', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        // If the applicant has never answered this block before and doesn't fill in any
        // answers now, we shouldn't show the error modal and should just go straight to
        // the previous page (which is the review page since this is the first block).
        // See issue #6987.
        await applicantQuestions.clickBack()

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })

      test('error on previous modal > click stay and fix > shows block', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        // await applicantQuestions.answerDateQuestion('')
        // Intentionally do NOT answer the date question
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickBack()
        await applicantQuestions.expectErrorOnPreviousModal(
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.clickStayAndFixAnswers(
          /* northStarEnabled= */ true,
        )

        // Verify the previously filled in answers are present
        // await applicantQuestions.checkDateQuestionValue('')
        await applicantQuestions.checkMemorableDateQuestionValue('', '', '')
        await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')

        // Answer the date question correctly and try clicking "Review" again
        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )

        await applicantQuestions.clickBack()

        // Verify we're taken to the previous page (which is the review page
        // since this was the first block) page and the answers were saved
        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )
        await applicantQuestions.answerEmailQuestion('')

        await applicantQuestions.clickBack()
        await applicantQuestions.expectErrorOnPreviousModal(
          /* northStarEnabled= */ true,
        )

        // Proceed to the previous page (which will be the review page,
        // since this is the first block), acknowledging that answers won't be saved
        await applicantQuestions.clickPreviousWithoutSaving(
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')
        await applicantQuestions.clickContinue()
        // Nothing to fill in since this is the static question block
        await applicantQuestions.clickContinue()

        // Only fill in half the address question, then try going to previous block
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          '',
          '',
          'WA',
          '',
        )
        await applicantQuestions.clickBack()
        await applicantQuestions.expectErrorOnPreviousModal(
          /* northStarEnabled= */ true,
        )

        // Proceed to the previous page and verify the first block answers are present
        await applicantQuestions.clickPreviousWithoutSaving(
          /* northStarEnabled= */ true,
        )
        // This is the static question block, so continue to the previous block
        await applicantQuestions.clickBack()

        await applicantQuestions.checkMemorableDateQuestionValue(
          '2021',
          '11',
          '1',
        )
        await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')
      })

      test('clicking previous after deleting answers to required questions shows error modal', async ({
        applicantQuestions,
      }) => {
        await test.step('answer questions on first block', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerMemorableDateQuestion(
            '2021',
            '11 - November',
            '1',
          )
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        })

        await test.step('delete answers on first block', async () => {
          await applicantQuestions.editBlock('Screen 1')
          await applicantQuestions.answerMemorableDateQuestion('', 'Select', '')
          await applicantQuestions.answerEmailQuestion('')
        })

        await test.step('previous button should show error modal', async () => {
          // Because the questions were previously answered and the date and email questions are required,
          // we don't let the user save the deletion of answers.
          await applicantQuestions.clickBack()
          await applicantQuestions.expectErrorOnPreviousModal(
            /* northStarEnabled= */ true,
          )
        })
      })

      test('previous saves blank optional answers', async ({
        applicantQuestions,
      }) => {
        await test.step('answer blocks with all required questions', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerMemorableDateQuestion(
            '2021',
            '11 - November',
            '1',
          )
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)

          await applicantQuestions.editBlock('Screen 3')
          await applicantQuestions.answerAddressQuestion(
            '1234 St',
            'Unit B',
            'Sim',
            'WA',
            '54321',
          )
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        })

        // We require that all questions be seen before an applicant can submit an application,
        // so open the optional question but don't fill in any answer, then use the "Previous" button.
        await test.step('open optional question block but do not answer', async () => {
          await applicantQuestions.editBlock('Screen 4')
          await applicantQuestions.clickBack()
        })

        await test.step('answer only required questions on block with both required and optional', async () => {
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
          // This block has a required phone question and optional currency question.
          // Only answer the required question, then use the "Previous" button.
          await applicantQuestions.editBlock('Screen 5')
          await applicantQuestions.answerPhoneQuestion('4256373270')
          await applicantQuestions.clickBack()
        })

        // Verify that the optional questions were marked as seen and we can now submit the application
        await test.step('can submit application', async () => {
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
          await applicantQuestions.submitFromReviewPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.expectConfirmationPage(
            /* northStarEnabled= */ true,
          )
        })
      })
    })

    test.describe('next button', () => {
      test('next block progression', async ({
        page,
        applicantQuestions,
        applicantProgramOverview,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantProgramOverview.startApplicationFromProgramOverviewPage(
          programName,
        )

        await validateAccessibility(page)

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

          // This step intentionally does NOT answer the qusetions on the page
          await applicantQuestions.clickContinue()

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
          await applicantQuestions.answerMemorableDateQuestion(
            '2021',
            '11 - November',
            '1',
          )
          await applicantQuestions.answerEmailQuestion('')
          await applicantQuestions.clickContinue()

          // Expect we're still on the same page and see errors since email wasn't answered
          await applicantQuestions.validateQuestionIsOnPage(dateQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
          await applicantQuestions.expectRequiredQuestionError(
            '.cf-question-email',
          )
        })

        await test.step('complete screen 1', async () => {
          await applicantQuestions.answerMemorableDateQuestion(
            '2021',
            '11 - November',
            '1',
          )
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickContinue()
        })

        await test.step('screen 2', async () => {
          await applicantQuestions.validateQuestionIsOnPage(staticQuestionText)
          await applicantQuestions.clickContinue()
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
          await applicantQuestions.clickContinue()
        })

        await test.step('screen 4', async () => {
          await applicantQuestions.validateQuestionIsOnPage(radioQuestionText)
          await applicantQuestions.answerRadioButtonQuestion('two')
          await applicantQuestions.clickContinue()
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
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectReviewPage(
            /* northStarEnabled= */ true,
          )
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
        })

        await test.step('can submit application', async () => {
          await applicantQuestions.submitFromReviewPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.expectConfirmationPage(
            /* northStarEnabled= */ true,
          )
        })
      })

      test('can skip optional questions', async ({applicantQuestions}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('screen 1', async () => {
          await applicantQuestions.answerMemorableDateQuestion(
            '2021',
            '11 - November',
            '1',
          )
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickContinue()
        })

        await test.step('screen 2', async () => {
          await applicantQuestions.validateQuestionIsOnPage(staticQuestionText)
          await applicantQuestions.clickContinue()
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
          await applicantQuestions.clickContinue()
        })

        await test.step('screen 4 (optional)', async () => {
          await applicantQuestions.validateQuestionIsOnPage(radioQuestionText)
          // Don't answer the optional radio question
          await applicantQuestions.clickContinue()
        })

        await test.step('screen 5 (currency optional)', async () => {
          await applicantQuestions.validateQuestionIsOnPage(phoneQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(
            currencyQuestionText,
          )

          await applicantQuestions.answerPhoneQuestion('4256373270')
          // Don't answer the optional currency question
          await applicantQuestions.clickContinue()
        })

        await test.step('can submit application', async () => {
          await applicantQuestions.expectReviewPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.submitFromReviewPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.expectConfirmationPage(
            /* northStarEnabled= */ true,
          )
        })
      })

      test('answering questions out of order', async ({
        applicantQuestions,
        applicantProgramOverview,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantProgramOverview.startApplicationFromProgramOverviewPage(
          programName,
        )
        await applicantQuestions.clickReview(/* northStarEnabled= */ true)

        await test.step('answer screen 4', async () => {
          await applicantQuestions.editBlock('Screen 4')

          await applicantQuestions.validateQuestionIsOnPage(radioQuestionText)
          await applicantQuestions.answerRadioButtonQuestion('one')
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        })

        await test.step('answer screen 1', async () => {
          await applicantQuestions.editBlock('Screen 1')
          await applicantQuestions.answerMemorableDateQuestion(
            '2021',
            '11 - November',
            '1',
          )
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        })

        // Re-answering questions by clicking "Edit" on the review page puts the application
        // in "review mode", which we'll test in the next step
        await test.step('re-answer screen 4', async () => {
          await applicantQuestions.editBlock('Screen 4')
          await applicantQuestions.validateQuestionIsOnPage(radioQuestionText)
          await applicantQuestions.answerRadioButtonQuestion('two')
        })

        // In review mode, you're only taken to blocks you haven't yet answered.
        // Screen 2 is just a static question which applicants don't need to answer,
        // so the first unanswered screen is screen 3.
        await test.step('next screen is screen 3', async () => {
          await applicantQuestions.clickContinue()
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
          await applicantQuestions.clickContinue()
          await applicantQuestions.validateQuestionIsOnPage(phoneQuestionText)
          await applicantQuestions.validateQuestionIsOnPage(
            currencyQuestionText,
          )
          await applicantQuestions.answerPhoneQuestion('4256373270')
        })

        // All the blocks are answered, so we should now be taken to the review page
        await test.step('next screen is review page', async () => {
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectReviewPage(
            /* northStarEnabled= */ true,
          )
        })

        await test.step('cannot delete answers to required questions', async () => {
          await applicantQuestions.editBlock('Screen 3')
          await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)
          await applicantQuestions.answerAddressQuestion('', '', '', '', '')
          await applicantQuestions.clickContinue()

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
          await applicantQuestions.clickContinue()
        })

        await test.step('can submit application', async () => {
          await applicantQuestions.expectReviewPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.submitFromReviewPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.expectConfirmationPage(
            /* northStarEnabled= */ true,
          )
        })
      })
    })

    test.describe('review button', () => {
      test('clicking review with correct form shows review page with saved answers', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickReview(/* northStarEnabled= */ true)

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        // Intentionally do NOT answer the date question
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickReview(/* northStarEnabled= */ true)

        // The date question is required, so expect the error modal.
        await applicantQuestions.expectErrorOnReviewModal(
          /* northStarEnabled= */ true,
        )
      })

      test('clicking review with no answers does not show error modal', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        // If the applicant has never answered this block before and doesn't fill in any
        // answers now, we shouldn't show the error modal and should just go straight to
        // the review page -- see issue #6987.
        await applicantQuestions.clickReview(/* northStarEnabled= */ true)

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })

      test('error on review modal > click stay and fix > shows block', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        // Intentionally do NOT answer the date question
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')

        await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        await applicantQuestions.expectErrorOnReviewModal(
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.clickStayAndFixAnswers(
          /* northStarEnabled= */ true,
        )

        // Verify the previously filled in answers are present
        await applicantQuestions.checkMemorableDateQuestionValue('', '', '')
        await applicantQuestions.checkEmailQuestionValue('test1@gmail.com')

        // Answer the date question correctly and try clicking "Review" again
        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )

        await applicantQuestions.clickReview(/* northStarEnabled= */ true)

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
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )
        await applicantQuestions.answerEmailQuestion('')

        await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        await applicantQuestions.expectErrorOnReviewModal(
          /* northStarEnabled= */ true,
        )

        // Proceed to the Review page, acknowledging that answers won't be saved
        await applicantQuestions.clickReviewWithoutSaving(
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerMemorableDateQuestion(
            '2021',
            '11 - November',
            '1',
          )
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        })

        await test.step('delete answers on first block', async () => {
          await applicantQuestions.editBlock('Screen 1')
          await applicantQuestions.answerMemorableDateQuestion('', '', '')
          await applicantQuestions.answerEmailQuestion('')
        })

        await test.step('review button should show error modal', async () => {
          // Because the questions were previously answered and the date and email questions are required,
          // we don't let the user save the deletion of answers.
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
          await applicantQuestions.expectErrorOnReviewModal(
            /* northStarEnabled= */ true,
          )
        })
      })

      test('review saves blank optional answers', async ({
        applicantQuestions,
      }) => {
        await test.step('answer blocks with all required questions', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerMemorableDateQuestion(
            '2021',
            '11 - November',
            '1',
          )
          await applicantQuestions.answerEmailQuestion('test1@gmail.com')
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)

          await applicantQuestions.editBlock('Screen 3')
          await applicantQuestions.answerAddressQuestion(
            '1234 St',
            'Unit B',
            'Sim',
            'WA',
            '54321',
          )
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        })

        // We require that all questions be seen before an applicant can submit an application,
        // so open the optional question but don't fill in any answer, then use the "Review" button.
        await test.step('open optional question block but do not answer', async () => {
          await applicantQuestions.editBlock('Screen 4')
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        })

        await test.step('answer only required questions on block with both required and optional', async () => {
          // This block has a required phone question and optional currency question.
          // Only answer the required question, then use the "Review" button.
          await applicantQuestions.editBlock('Screen 5')
          await applicantQuestions.answerPhoneQuestion('4256373270')
          await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        })

        // Verify that the optional questions were marked as seen and we can now submit the application
        await test.step('can submit application', async () => {
          await applicantQuestions.submitFromReviewPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.expectConfirmationPage(
            /* northStarEnabled= */ true,
          )
        })
      })
    })
  })

  test.describe('navigation with two blocks', () => {
    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(
        page,
        'suggest_programs_on_application_confirmation_page',
      )

      await test.step('Set up program with questions', async () => {
        await adminQuestions.addPhoneQuestion({
          questionName: 'nav-phone-q',
          questionText: phoneQuestionText,
        })
        await adminQuestions.addCurrencyQuestion({
          questionName: 'nav-currency-q',
          questionText: currencyQuestionText,
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockUsingSpec(programName, {
          name: 'Page A',
          description: 'Created first',
          questions: [{name: 'nav-phone-q', isOptional: false}],
        })
        await adminPrograms.addProgramBlockUsingSpec(programName, {
          name: 'Page B',
          description: 'Created second',
          questions: [{name: 'nav-currency-q', isOptional: false}],
        })

        // Move Page B to the first page in the application. Expect its block ID is 2.
        await page.locator('[data-test-id="move-block-up-2"]').click()

        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(programName)
        await logout(page)
      })
    })

    test('Applying to a program shows blocks in the admin-specified order', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Expect Page B as the first page', async () => {
        // Even though Page B was created second, it's the first page in the application
        await expect(page.getByText('1 of 3', {exact: true})).toBeVisible()
        await expect(page.getByText('Page B', {exact: true})).toBeVisible()
        await applicantQuestions.answerCurrencyQuestion('1.00')
        await applicantQuestions.clickContinue()
      })

      await test.step('Expect Page A as the second page', async () => {
        await expect(page.getByText('2 of 3', {exact: true})).toBeVisible()
        await expect(page.getByText('Page A', {exact: true})).toBeVisible()
        await applicantQuestions.answerPhoneQuestion('4254567890')
        await applicantQuestions.clickContinue()
      })

      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
    })

    test('Editing an in-progress application takes user to the next incomplete page', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Fill out page 1, then go to home page', async () => {
        await expect(page.getByText('1 of 3', {exact: true})).toBeVisible()
        await applicantQuestions.answerCurrencyQuestion('1.00')
        await applicantQuestions.clickContinue()
        await applicantQuestions.gotoApplicantHomePage()
      })

      await test.step('Edit application and expect page 2', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
          /* showProgramOverviewPage= */ false,
        )
        await expect(page.getByText('2 of 3', {exact: true})).toBeVisible()
      })
    })
  })
})
