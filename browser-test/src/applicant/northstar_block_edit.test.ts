import {test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
} from '../support'

test.describe('Applicant block edit', {tag: ['@northstar']}, () => {
  const programName = 'Test program for block edit page'
  const programDescription = 'Test description'
  const dateQuestionText = 'date question text'
  const emailQuestionText = 'email question text'
  const staticQuestionText = 'static question text'
  const addressQuestionText = 'address question text'
  const radioQuestionText = 'radio question text'

  test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await loginAsAdmin(page)

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

    await adminPrograms.addProgram(programName, programDescription)
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

    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.publishProgram(programName)
    await logout(page)
  })

  test('validate block edit page title', async ({page, applicantQuestions}) => {
    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.expectTitle(
      page,
      'Test program for block edit page — 1 of 4',
    )

    await validateAccessibility(page)
  })
})
