import {
  AdminQuestions,
  createTestContext,
  disableFeatureFlag,
  enableFeatureFlag,
  dropTables,
  loginAsAdmin,
  seedCanonicalQuestions,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {QuestionType} from './support/admin_questions'
import {BASE_URL} from './support/config'

describe('normal question lifecycle', () => {
  const ctx = createTestContext()

  it('canonical question seeding works', async () => {
    const {page, adminQuestions} = ctx
    await dropTables(page)
    await seedCanonicalQuestions(page)

    await page.goto(BASE_URL)
    await loginAsAdmin(page)

    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.expectDraftQuestionExist('Name')
    await adminQuestions.expectDraftQuestionExist('Applicant Date of Birth')
  })

  // Run create-update-publish test for each question type individually to keep
  // test duration reasonable.
  for (const type of Object.values(QuestionType)) {
    // TODO(#4125) Each of these test cases is a duplicate of a test case in the loop further down and should be removed once the program_read_only_view
    // flag has been removed.
    it(`${type} question: create, update, publish, create a new version, and update`, async () => {
      const {page, adminQuestions, adminPrograms} = ctx

      await loginAsAdmin(page)
      await disableFeatureFlag(page, 'program_read_only_view_enabled')

      const questionName = `qlc-${type}`
      // for most question types there will be only 1 question. But for
      // enumerator question we'll create repeated question later.
      const allQuestions = [questionName]

      await adminQuestions.addQuestionForType(type, questionName)
      const repeatedQuestion = 'qlc-repeated-number'
      const isEnumerator = type === QuestionType.ENUMERATOR
      if (isEnumerator) {
        // Add to the front of the list because creating a new version of the enumerator question will
        // automatically create a new version of the repeated question. This is important for
        // createNewVersionForQuestions() call below.
        allQuestions.unshift(repeatedQuestion)
        await adminQuestions.addNumberQuestion({
          questionName: repeatedQuestion,
          description: 'description',
          questionText: "$this's favorite number",
          helpText: '',
          enumeratorName: questionName,
        })
        await adminQuestions.updateQuestion(repeatedQuestion)
      }

      await adminQuestions.gotoQuestionEditPage(questionName)
      await validateScreenshot(page, `${type}-edit-page`)
      await adminQuestions.updateQuestion(questionName)

      const programName = `program-for-${type}-question-lifecycle`
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(
        programName,
        'qlc program description',
        [questionName],
      )
      if (isEnumerator) {
        await adminPrograms.addProgramRepeatedBlock(
          programName,
          'Screen 1',
          'repeated block desc',
          [repeatedQuestion],
        )
      }
      await adminPrograms.publishProgram(programName)

      await adminQuestions.expectActiveQuestions(allQuestions)

      // Take screenshot of questions being published and active.
      await adminQuestions.gotoAdminQuestionsPage()
      await validateScreenshot(page, `${type}-only-active`)

      await adminQuestions.createNewVersionForQuestions(allQuestions)

      await adminQuestions.updateAllQuestions(allQuestions)

      // Take screenshot of question being in draft state.
      await adminQuestions.gotoAdminQuestionsPage()
      await validateScreenshot(page, `${type}-active-and-draft`)

      await adminPrograms.publishProgram(programName)

      await adminPrograms.createNewVersion(
        programName,
        /* programReadOnlyViewEnabled = */ false,
      )

      await adminPrograms.publishProgram(programName)

      await adminQuestions.expectActiveQuestions(allQuestions)
    })
  }

  for (const type of Object.values(QuestionType)) {
    it(`${type} question: create, update, publish, create a new version, and update`, async () => {
      const {page, adminQuestions, adminPrograms} = ctx

      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_read_only_view_enabled')

      const questionName = `qlc-${type}`
      // for most question types there will be only 1 question. But for
      // enumerator question we'll create repeated question later.
      const allQuestions = [questionName]

      await adminQuestions.addQuestionForType(type, questionName)
      const repeatedQuestion = 'qlc-repeated-number'
      const isEnumerator = type === QuestionType.ENUMERATOR
      if (isEnumerator) {
        // Add to the front of the list because creating a new version of the enumerator question will
        // automatically create a new version of the repeated question. This is important for
        // createNewVersionForQuestions() call below.
        allQuestions.unshift(repeatedQuestion)
        await adminQuestions.addNumberQuestion({
          questionName: repeatedQuestion,
          description: 'description',
          questionText: "$this's favorite number",
          helpText: '',
          enumeratorName: questionName,
        })
        await adminQuestions.updateQuestion(repeatedQuestion)
      }

      await adminQuestions.gotoQuestionEditPage(questionName)
      await validateScreenshot(page, `${type}-edit-page`)
      await adminQuestions.updateQuestion(questionName)

      const programName = `program-for-${type}-question-lifecycle`
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(
        programName,
        'qlc program description',
        [questionName],
      )
      if (isEnumerator) {
        await adminPrograms.addProgramRepeatedBlock(
          programName,
          'Screen 1',
          'repeated block desc',
          [repeatedQuestion],
        )
      }
      await adminPrograms.publishProgram(programName)

      await adminQuestions.expectActiveQuestions(allQuestions)

      // Take screenshot of questions being published and active.
      await adminQuestions.gotoAdminQuestionsPage()
      await validateScreenshot(page, `${type}-only-active`)

      await adminQuestions.createNewVersionForQuestions(allQuestions)

      await adminQuestions.updateAllQuestions(allQuestions)

      // Take screenshot of question being in draft state.
      await adminQuestions.gotoAdminQuestionsPage()
      await validateScreenshot(page, `${type}-active-and-draft`)

      await adminPrograms.publishProgram(programName)

      await adminPrograms.createNewVersion(programName)

      await adminPrograms.publishProgram(programName)

      await adminQuestions.expectActiveQuestions(allQuestions)
    })
  }

  it('shows error when creating a dropdown question and admin left an option field blank', async () => {
    const {page, adminQuestions} = ctx

    await loginAsAdmin(page)

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
    const {page, adminQuestions} = ctx

    await loginAsAdmin(page)

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
    const {page, adminQuestions} = ctx

    await loginAsAdmin(page)

    const options = ['option1', 'option2']
    const questionName = 'updateEmptyDropdown'

    // Add a new valid dropdown question
    await adminQuestions.addDropdownQuestion({questionName, options})
    // Edit the newly created question
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Add an empty option
    await page.click('#add-new-option')
    // Add the empty option to the options array
    options.push('')
    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    await adminQuestions.expectMultiOptionBlankOptionError(options)
  })

  it('shows error when updating a radio question and admin left an option field blank', async () => {
    const {page, adminQuestions} = ctx

    await loginAsAdmin(page)

    const options = ['option1', 'option2']
    const questionName = 'updateEmptyRadio'

    // Add a new valid radio question
    await adminQuestions.addRadioButtonQuestion({questionName, options})

    // Edit the newly created question
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Add an empty option
    await page.click('#add-new-option')
    // Add the empty option to the options array
    options.push('')

    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    await adminQuestions.expectMultiOptionBlankOptionError(options)
  })

  it('persists export state', async () => {
    const {page, adminQuestions} = ctx

    await loginAsAdmin(page)

    // Navigate to the new question page and ensure that "Don't allow answers to be exported"
    // is pre-selected.
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
    const {page, adminQuestions, adminPrograms} = ctx
    await loginAsAdmin(page)

    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.addNameQuestion({questionName: 'name-q'})

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishProgram(programName)

    // Update the question to create new draft version.
    await adminQuestions.gotoQuestionEditPage('name-q')
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
