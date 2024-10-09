import {test} from '../../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('Applicant navigation flow', {tag: ['@northstar']}, () => {
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

      await enableFeatureFlag(page, 'north_star_applicant_ui')
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
    })
  })
})
