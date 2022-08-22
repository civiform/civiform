import {
  startSession,
  loginAsAdmin,
  AdminQuestions,
  AdminPrograms,
  endSession,
  dropTables,
  seedCanonicalQuestions,
  waitForPageJsLoad,
} from './support'

describe('normal question lifecycle', () => {
  beforeAll(async () => {
    const {page} = await startSession()
    await dropTables(page)
    await seedCanonicalQuestions(page)
  })

  it('has canonical questions available by default', async () => {
    const {browser, page} = await startSession()
    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.expectDraftQuestionExist('Name')
    await adminQuestions.expectDraftQuestionExist('Applicant Date of Birth')

    await endSession(browser)
  })

  it('create, update, publish, create a new version, and update all questions', async () => {
    const {browser, page} = await startSession()
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    const questions = await adminQuestions.addAllNonSingleBlockQuestionTypes(
      'qlc-',
    )
    const singleBlockQuestions =
      await adminQuestions.addAllSingleBlockQuestionTypes('qlc-')
    const repeatedQuestion = 'qlc-repeated-number'
    await adminQuestions.addNumberQuestion({
      questionName: repeatedQuestion,
      description: 'description',
      questionText: "$this's favorite number",
      helpText: '',
      enumeratorName: 'qlc-enumerator',
    })

    // Combine all the questions that were made so we can update them all together.
    const allQuestions = questions.concat(singleBlockQuestions)
    // Add to the front of the list because creating a new version of the enumerator question will
    // automatically create a new version of the repeated question.
    allQuestions.unshift(repeatedQuestion)

    await adminQuestions.updateAllQuestions(allQuestions)

    const programName = 'program for question lifecycle'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(
      programName,
      'qlc program description',
      questions,
    )
    for (const singleBlockQuestion of singleBlockQuestions) {
      const blockName = await adminPrograms.addProgramBlock(
        programName,
        'single-block question',
        [singleBlockQuestion],
      )
      if (singleBlockQuestion == 'qlc-enumerator') {
        await adminPrograms.addProgramRepeatedBlock(
          programName,
          blockName,
          'repeated block desc',
          [repeatedQuestion],
        )
      }
    }
    await adminPrograms.publishProgram(programName)

    await adminQuestions.expectActiveQuestions(allQuestions)

    await adminQuestions.createNewVersionForQuestions(allQuestions)

    await adminQuestions.updateAllQuestions(allQuestions)

    await adminPrograms.publishProgram(programName)

    await adminPrograms.createNewVersion(programName)

    await adminPrograms.publishProgram(programName)

    await adminQuestions.expectActiveQuestions(allQuestions)

    await endSession(browser)
  })

  it('shows error when creating a dropdown question and admin left an option field blank', async () => {
    const {page} = await startSession()
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    const options = ['option1', 'option2', '']

    await adminQuestions.createDropdownQuestion({
      questionName: 'dropdownWithEmptyOptions',
      options,
    })

    await adminQuestions.expectMultiOptionBlankOptionError(options)

    // Update empty option to have a value
    await adminQuestions.changeMultiOptionAnswer(3, 'option3')

    await adminQuestions.clickSubmitButtonAndNavigate('Create')

    await adminQuestions.expectAdminQuestionsPageWithCreateSuccessToast()
  })

  it('shows error when creating a radio question and admin left an option field blank', async () => {
    const {page} = await startSession()
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    const options = ['option1', 'option2', '']

    await adminQuestions.createRadioButtonQuestion({
      questionName: 'radioButtonWithEmptyOptions',
      options,
    })

    await adminQuestions.expectMultiOptionBlankOptionError(options)

    // Update empty option to have a value
    await adminQuestions.changeMultiOptionAnswer(3, 'option3')

    await adminQuestions.clickSubmitButtonAndNavigate('Create')

    await adminQuestions.expectAdminQuestionsPageWithCreateSuccessToast()
  })

  it('shows error when updating a dropdown question and admin left an option field blank', async () => {
    const {page} = await startSession()
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    const options = ['option1', 'option2']
    const questionName = 'updateEmptyDropdown'

    // Add a new valid dropdown question
    await adminQuestions.addDropdownQuestion({questionName, options})
    // Edit the newly created question
    await page.click(
      adminQuestions.selectWithinQuestionTableRow(
        questionName,
        ':text("Edit")',
      ),
    )

    // Add an empty option
    await page.click('#add-new-option')
    // Add the empty option to the options array
    options.push('')
    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    await adminQuestions.expectMultiOptionBlankOptionError(options)
  })

  it('shows error when updating a radio question and admin left an option field blank', async () => {
    const {page} = await startSession()
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    const options = ['option1', 'option2']
    const questionName = 'updateEmptyRadio'

    // Add a new valid radio question
    await adminQuestions.addRadioButtonQuestion({questionName, options})

    // Edit the newly created question
    await page.click(
      adminQuestions.selectWithinQuestionTableRow(
        questionName,
        ':text("Edit")',
      ),
    )

    // Add an empty option
    await page.click('#add-new-option')
    // Add the empty option to the options array
    options.push('')

    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    await adminQuestions.expectMultiOptionBlankOptionError(options)
  })

  it('persists export state', async () => {
    const {page} = await startSession()
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    // Navigate to the new question page and ensure that "No export" is pre-selected.
    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.page.click('#create-question-button')
    await adminQuestions.page.click('#create-text-question')
    await waitForPageJsLoad(adminQuestions.page)
    expect(
      await page.isChecked(
        adminQuestions.selectorForExportOption(AdminQuestions.NO_EXPORT_OPTION),
      ),
    ).toBeTruthy()

    const questionName = 'textQuestionWithObfuscatedExport'
    await adminQuestions.addTextQuestion({
      questionName,
      exportOption: AdminQuestions.EXPORT_OBFUSCATED_OPTION,
    })

    // Confirm that the previously selected export option was propagated.
    await adminQuestions.gotoQuestionEditPage(questionName)
    expect(
      await page.isChecked(
        adminQuestions.selectorForExportOption(
          AdminQuestions.EXPORT_OBFUSCATED_OPTION,
        ),
      ),
    ).toBeTruthy()

    // Edit the result and confirm that the new value is propagated.
    await adminQuestions.selectExportOption(AdminQuestions.EXPORT_VALUE_OPTION)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    await adminQuestions.gotoQuestionEditPage(questionName)
    expect(
      await page.isChecked(
        adminQuestions.selectorForExportOption(
          AdminQuestions.EXPORT_VALUE_OPTION,
        ),
      ),
    ).toBeTruthy()
  })

  it('redirects to draft question when trying to edit original question', async () => {
    const {page} = await startSession()
    await loginAsAdmin(page)

    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.addNameQuestion({questionName: 'name-q'})

    const programName = 'test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishProgram(programName)

    // Update the question to create new draft version.
    await adminQuestions.gotoQuestionNewVersionPage('name-q')
    // The ID in the URL after clicking new version corresponds to the active question form (e.g. ID=15).
    // After a draft is created, the ID will reflect the newly created draft version (e.g. ID=16).
    const editUrl = page.url()
    const newQuestionText = await adminQuestions.updateQuestionText(
      'second version',
    )
    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    // Try edit the original published question and make sure that we see the draft version.
    await page.goto(editUrl)
    await waitForPageJsLoad(page)
    expect(await page.inputValue('label:has-text("Question text")')).toContain(
      newQuestionText,
    )
  })
})
