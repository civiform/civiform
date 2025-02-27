import {test, expect} from './support/civiform_fixtures'
import {
  AdminQuestions,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {QuestionType} from './support/admin_questions'
import {BASE_URL} from './support/config'

test.describe('normal question lifecycle', () => {
  test('sample question seeding works', async ({
    page,
    adminQuestions,
    seeding,
  }) => {
    await seeding.seedQuestions()

    await page.goto(BASE_URL)
    await loginAsAdmin(page)

    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.expectDraftQuestionExist('Sample Address Question')
    await adminQuestions.expectDraftQuestionExist('Sample Number Question')
    await adminQuestions.expectDraftQuestionExist('Sample Name Question')
  })

  // Run create-update-publish test for each question type individually to keep
  // test duration reasonable.
  for (const type of Object.values(QuestionType)) {
    test(`${type} question: create, update, publish, create a new version, and update`, async ({
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      await loginAsAdmin(page)

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

  test('allows re-ordering options in dropdown question', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const options = [
      {adminName: 'option1_admin', text: 'option1'},
      {adminName: 'option2_admin', text: 'option2'},
      {adminName: 'option3_admin', text: 'option3'},
      {adminName: 'option4_admin', text: 'option4'},
    ]
    const questionName = 'adropdownquestion'
    await adminQuestions.createDropdownQuestion(
      {
        questionName: questionName,
        options,
      },
      /* clickSubmit= */ false,
    )

    const downButtons = await page
      .locator(
        '.cf-multi-option-question-option-editable:not(.hidden) > .multi-option-question-field-move-down-button',
      )
      .all()
    const upButtons = await page
      .locator(
        '.cf-multi-option-question-option-editable:not(.hidden) > .multi-option-question-field-move-up-button',
      )
      .all()
    expect(upButtons).toHaveLength(4)
    expect(downButtons).toHaveLength(4)

    await downButtons[3].click() // Should do nothing
    await waitForPageJsLoad(page)
    await upButtons[0].click() // Should do nothing
    await waitForPageJsLoad(page)

    await downButtons[0].click() // becomes 2, 1, 3, 4
    await waitForPageJsLoad(page)
    await downButtons[1].click() // becomes 2, 3, 1, 4
    await waitForPageJsLoad(page)
    await upButtons[1].click() // becomes 3, 2, 1, 4
    await waitForPageJsLoad(page)

    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(4, {
      adminName: 'option5_admin',
      text: 'option5',
    })
    const newUpButtons = await page
      .locator(
        '.cf-multi-option-question-option-editable:not(.hidden) > .multi-option-question-field-move-up-button',
      )
      .all()
    expect(newUpButtons).toHaveLength(5)
    await newUpButtons[4].click() // becomes 3, 2, 1, 5, 4

    await validateScreenshot(page, 'question-with-rearranged-options')

    await adminQuestions.clickSubmitButtonAndNavigate('Create')
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Validate that the options are in the correct order after saving.
    const optionText = await page
      .getByRole('textbox', {name: 'Option Text'})
      .all()
    expect(await optionText[0].inputValue()).toContain('option3')
    expect(await optionText[1].inputValue()).toContain('option2')
    expect(await optionText[2].inputValue()).toContain('option1')
    expect(await optionText[3].inputValue()).toContain('option5')
    expect(await optionText[4].inputValue()).toContain('option4')

    // Validate that the option admin names are in the correct order after saving.
    const adminNames = await page.getByRole('textbox', {name: 'Admin ID'}).all()
    expect(await adminNames[0].inputValue()).toContain('option3_admin')
    expect(await adminNames[1].inputValue()).toContain('option2_admin')
    expect(await adminNames[2].inputValue()).toContain('option1_admin')
    expect(await adminNames[3].inputValue()).toContain('option5_admin')
    expect(await adminNames[4].inputValue()).toContain('option4_admin')
  })

  test('shows markdown format correctly in the preview when creating a new question', async ({
    page,
    adminQuestions,
  }) => {
    const questionName = 'markdown formatted question'

    await loginAsAdmin(page)

    await adminQuestions.createCheckboxQuestion({
      questionName: questionName,
      questionText: 'https://google.com **bold**',
      helpText: '*italic* [link](https://test.com)',
      options: [
        {adminName: 'red_admin', text: 'red'},
        {adminName: 'green_admin', text: 'green'},
        {adminName: 'orange_admin', text: 'orange'},
        {adminName: 'blue_admin', text: 'blue'},
      ],
    })

    await adminQuestions.gotoQuestionEditPage(questionName)
    expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
      'https://google.com</a> <strong>bold</strong>',
    )
    expect(await page.innerHTML('.cf-applicant-question-help-text')).toContain(
      '<em>italic</em>',
    )
    await validateScreenshot(page, 'question-with-markdown-formatted-preview')
  })

  test('shows error when creating a dropdown question and admin left an option field blank', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const options = [
      {adminName: 'option1_admin', text: 'option1'},
      {adminName: 'option2_admin', text: 'option2'},
      {adminName: '', text: ''},
    ]

    await adminQuestions.createDropdownQuestion({
      questionName: 'dropdownWithEmptyOptions',
      options,
    })

    await validateScreenshot(page, 'question-with-blank-options-error')
    await adminQuestions.expectMultiOptionBlankOptionError(options, [2])
    await adminQuestions.expectMultiOptionInvalidOptionAdminError(options, [2])

    // Update empty option to have a value
    await adminQuestions.fillMultiOptionAnswer(2, {
      adminName: 'option3_admin',
      text: 'option3',
    })

    await adminQuestions.clickSubmitButtonAndNavigate('Create')

    await adminQuestions.expectAdminQuestionsPageWithCreateSuccessToast()
  })

  test('shows error when creating a radio question and admin left an option field blank', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const options = [
      {adminName: 'option1_admin', text: 'option1'},
      {adminName: 'option2_admin', text: 'option2'},
      {adminName: '', text: ''},
    ]

    await adminQuestions.createRadioButtonQuestion({
      questionName: 'radioButtonWithEmptyOptions',
      options,
    })

    await adminQuestions.expectMultiOptionBlankOptionError(options, [2])
    await adminQuestions.expectMultiOptionInvalidOptionAdminError(options, [2])

    // Update empty option to have a value
    await adminQuestions.fillMultiOptionAnswer(2, {
      adminName: 'option3_admin',
      text: 'option3',
    })

    await adminQuestions.clickSubmitButtonAndNavigate('Create')

    await adminQuestions.expectAdminQuestionsPageWithCreateSuccessToast()
  })

  test('shows error when updating a dropdown question and admin left an option field blank', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const options = [
      {adminName: 'option1_admin', text: 'option1'},
      {adminName: 'option2_admin', text: 'option2'},
    ]
    const questionName = 'updateEmptyDropdown'

    // Add a new valid dropdown question
    await adminQuestions.addDropdownQuestion({questionName, options})
    // Edit the newly created question
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Add an empty option
    await page.click('#add-new-option')
    // Add the empty option to the options array
    options.push({adminName: '', text: ''})
    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    await adminQuestions.expectMultiOptionBlankOptionError(options, [2])
    await adminQuestions.expectMultiOptionInvalidOptionAdminError(options, [2])
  })

  test('shows error when updating a radio question and admin left an option field blank', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const options = [
      {adminName: 'option1_admin', text: 'option1'},
      {adminName: 'option2_admin', text: 'option2'},
    ]
    const questionName = 'updateEmptyRadio'

    // Add a new valid radio question
    await adminQuestions.addRadioButtonQuestion({questionName, options})

    // Edit the newly created question
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Add an empty option
    await page.click('#add-new-option')
    // Add the empty option to the options array
    options.push({adminName: '', text: ''})

    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    await adminQuestions.expectMultiOptionBlankOptionError(options, [2])
    await adminQuestions.expectMultiOptionInvalidOptionAdminError(options, [2])
  })

  test('shows error when updating a radio question and admin left an adminName field blank', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const options = [
      {adminName: 'option1_admin', text: 'option1'},
      {adminName: 'option2_admin', text: 'option2'},
    ]
    const questionName = 'updateEmptyRadio'

    // Add a new valid radio question
    await adminQuestions.addRadioButtonQuestion({questionName, options})

    // Edit the newly created question
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Add an option with a missing adminName
    await page.click('#add-new-option')
    const newOption = {adminName: '', text: 'option3'}
    await adminQuestions.fillMultiOptionAnswer(2, newOption)
    // Add the empty option to the options array
    options.push(newOption)

    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    await validateScreenshot(page, 'question-with-blank-admin-names-error')
    await adminQuestions.expectMultiOptionBlankOptionError(options, [])
    await adminQuestions.expectMultiOptionInvalidOptionAdminError(options, [2])
  })

  test('shows error when updating a radio question and admin used invalid characters in the admin name', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const options = [
      {adminName: 'option1_admin', text: 'option1'},
      {adminName: 'option2_admin', text: 'option2'},
    ]
    const questionName = 'updateEmptyRadio'

    // Add a new valid radio question
    await adminQuestions.addRadioButtonQuestion({questionName, options})

    // Edit the newly created question
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Add options with an invalid adminNames
    await page.click('#add-new-option')
    const invalidOptionSpace = {adminName: 'invalid name', text: 'option3'}
    await adminQuestions.fillMultiOptionAnswer(2, invalidOptionSpace)

    await page.click('#add-new-option')
    const invalidOptionCapitalLetter = {
      adminName: 'invalid_Name',
      text: 'option4',
    }
    await adminQuestions.fillMultiOptionAnswer(3, invalidOptionCapitalLetter)

    // Add the invalid options to the options array
    options.push(invalidOptionSpace)
    options.push(invalidOptionCapitalLetter)

    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    await validateScreenshot(page, 'question-with-invalid-admin-names-error')
    await adminQuestions.expectMultiOptionBlankOptionError(options, [])
    await adminQuestions.expectMultiOptionInvalidOptionAdminError(
      options,
      [2, 3],
    )
  })

  test('persists export state', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)

    // Navigate to the new question page and ensure that "Don't allow answers to be exported"
    // is pre-selected.
    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.page.click('#create-question-button')
    await adminQuestions.page.click('#create-text-question')
    await waitForPageJsLoad(adminQuestions.page)

    await expect(
      page.locator(
        adminQuestions.selectorForExportOption(AdminQuestions.NO_EXPORT_OPTION),
      ),
    ).toBeChecked()

    const questionName = 'textQuestionWithObfuscatedExport'
    await adminQuestions.addTextQuestion({
      questionName,
      exportOption: AdminQuestions.EXPORT_OBFUSCATED_OPTION,
    })

    // Confirm that the previously selected export option was propagated.
    await adminQuestions.gotoQuestionEditPage(questionName)
    await expect(
      page.locator(
        adminQuestions.selectorForExportOption(
          AdminQuestions.EXPORT_OBFUSCATED_OPTION,
        ),
      ),
    ).toBeChecked()

    // Edit the result and confirm that the new value is propagated.
    await adminQuestions.selectExportOption(AdminQuestions.EXPORT_VALUE_OPTION)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    await adminQuestions.gotoQuestionEditPage(questionName)

    // Fix me! ESLint: playwright/prefer-web-first-assertions
    // Directly switching to the best practice method fails
    // because of a locator stict mode violation. That is it
    // returns multiple elements.
    //
    // Recommended prefer-web-first-assertions fix:
    // await expect(page.locator(adminQuestions.selectorForExportOption(AdminQuestions.EXPORT_VALUE_OPTION))).toBeChecked()
    expect(
      await page.isChecked(
        adminQuestions.selectorForExportOption(
          AdminQuestions.EXPORT_VALUE_OPTION,
        ),
      ),
    ).toBeTruthy()
  })

  test('shows the "Remove from universal questions" confirmation modal in the right circumstances and navigation works', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    const questionName = 'text question'
    await adminQuestions.addTextQuestion({questionName})
    await adminQuestions.gotoQuestionEditPage(questionName)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    // Since the flag is not enabled, the modal should not appear and you should be redirected to the admin questions page
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()

    await adminQuestions.gotoQuestionEditPage(questionName)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    // Since we are going from "off" to "on", the modal should not appear and you should be redirected to the admin questions page
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()

    await adminQuestions.gotoQuestionEditPage(questionName)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    // Flag is on and we are going from "on" to "off" so the modal should show
    await validateScreenshot(page, 'remove-universal-confirmation-modal')

    // Clicking "Cancel" on the modal closes the modal and returns you to the edit page
    await adminQuestions.clickSubmitButtonAndNavigate('Cancel')
    await adminQuestions.expectQuestionEditPage(questionName)

    // Clicking "Remove from universal questions" submits the form and redirects you to the admin questions page
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.clickSubmitButtonAndNavigate(
      'Remove from universal questions',
    )
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
  })

  test('redirects to draft question when trying to edit original question', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
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
    const newQuestionText =
      await adminQuestions.updateQuestionText('second version')
    await adminQuestions.clickSubmitButtonAndNavigate('Update')

    // Try edit the original published question and make sure that we see the draft version.
    await page.goto(editUrl)
    await waitForPageJsLoad(page)
    expect(await page.inputValue('label:has-text("Question text")')).toContain(
      newQuestionText,
    )
  })

  test('shows preview of formatted question name when creating a new question', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.gotoAdminQuestionsPage()
    await page.click('#create-question-button')
    await page.click('#create-name-question')
    await waitForPageJsLoad(page)
    await page.fill(
      'label:has-text("Administrative identifier")',
      'My Test Question14-0',
    )
    expect(await page.locator('#question-name-preview').innerText()).toContain(
      'Visible in the API as:',
    )

    // Wait for debounce
    await page.waitForTimeout(300) // ms

    await expect(page.locator('#formatted-name')).toHaveText('my_test_question')
  })
})
