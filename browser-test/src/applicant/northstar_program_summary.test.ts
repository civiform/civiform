import {expect, test} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
} from '../support'

test.describe('Applicant navigation flow', {tag: ['@northstar']}, () => {
  test.describe('navigation with five blocks', () => {
    const programName = 'Test program for summary page'
    const programDescription = 'Test description'
    const dateQuestionText = 'date question text'
    const emailQuestionText = 'email question text'
    const staticQuestionText = 'static question text'
    const addressQuestionText = 'address question text'
    const radioQuestionText = 'radio question text'
    const phoneQuestionText = 'phone question text'
    const currencyQuestionText = 'currency question text'

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
      await adminQuestions.addPhoneQuestion({
        questionName: 'nav-phone-q',
        questionText: phoneQuestionText,
      })
      await adminQuestions.addCurrencyQuestion({
        questionName: 'nav-currency-q',
        questionText: currencyQuestionText,
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

    test('Verify program summary page', async ({page, applicantQuestions}) => {
      await test.step('Apply to program', async () => {
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
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
        )
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerRadioButtonQuestion('one')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerPhoneQuestion('4256373270')
        await applicantQuestions.clickContinue()
      })

      await test.step('Verify program summary page', async () => {
        await applicantQuestions.expectTitle(
          page,
          'Program application summary — Test program for summary page',
        )
        await expect(page.getByText(programName)).toBeVisible()
        await expect(page.getByText(programDescription)).toBeVisible()

        await validateAccessibility(page)
      })
    })
  })

  test('Click to download file', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    const programName = 'Test program for single file upload'
    const fileUploadQuestionText = 'Required file upload question'
    const fileName = 'foo.txt'
    const fileContent = 'some sample text'

    // TODO(#8143): File uploads in North Star tests are blocked by CSP errors. After those
    // errors are fixed, this entire test can run with north_star_applicant_ui enabled
    await disableFeatureFlag(page, 'north_star_applicant_ui')

    await test.step('As admin, set up program', async () => {
      await loginAsAdmin(page)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-q',
        questionText: fileUploadQuestionText,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['file-upload-test-q'],
        programName,
      )

      await logout(page)
    })

    await test.step('Upload file', async () => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(fileContent, fileName)
      await applicantQuestions.clickNext()
    })

    await test.step('Download file in North Star', async () => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await expect(page.getByText(fileName)).toBeVisible()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage(
          /* northStarEnabled= */ true,
        )
      expect(downloadedFileContent).toEqual(fileContent)
    })
  })
})
