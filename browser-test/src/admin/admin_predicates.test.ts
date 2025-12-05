import {expect, test} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
} from '../support'

test.describe('create and edit predicates', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(page, 'expanded_form_logic_enabled')
  })

  test('add a hide predicate', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)

    // Add a program with two screens
    const hideQuestionName = 'hide-predicate-q'
    await adminQuestions.addTextQuestion({questionName: hideQuestionName})
    await adminQuestions.addTextQuestion({
      questionName: 'hide-other-q',
      description: 'desc',
      questionText: 'conditional question',
    })

    const programName = 'Create hide predicate'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      description: 'first screen',
      questions: [{name: hideQuestionName}],
    })
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      name: 'Screen 2',
      description: 'screen with predicate',
      questions: [{name: 'hide-other-q'}],
    })

    // Validate empty state without predicate
    await adminPrograms.goToBlockInProgram(programName, 'Screen 2')
    expect(await page.innerText('#visibility-predicate')).toContain(
      'This screen is always shown',
    )

    // Edit predicate for second block
    await adminPrograms.goToEditBlockVisibilityPredicatePage(
      programName,
      'Screen 2',
    )

    await adminPredicates.clickAddConditionButton()
    await validateToastMessage(page, 'Please select a question')

    await adminPredicates.addPredicates({
      questionName: hideQuestionName,
      action: 'hidden if',
      scalar: 'text',
      operator: 'is equal to',
      value: 'hide me',
    })
    await adminPredicates.expectPredicateDisplayTextContains(
      'Screen 2 is hidden if "hide-predicate-q" text is equal to "hide me"',
    )
    await validateScreenshot(page, 'hide-predicate')

    // Verify block with predicate display
    await adminPrograms.goToBlockInProgram(programName, 'Screen 2')
    await validateScreenshot(
      page.locator('#visibility-predicate'),
      'block-hide-predicate-collapsed',
    )
    await adminPredicates.expandPredicateDisplay('visibility')
    await validateScreenshot(
      page.locator('#visibility-predicate'),
      'block-hide-predicate-expanded',
    )

    // Visit block with question in predicate
    await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
    // Verify question visibility accordion is shown
    await adminPrograms.expectQuestionCardWithLabel(
      hideQuestionName,
      'This question shows or hides screens',
    )
    const visibilityContentId = hideQuestionName + '-visibility-content'
    await expect(page.locator('#' + visibilityContentId)).toBeHidden()
    await validateScreenshot(
      page.locator('#' + hideQuestionName + '-visibility-accordion'),
      'question-card-with-hide-predicate-collapsed',
    )
    // Expand accordion and verify it displays the block containing the predicate
    await page
      .locator('button[aria-controls="' + visibilityContentId + '"]')
      .click()
    await expect(page.locator('#' + visibilityContentId)).toBeVisible()
    await expect(page.locator('#' + visibilityContentId)).toContainText(
      'Screen 2',
    )
    await validateScreenshot(page, 'question-card-with-hide-predicate')

    // Publish the program
    await adminPrograms.publishProgram(programName)

    // Switch to the applicantQuestions.view and apply to the program
    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName, true)

    // Initially fill out the first screen so that the next screen will be shown
    await applicantQuestions.answerTextQuestion('show me')
    await applicantQuestions.clickContinue()

    // Fill out the second screen
    await applicantQuestions.answerTextQuestion(
      'will be hidden and not submitted',
    )
    await applicantQuestions.clickContinue()

    // We should be on the review page, with an answer to Screen 2's question
    await applicantQuestions.expectQuestionExistsOnReviewPage(
      'conditional question',
    )

    // Return to the first screen and answer it so that the second screen is hidden
    await page.click('text=Edit') // first screen edit
    await applicantQuestions.answerTextQuestion('hide me')
    await applicantQuestions.clickContinue()

    // We should be on the review page
    await applicantQuestions.expectQuestionDoesNotExistOnReviewPage(
      'conditional question',
    )
    await applicantQuestions.submitFromReviewPage(true)

    // Visit the program admin page and assert the hidden question does not show
    await logout(page)
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())

    expect(await page.innerHTML('#application-view')).not.toContain('Screen 2')

    await page.getByRole('link', {name: 'Back'}).click()
  })

  test('add a show predicate', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)

    // Add a program with two screens
    const showQuestionName = 'show-predicate-q'
    await adminQuestions.addTextQuestion({
      questionName: showQuestionName,
      description: 'desc',
      questionText: 'text [markdown](example.com) *question*',
      helpText: '**bolded**',
      markdown: true,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'show-other-q',
      description: 'desc',
      questionText: 'conditional question',
    })

    const programName = 'Create show predicate'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      description: 'first screen',
      questions: [{name: showQuestionName}],
    })
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      name: 'Screen 2',
      description: 'screen with predicate',
      questions: [{name: 'show-other-q'}],
    })

    // Validate empty state without predicate
    await adminPrograms.goToBlockInProgram(programName, 'Screen 2')
    expect(await page.innerText('#visibility-predicate')).toContain(
      'This screen is always shown',
    )

    // Edit predicate for second screen
    await adminPrograms.goToEditBlockVisibilityPredicatePage(
      programName,
      'Screen 2',
    )
    await adminPredicates.addPredicates({
      questionName: showQuestionName,
      action: 'shown if',
      scalar: 'text',
      operator: 'is equal to',
      value: 'show me',
    })
    await adminPredicates.expectPredicateDisplayTextContains(
      'Screen 2 is shown if "show-predicate-q" text is equal to "show me"',
    )
    await validateScreenshot(page, 'show-predicate')

    // Verify block with predicate display
    await adminPrograms.goToBlockInProgram(programName, 'Screen 2')
    await validateScreenshot(
      page.locator('#visibility-predicate'),
      'block-show-predicate-collapsed',
    )
    await adminPredicates.expandPredicateDisplay('visibility')
    await validateScreenshot(
      page.locator('#visibility-predicate'),
      'block-show-predicate-expanded',
    )

    // Visit block with question in predicate
    await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
    // Verify question visibility accordion is shown
    await adminPrograms.expectQuestionCardWithLabel(
      showQuestionName,
      'This question shows or hides screens',
    )
    const visibilityContentId = showQuestionName + '-visibility-content'
    await expect(page.locator('#' + visibilityContentId)).toBeHidden()
    // Expand accordion and verify it displays the block containing the predicate
    await page
      .locator('button[aria-controls="' + visibilityContentId + '"]')
      .click()
    await expect(page.locator('#' + visibilityContentId)).toBeVisible()
    await expect(page.locator('#' + visibilityContentId)).toContainText(
      'Screen 2',
    )

    // Publish the program
    await adminPrograms.publishProgram(programName)

    // Switch to the applicantQuestions.view and apply to the program
    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName, true)

    // Initially fill out the first screen so that the next screen will be hidden
    await applicantQuestions.answerTextQuestion('hide next screen')
    await applicantQuestions.clickContinue()

    // We should be on the review page, with no Screen 2 questions shown. We should
    // be able to submit the application
    await applicantQuestions.expectQuestionDoesNotExistOnReviewPage(
      'conditional question',
    )
    await expect(
      page.getByRole('button', {name: 'Submit application'}),
    ).toBeVisible()

    // Return to the first screen and answer it so that the second screen is shown
    await page.click('text=Edit') // first screen edit
    await applicantQuestions.answerTextQuestion('show me')
    await applicantQuestions.clickContinue()

    // The second screen should now appear, and we must fill it out
    await applicantQuestions.answerTextQuestion('hello world!')
    await applicantQuestions.clickContinue()
    await validateScreenshot(page, 'program-summary-page')

    // We should be on the review page
    await applicantQuestions.expectQuestionExistsOnReviewPage(
      'conditional question',
    )
    await applicantQuestions.submitFromReviewPage(true)

    // Visit the program admin page and assert the conditional question is shown
    await logout(page)
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)

    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    expect(await page.locator('#application-view').innerText()).toContain(
      'Screen 2',
    )

    await page.getByRole('link', {name: 'Back'}).click()
  })

  test('add an eligibility predicate', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    // Add a program with two screens
    await adminQuestions.addTextQuestion({
      questionName: 'eligibility-predicate-q',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'eligibility-other-q',
      description: 'desc',
      questionText: 'eligibility question',
    })

    const programName = 'Create eligibility predicate'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      description: 'first screen',
      questions: [{name: 'eligibility-predicate-q'}],
    })

    // Validate empty state without predicate
    await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
    expect(await page.innerText('#eligibility-predicate')).toContain(
      'This screen does not have any eligibility conditions',
    )

    // Edit predicate for second screen
    await adminPrograms.goToEditBlockEligibilityPredicatePage(
      programName,
      'Screen 1',
    )

    // Add predicate with missing operator.
    await adminPredicates.addPredicates({
      questionName: 'eligibility-predicate-q',
      scalar: 'text',
      operator: '',
      value: 'eligible',
    })
    await adminPredicates.expectPredicateErrorToast('dropdowns')

    await adminPredicates.configurePredicate({
      questionName: 'eligibility-predicate-q',
      scalar: 'text',
      operator: 'is equal to',
      value: 'eligible',
    })
    await adminPredicates.clickSaveConditionButton()
    await adminPredicates.clickRemovePredicateButton('eligibility')

    // Add predicate with missing value.
    await adminPredicates.addPredicates({
      questionName: 'eligibility-predicate-q',
      scalar: 'text',
      operator: 'is equal to',
      value: '',
    })
    await adminPredicates.expectPredicateErrorToast('form fields')
    await validateScreenshot(page, 'predicate-error')

    await adminPredicates.configurePredicate({
      questionName: 'eligibility-predicate-q',
      scalar: 'text',
      operator: 'is equal to',
      value: 'eligible',
    })
    await adminPredicates.clickSaveConditionButton()
    await adminPredicates.clickRemovePredicateButton('eligibility')

    // Add predicate with missing operator and value.
    await adminPredicates.addPredicates({
      questionName: 'eligibility-predicate-q',
      scalar: 'text',
      operator: '',
      value: '',
    })
    await adminPredicates.clickSaveConditionButton()
    await adminPredicates.expectPredicateErrorToast('form fields or dropdowns')

    await adminPredicates.configurePredicate({
      questionName: 'eligibility-predicate-q',
      scalar: 'text',
      operator: 'is equal to',
      value: 'eligible',
    })
    await adminPredicates.clickSaveConditionButton()

    await adminPredicates.expectPredicateDisplayTextContains(
      'Applicant is eligible if "eligibility-predicate-q" text is equal to "eligible"',
    )
    await validateScreenshot(page, 'eligibility-predicate')

    await page.click(`a:has-text("Back")`)

    // Verify block with predicate display
    await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
    await adminPredicates.expandPredicateDisplay('eligibility')
    await validateScreenshot(
      page.locator('#eligibility-predicate'),
      'block-eligibility-predicate',
    )

    // Publish the program
    await adminPrograms.publishProgram(programName)

    // Switch to the applicantQuestions.view and apply to the program
    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName, true)

    // Initially fill out the first screen so that it is ineligible
    await applicantQuestions.answerTextQuestion('ineligble')
    await applicantQuestions.clickContinue()
    await applicantQuestions.expectIneligiblePage(true)
    await validateScreenshot(page, 'ineligible')

    // Verify that the program details link goes to the program overview page
    const programDetailsURL = page.getByRole('link', {name: 'program details'})

    await expect(programDetailsURL).toHaveAttribute(
      'href',
      '/programs/create-eligibility-predicate',
    )

    // Return to the screen and fill it out to be eligible.
    await page.goBack()
    await applicantQuestions.answerTextQuestion('eligible')
    await applicantQuestions.clickContinue()

    // We should be on the review page, and able to submit the application
    await expect(
      page.getByRole('button', {name: 'Submit application'}),
    ).toBeVisible()
    await applicantQuestions.submitFromReviewPage(true)

    // Visit the program admin page and assert the question is shown
    await logout(page)
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)

    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    expect(await page.locator('#application-view').innerText()).toContain(
      'Screen 1',
    )

    await page.getByRole('link', {name: 'Back'}).click()
  })

  test('suffix cannot be added as an eligibility predicate for name question', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'name_suffix_dropdown_enabled')

    const programName =
      'Test name question as a eligibility condition excluding suffix'
    const questionName = 'name-question'
    const screenName = 'Screen 1'

    await test.step('adds name question as an eligibility condition', async () => {
      await adminQuestions.addNameQuestion({questionName: questionName})
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: screenName,
        questions: [{name: questionName}],
      })
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        screenName,
      )
      await adminPredicates.selectQuestionForPredicate(questionName)
      await adminPredicates.clickAddConditionButton()
      await adminPredicates.addValueRows(1)
    })

    await test.step('name suffix is not visible to be selected as a value', async () => {
      const dropdown = page.locator('.cf-scalar-select').getByRole('combobox')
      await expect(dropdown.locator('option')).toHaveText([
        '', // This accounts for the hidden, empty placeholder option
        'first name',
        'middle name',
        'last name',
      ])
    })
  })

  test('eligibility message field is available to use', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Test eligibility message field availbale to use'
    const firstScreen = 'Screen 1'
    const secondScreen = 'Screen 2'

    await test.step('Adds a program with two screens', async () => {
      await adminQuestions.addTextQuestion({
        questionName: 'show-predicate-q',
        description: 'desc',
        questionText: 'text question',
      })
      await adminQuestions.addTextQuestion({
        questionName: 'show-other-q',
        description: 'desc',
        questionText: 'conditional question',
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: firstScreen,
        description: 'first screen',
        questions: [{name: 'show-predicate-q'}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: secondScreen,
        description: 'screen with predicate',
        questions: [{name: 'show-other-q'}],
      })
    })

    await test.step('Eligibility message field is visible', async () => {
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        firstScreen,
      )
      await expect(page.getByLabel('Eligibility message')).toBeVisible()
      await expect(page.getByText('Markdown is supported')).toBeVisible()
    })

    await test.step('Eligibility message field gets updated', async () => {
      await adminPredicates.updateEligibilityMessage(
        'Customized eligibility message',
      )
      await validateToastMessage(
        page,
        'Eligibility message set to Customized eligibility message',
      )
      await validateScreenshot(page, 'edit-predicate-eligibility-msg-updated')
    })
  })

  // TODO(https://github.com/civiform/civiform/issues/4167): Enable integration testing of ESRI functionality
  if (isLocalDevEnvironment()) {
    test('add a service area validation predicate', async ({
      page,
      adminQuestions,
      adminPrograms,
      adminPredicates,
    }) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      // Add a program with two screens
      await adminQuestions.addAddressQuestion({
        questionName: 'eligibility-predicate-q',
      })

      const programName = 'Create eligibility predicate'
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [{name: 'eligibility-predicate-q'}],
      })

      await adminPrograms.clickAddressCorrectionToggle()

      // Edit predicate for second screen
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
      )

      // Add two to ensure the JS correctly handles multiple value rows
      await adminPredicates.addPredicates({
        questionName: 'eligibility-predicate-q',
        scalar: 'service area',
        operator: 'in service area',
        values: ['Seattle', 'Seattle'],
      })

      await adminPredicates.expectPredicateDisplayTextContains(
        '"eligibility-predicate-q" is in service area "Seattle"',
      )

      // ensure the edit page renders without errors
      await adminPredicates.clickEditPredicateButton('eligibility')
      expect(await page.innerText('h1')).toContain(
        'Configure eligibility conditions',
      )
    })
  }

  test('show operator help texts', async ({
    page,
    adminPrograms,
    adminPredicates,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    await adminQuestions.addNameQuestion({questionName: 'name-question'})
    await adminQuestions.addDateQuestion({questionName: 'date-question'})
    await adminQuestions.addDropdownQuestion({
      questionName: 'dropdown-question',
      options: [
        {adminName: 'a', text: 'a'},
        {adminName: 'b', text: 'b'},
        {adminName: 'c', text: 'c'},
      ],
    })
    await adminQuestions.addEmailQuestion({questionName: 'email-question'})
    await adminQuestions.addCheckboxQuestion({
      questionName: 'checkbox-question',
      options: [
        {adminName: 'a', text: 'a'},
        {adminName: 'b', text: 'b'},
        {adminName: 'c', text: 'c'},
      ],
    })

    const programName = 'Help text program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      questions: [
        {name: 'name-question'},
        {name: 'date-question'},
        {name: 'dropdown-question'},
        {name: 'email-question'},
        {name: 'checkbox-question'},
      ],
    })
    await adminPrograms.goToEditBlockEligibilityPredicatePage(
      programName,
      'Screen 1',
    )

    await adminPredicates.selectQuestionForPredicate('name-question')
    await adminPredicates.selectQuestionForPredicate('date-question')
    await adminPredicates.selectQuestionForPredicate('dropdown-question')
    await adminPredicates.selectQuestionForPredicate('email-question')
    await adminPredicates.selectQuestionForPredicate('checkbox-question')
    await adminPredicates.clickAddConditionButton()

    await adminPredicates.addValueRows(1)

    await adminPredicates.configurePredicate({
      questionName: 'name-question',
      scalar: 'first name',
      operator: 'is one of',
    })
    await adminPredicates.configurePredicate({
      questionName: 'date-question',
      scalar: 'date',
      operator: 'age is between',
    })
    await adminPredicates.configurePredicate({
      questionName: 'dropdown-question',
      scalar: 'selection',
      operator: 'is one of',
    })
    await adminPredicates.configurePredicate({
      questionName: 'email-question',
      scalar: 'email',
      operator: 'is one of',
    })

    await validateScreenshot(
      page.locator('.predicate-config-form'),
      'operator-help-text',
      {
        fullPage: false,
      },
    )
  })

  test('eligibility multiple values and multiple questions', async ({
    page,
    adminPrograms,
    adminPredicates,
    adminQuestions,
  }) => {
    test.slow()

    await loginAsAdmin(page)

    await adminQuestions.addDateQuestion({questionName: 'date-question'})
    await adminQuestions.addCurrencyQuestion({
      questionName: 'currency-question',
      questionText: '[currency](example.com) *question*',
      markdown: true,
    })
    await adminQuestions.addNumberQuestion({questionName: 'number-question'})
    await adminQuestions.addTextQuestion({questionName: 'text-question'})

    const programName = 'Test multiple question and value predicate config'
    await adminPrograms.addProgram(programName)

    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      questions: [
        {name: 'date-question'},
        {name: 'currency-question'},
        {name: 'number-question'},
        {name: 'text-question'},
      ],
    })

    await adminPrograms.goToEditBlockEligibilityPredicatePage(
      programName,
      'Screen 1',
    )

    await adminPredicates.addPredicates(
      {
        questionName: 'date-question',
        scalar: 'date',
        operator: 'is earlier than',
        values: ['2021-01-01', '2022-02-02'],
      },
      {
        questionName: 'currency-question',
        scalar: 'currency',
        operator: 'is less than',
        values: ['10', '20'],
      },
      // Question itself is a number question (single scalar answer)
      // but we specify multiple values for comparison.
      {
        questionName: 'number-question',
        scalar: 'number',
        operator: 'is one of',
        values: ['1,2', '3,4'],
      },
      // Question itself is a text question (single scalar answer)
      // but we specify multiple values for comparison.
      {
        questionName: 'text-question',
        scalar: 'text',
        operator: 'is not one of',
        values: ['one,two', 'three,four'],
      },
    )

    await validateScreenshot(
      page,
      'eligibility-predicates-multi-values-multi-questions-predicate-saved',
    )
    let predicateDisplay = await page.innerText('.cf-display-predicate')
    expect(predicateDisplay).toContain(
      'Applicant is eligible if any of the following is true:',
    )
    expect(predicateDisplay).toContain(
      '"currency-question" currency is less than $10.00',
    )
    expect(predicateDisplay).toContain(
      '"date-question" date is earlier than 2021-01-01',
    )
    expect(predicateDisplay).toContain(
      '"currency-question" currency is less than $20.00',
    )
    expect(predicateDisplay).toContain(
      '"date-question" date is earlier than 2022-02-02',
    )

    await adminPredicates.clickEditPredicateButton('eligibility')
    await validateScreenshot(
      page,
      'eligibility-predicates-multi-values-multi-questions-predicate-edit',
    )

    await adminPredicates.configurePredicate({
      questionName: 'currency-question',
      scalar: 'currency',
      operator: 'is greater than',
      values: ['100', '200'],
    })

    await adminPredicates.clickSaveConditionButton()
    await validateScreenshot(
      page,
      'eligibility-predicates-multi-values-multi-questions-predicate-updated',
    )
    predicateDisplay = await page.innerText('.cf-display-predicate')
    expect(predicateDisplay).toContain(
      '"currency-question" currency is greater than $100.00',
    )
    expect(predicateDisplay).toContain(
      '"currency-question" currency is greater than $200.00',
    )
  })

  test('visibility multiple values and multiple questions', async ({
    page,
    adminPrograms,
    adminPredicates,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    await adminQuestions.addDateQuestion({questionName: 'date-question'})
    await adminQuestions.addCurrencyQuestion({
      questionName: 'currency-question',
      questionText: '*currency question*',
      markdown: true,
    })

    const programName = 'Test multiple question and value predicate config'
    await adminPrograms.addProgram(programName)

    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      questions: [{name: 'date-question'}, {name: 'currency-question'}],
    })

    await adminPrograms.addProgramBlockUsingSpec(programName, {
      name: 'Screen 2',
    })

    await adminPrograms.goToEditBlockVisibilityPredicatePage(
      programName,
      'Screen 2',
    )

    await adminPredicates.addPredicates(
      {
        questionName: 'date-question',
        scalar: 'date',
        operator: 'is earlier than',
        values: ['2021-01-01', '2022-02-02'],
      },
      {
        questionName: 'currency-question',
        scalar: 'currency',
        operator: 'is less than',
        values: ['10', '20'],
      },
    )

    let predicateDisplay = await page.innerText('.cf-display-predicate')
    await validateScreenshot(
      page,
      'visibility-predicates-multi-values-multi-questions-predicate-saved',
    )
    expect(predicateDisplay).toContain(
      'Screen 2 is hidden if any of the following is true:',
    )
    expect(predicateDisplay).toContain(
      '"currency-question" currency is less than $10.00',
    )
    expect(predicateDisplay).toContain(
      '"date-question" date is earlier than 2021-01-01',
    )
    expect(predicateDisplay).toContain(
      '"currency-question" currency is less than $20.00',
    )
    expect(predicateDisplay).toContain(
      '"date-question" date is earlier than 2022-02-02',
    )

    await adminPredicates.clickEditPredicateButton('visibility')
    await validateScreenshot(
      page,
      'visibility-predicates-multi-values-multi-questions-predicate-edit',
    )

    await adminPredicates.configurePredicate({
      questionName: 'currency-question',
      scalar: 'currency',
      operator: 'is greater than',
      values: ['100', '200'],
    })

    await adminPredicates.clickSaveConditionButton()
    await validateScreenshot(
      page,
      'visibility-predicates-multi-values-multi-questions-predicate-updated',
    )
    predicateDisplay = await page.innerText('.cf-display-predicate')
    expect(predicateDisplay).toContain(
      '"currency-question" currency is greater than $100.00',
    )
    expect(predicateDisplay).toContain(
      '"currency-question" currency is greater than $200.00',
    )
  })

  test('every visibility right hand type evaluates correctly', async ({
    page,
    adminPrograms,
    applicantQuestions,
    adminPredicates,
    adminQuestions,
  }) => {
    test.slow()

    await loginAsAdmin(page)

    const programName = 'Test all visibility predicate types'
    await adminPrograms.addProgram(programName)

    // Configure each screen so that it is not shown until the question on
    // the screen before it is answered a certain way
    await test.step('Configure screen 1 - question only', async () => {
      await adminQuestions.addNameQuestion({questionName: 'name-question'})
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        questions: [{name: 'name-question'}],
      })
    })

    await test.step('Configure screen 2', async () => {
      await adminQuestions.addTextQuestion({questionName: 'text-question'})
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 2',
        questions: [{name: 'text-question'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 2',
      )
      await adminPredicates.addPredicates({
        questionName: 'name-question',
        action: 'shown if',
        scalar: 'first name',
        operator: 'is not equal to',
        value: 'hidden',
      })
    })

    await test.step('Configure screen 3', async () => {
      await adminQuestions.addNumberQuestion({
        questionName: 'number-question-equal-to',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 3',
        questions: [{name: 'number-question-equal-to'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 3',
      )
      await adminPredicates.addPredicates({
        questionName: 'text-question',
        action: 'shown if',
        scalar: 'text',
        operator: 'is one of',
        value: 'blue, green',
      })
    })

    await test.step('Configure screen 4', async () => {
      await adminQuestions.addNumberQuestion({
        questionName: 'number-question-one-of',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 4',
        questions: [{name: 'number-question-one-of'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 4',
      )
      await adminPredicates.addPredicates({
        questionName: 'number-question-equal-to',
        action: 'shown if',
        scalar: 'number',
        operator: 'is equal to',
        value: '42',
      })
    })

    await test.step('Configure screen 5', async () => {
      await adminQuestions.addCurrencyQuestion({
        questionName: 'currency-question',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 5',
        questions: [{name: 'currency-question'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 5',
      )
      await adminPredicates.addPredicates({
        questionName: 'number-question-one-of',
        action: 'shown if',
        scalar: 'number',
        operator: 'is one of',
        value: '123, 456',
      })
    })

    await test.step('Configure screen 6', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-is-earlier-than',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 6',
        questions: [{name: 'date-question-is-earlier-than'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 6',
      )
      await adminPredicates.addPredicates({
        questionName: 'currency-question',
        action: 'shown if',
        scalar: 'currency',
        operator: 'is greater than',
        value: '100.01',
      })
    })

    await test.step('Configure screen 7', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-on-or-after',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 7',
        questions: [{name: 'date-question-on-or-after'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 7',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-is-earlier-than',
        action: 'shown if',
        scalar: 'date',
        operator: 'is earlier than',
        value: '2021-01-01',
      })
    })

    await test.step('Configure screen 8', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-age-older-than',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 8',
        questions: [{name: 'date-question-age-older-than'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 8',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-on-or-after',
        action: 'shown if',
        scalar: 'date',
        operator: 'is on or later than',
        value: '2023-01-01',
      })
    })

    await test.step('Configure screen 9', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-age-younger-than',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 9',
        questions: [{name: 'date-question-age-younger-than'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 9',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-age-older-than',
        action: 'shown if',
        scalar: 'date',
        operator: 'age is older than',
        value: '90',
      })
    })

    await test.step('Configure screen 10', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-age-between',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 10',
        questions: [{name: 'date-question-age-between'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 10',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-age-younger-than',
        action: 'shown if',
        scalar: 'date',
        operator: 'age is younger than',
        value: '50.5',
      })
    })

    await test.step('Configure screen 11', async () => {
      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-question',
        options: [
          {adminName: 'dog_admin', text: 'dog'},
          {adminName: 'rabbit_admin', text: 'rabbit'},
          {adminName: 'cat_admin', text: 'cat'},
        ],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 11',
        questions: [{name: 'checkbox-question'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 11',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-age-between',
        action: 'shown if',
        scalar: 'date',
        operator: 'age is between',
        complexValues: [{value: '1', secondValue: '90'}],
      })
    })

    await test.step('Configure screen 12', async () => {
      await adminQuestions.addNumberQuestion({
        questionName: 'number-question-between',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 12',
        questions: [{name: 'number-question-between'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 12',
      )
      await adminPredicates.addPredicates({
        questionName: 'checkbox-question',
        action: 'shown if',
        scalar: 'selections',
        operator: 'contains any of',
        value: 'dog,cat',
      })
    })

    await test.step('Configure screen 13', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-between',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 13',
        questions: [{name: 'date-question-between'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 13',
      )
      await adminPredicates.addPredicates({
        questionName: 'number-question-between',
        action: 'shown if',
        scalar: 'number',
        operator: 'is between',
        complexValues: [{value: '10', secondValue: '20'}],
      })
    })

    await test.step('Configure screen 14', async () => {
      await adminQuestions.addCurrencyQuestion({
        questionName: 'currency-question-between',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 14',
        questions: [{name: 'currency-question-between'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 14',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-between',
        action: 'shown if',
        scalar: 'date',
        operator: 'is between',
        complexValues: [{value: '2020-05-20', secondValue: '2024-05-20'}],
      })
    })

    await test.step('Configure screen 15', async () => {
      await adminQuestions.addTextQuestion({
        questionName: 'text-question-last-page',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 15',
        questions: [{name: 'text-question-last-page'}],
      })
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 15',
      )
      await adminPredicates.addPredicates({
        questionName: 'currency-question-between',
        action: 'shown if',
        scalar: 'currency',
        operator: 'is between',
        complexValues: [{value: '4.25', secondValue: '9.99'}],
      })
    })

    await adminPrograms.publishProgram(programName)

    // Switch to applicantQuestions.view - if they answer each question according to the predicate,
    // the next screen will be shown.
    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName, true)

    // For each screen:
    // - enter and submit a disallowed answer
    // - verify the other screens aren't shown and the review page is shown
    // - go back
    // - enter and submit an allowed answer to go to the next screen

    await test.step('Apply screen 1', async () => {
      // "hidden" first name is not allowed.
      await applicantQuestions.answerNameQuestion('hidden', 'next', 'screen')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerNameQuestion('show', 'next', 'screen')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 2', async () => {
      // "blue" or "green" are allowed.
      await applicantQuestions.answerTextQuestion('red')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerTextQuestion('blue')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 3', async () => {
      // 42 is allowed.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('42')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 4', async () => {
      // 123 or 456 are allowed.
      await applicantQuestions.answerNumberQuestion('11111')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('123')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 5', async () => {
      // Greater than 100.01 is allowed
      await applicantQuestions.answerCurrencyQuestion('100.01')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('100.02')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 6', async () => {
      // Earlier than 2021-01-01 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '2021',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2020',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 7', async () => {
      // On or later than 2023-01-01 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2023',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 8', async () => {
      // Age greater than 90 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '1930',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 9', async () => {
      // Age less than 50.5 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '1930',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 10', async () => {
      // Age between 1 and 90 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '1920',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2000',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 11', async () => {
      // "dog" or "cat" are allowed.
      await applicantQuestions.answerCheckboxQuestion(['rabbit'])
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerCheckboxQuestion(['cat'])
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 12', async () => {
      // number between 10 and 20 is allowed
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('15')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 13', async () => {
      // date between 2020-05-20 and 2024-05-20 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '2019',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '05 - May',
        '20',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 14', async () => {
      // currency between 4.25 and 9.99 is allowed
      await applicantQuestions.answerCurrencyQuestion('2.00')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectReviewPage(true)
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('5.50')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 15', async () => {
      await applicantQuestions.answerTextQuestion('last one!')
      await applicantQuestions.clickContinue()
    })

    // We should now be on the summary page
    await applicantQuestions.submitFromReviewPage(true)
  })

  test('every eligibility right hand type evaluates correctly', async ({
    page,
    adminPrograms,
    applicantQuestions,
    adminPredicates,
    adminQuestions,
  }) => {
    test.slow()

    await loginAsAdmin(page)
    const programName = 'Test all eligibility predicate types'
    await adminPrograms.addProgram(programName)

    await test.step('Configure screen 1', async () => {
      await adminQuestions.addNameQuestion({questionName: 'name-question'})
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        questions: [{name: 'name-question'}],
      })
      // Simple string predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
      )
      await adminPredicates.addPredicates({
        questionName: 'name-question',
        scalar: 'first name',
        operator: 'is not equal to',
        value: 'hidden',
      })
    })

    await test.step('Configure screen 2', async () => {
      await adminQuestions.addTextQuestion({questionName: 'text-question'})
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 2',
        questions: [{name: 'text-question'}],
      })
      // Single string one of a list of strings
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 2',
      )
      await adminPredicates.addPredicates({
        questionName: 'text-question',
        scalar: 'text',
        operator: 'is one of',
        value: 'blue, green',
      })
    })

    await test.step('Configure screen 3', async () => {
      await adminQuestions.addNumberQuestion({
        questionName: 'number-question-equal-to',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 3',
        questions: [{name: 'number-question-equal-to'}],
      })
      // Simple long predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 3',
      )
      await adminPredicates.addPredicates({
        questionName: 'number-question-equal-to',
        scalar: 'number',
        operator: 'is equal to',
        value: '42',
      })
    })

    await test.step('Configure screen 4', async () => {
      await adminQuestions.addNumberQuestion({
        questionName: 'number-question-one-of',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 4',
        questions: [{name: 'number-question-one-of'}],
      })
      // Single long one of a list of longs
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 4',
      )
      await adminPredicates.addPredicates({
        questionName: 'number-question-one-of',
        scalar: 'number',
        operator: 'is one of',
        value: '123, 456',
      })
    })

    await test.step('Configure screen 5', async () => {
      await adminQuestions.addCurrencyQuestion({
        questionName: 'currency-question',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 5',
        questions: [{name: 'currency-question'}],
      })
      // Currency predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 5',
      )
      await adminPredicates.addPredicates({
        questionName: 'currency-question',
        scalar: 'currency',
        operator: 'is greater than',
        value: '100.01',
      })
    })

    await test.step('Configure screen 6', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-is-earlier-than',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 6',
        questions: [{name: 'date-question-is-earlier-than'}],
      })
      // Date predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 6',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-is-earlier-than',
        scalar: 'date',
        operator: 'is earlier than',
        value: '2021-01-01',
      })
    })

    await test.step('Configure screen 7', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-on-or-after',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 7',
        questions: [{name: 'date-question-on-or-after'}],
      })
      // Date predicate is on or after
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 7',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-on-or-after',
        scalar: 'date',
        operator: 'is on or later than',
        value: '2023-01-01',
      })
    })

    await test.step('Configure screen 8', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-age-older-than',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 8',
        questions: [{name: 'date-question-age-older-than'}],
      })
      // Date predicate age is greater than
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 8',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-age-older-than',
        scalar: 'date',
        operator: 'age is older than',
        value: '90',
      })
      // ensure the edit page renders without errors
      await adminPredicates.clickEditPredicateButton('eligibility')
      expect(await page.innerText('h1')).toContain(
        'Configure eligibility conditions',
      )
      await validateScreenshot(page, 'predicate-age-greater-than-edit')
      await adminPredicates.clickSaveConditionButton()
    })

    await test.step('Configure screen 9', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-age-younger-than',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 9',
        questions: [{name: 'date-question-age-younger-than'}],
      })
      // Date predicate age is less than
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 9',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-age-younger-than',
        scalar: 'date',
        operator: 'age is younger than',
        value: '50.5',
      })

      // ensure the edit page renders without errors
      await adminPredicates.clickEditPredicateButton('eligibility')
      expect(await page.innerText('h1')).toContain(
        'Configure eligibility conditions',
      )
      await adminPredicates.clickSaveConditionButton()
    })

    await test.step('Configure screen 10', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-age-between',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 10',
        questions: [{name: 'date-question-age-between'}],
      })
      // Date predicate age is between
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 10',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-age-between',
        scalar: 'date',
        operator: 'age is between',
        complexValues: [{value: '1', secondValue: '90'}],
      })

      // ensure the edit page renders without errors
      await adminPredicates.clickEditPredicateButton('eligibility')
      expect(await page.innerText('h1')).toContain(
        'Configure eligibility conditions',
      )
      await validateScreenshot(page, 'predicate-age-between-edit')
      await adminPredicates.clickSaveConditionButton()
    })

    await test.step('Configure screen 11', async () => {
      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-question',
        options: [
          {adminName: 'dog_admin', text: 'dog'},
          {adminName: 'rabbit_admin', text: 'rabbit'},
          {adminName: 'cat_admin', text: 'cat'},
        ],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 11',
        questions: [{name: 'checkbox-question'}],
      })
      // Lists of strings on both sides (multi-option question checkbox)
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 11',
      )
      await adminPredicates.addPredicates({
        questionName: 'checkbox-question',
        scalar: 'selections',
        operator: 'contains any of',
        value: 'dog,cat',
      })
    })

    await test.step('Configure screen 12', async () => {
      await adminQuestions.addNumberQuestion({
        questionName: 'number-question-between',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 12',
        questions: [{name: 'number-question-between'}],
      })
      // Lists of strings on both sides (multi-option question checkbox)
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 12',
      )
      await adminPredicates.addPredicates({
        questionName: 'number-question-between',
        scalar: 'number',
        operator: 'is between',
        complexValues: [{value: '10', secondValue: '20'}],
      })
    })

    await test.step('Configure screen 13', async () => {
      await adminQuestions.addDateQuestion({
        questionName: 'date-question-between',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 13',
        questions: [{name: 'date-question-between'}],
      })
      // Lists of strings on both sides (multi-option question checkbox)
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 13',
      )
      await adminPredicates.addPredicates({
        questionName: 'date-question-between',
        scalar: 'date',
        operator: 'is between',
        complexValues: [{value: '2020-05-20', secondValue: '2024-05-20'}],
      })
    })

    await test.step('Configure screen 14', async () => {
      await adminQuestions.addCurrencyQuestion({
        questionName: 'currency-question-between',
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 14',
        questions: [{name: 'currency-question-between'}],
      })
      // Lists of strings on both sides (multi-option question checkbox)
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 14',
      )
      await adminPredicates.addPredicates({
        questionName: 'currency-question-between',
        scalar: 'currency',
        operator: 'is between',
        complexValues: [{value: '4.25', secondValue: '9.99'}],
      })
    })

    await adminPrograms.publishProgram(programName)

    // Switch to applicantQuestions.view - if they answer each question according to the predicate,
    // the next screen will be shown.
    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName, true)

    // For each condition:
    // - submit an invalid option
    // - verify the ineligible page is shown
    // - go back
    // - enter an allowed value

    await test.step('Apply screen 1', async () => {
      // "hidden" first name is not allowed.
      await applicantQuestions.answerNameQuestion('hidden', 'next', 'screen')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'name question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerNameQuestion('show', 'next', 'screen')
      await applicantQuestions.clickContinue()
      await validateScreenshot(page, 'toast-message-may-qualify')
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
    })

    await test.step('Apply screen 2', async () => {
      // "blue" or "green" are allowed.
      await applicantQuestions.answerTextQuestion('red')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'text question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerTextQuestion('blue')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
    })

    await test.step('Apply screen 3', async () => {
      // 42 is allowed.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'number question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('42')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
    })

    await test.step('Apply screen 4', async () => {
      // 123 or 456 are allowed.
      await applicantQuestions.answerNumberQuestion('11111')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'number question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('123')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()

      await applicantQuestions.clickReview(true)
      await validateScreenshot(page, 'review-page-no-ineligible-banner')
      await validateToastMessage(page, '')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 5', async () => {
      // Greater than 100.01 is allowed
      await applicantQuestions.answerCurrencyQuestion('100.01')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'currency question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('100.02')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
    })

    await test.step('Apply screen 6', async () => {
      // Earlier than 2021-01-01 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '2021',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'date question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2020',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
    })

    await test.step('Apply screen 7', async () => {
      // On or later than 2023-01-01 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'date question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2023',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
    })

    await test.step('Apply screen 8', async () => {
      // Age greater than 90 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'date question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '1930',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 9', async () => {
      // Age less than 50.5 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '1930',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'date question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 10', async () => {
      // Age between 1 and 90 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '1920',
        '12 - December',
        '31',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'date question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2000',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 11', async () => {
      // "dog" or "cat" are allowed.
      await applicantQuestions.answerCheckboxQuestion(['rabbit'])
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'checkbox question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)

      await applicantQuestions.clickGoBackAndEditOnIneligiblePageNorthStar()
      await validateScreenshot(page, 'review-page-has-ineligible-banner')
      await applicantQuestions.expectMayNotBeEligibileAlertToBeVisible()

      await applicantQuestions.editQuestionFromReviewPage(
        'checkbox question text',
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerCheckboxQuestion(['cat'])
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 12', async () => {
      // Age between 1 and 90 is allowed
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'number question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('15')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 13', async () => {
      // date between 2020-05-20 and 2024-05-20 is allowed
      await applicantQuestions.answerMemorableDateQuestion(
        '2019',
        '01 - January',
        '01',
      )
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'date question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '05 - May',
        '20',
      )
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 14', async () => {
      // currency between 4.25 and 9.99 is allowed
      await applicantQuestions.answerCurrencyQuestion('2.00')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectIneligiblePage(true)
      await applicantQuestions.expectIneligibleQuestionNorthStar(
        'currency question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('5.50')
      await applicantQuestions.clickContinue()
    })

    // We should now be on the review page with a completed form and no banner should show.
    await validateScreenshot(page, 'review-page-no-ineligible-banner-completed')
    await validateToastMessage(page, '')

    await applicantQuestions.submitFromReviewPage(true)
  })

  test('multiple questions ineligible', async ({
    page,
    adminPrograms,
    adminPredicates,
    adminQuestions,
    applicantQuestions,
  }) => {
    test.slow()

    await loginAsAdmin(page)

    await adminQuestions.addNameQuestion({questionName: 'name-question'})
    await adminQuestions.addCurrencyQuestion({
      questionName: 'currency-question',
    })

    const programName = 'Multiple ineligible program'
    await adminPrograms.addProgram(programName)

    // Name predicate
    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      questions: [{name: 'name-question'}],
    })
    await adminPrograms.goToEditBlockEligibilityPredicatePage(
      programName,
      'Screen 1',
    )
    await adminPredicates.addPredicates({
      questionName: 'name-question',
      scalar: 'first name',
      operator: 'is not equal to',
      value: 'hidden',
    })

    // Currency predicate
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      name: 'Screen 2',
      questions: [{name: 'currency-question'}],
    })
    await adminPrograms.goToEditBlockEligibilityPredicatePage(
      programName,
      'Screen 2',
    )
    await adminPredicates.addPredicates({
      questionName: 'currency-question',
      scalar: 'currency',
      operator: 'is greater than',
      value: '100.01',
    })

    await adminPrograms.publishProgram(programName)
    await logout(page)

    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName, true)

    // 'Hidden' name is ineligible
    await applicantQuestions.answerNameQuestion('hidden', 'next', 'screen')
    await applicantQuestions.clickContinue()
    await applicantQuestions.expectIneligiblePage(true)
    await applicantQuestions.expectIneligibleQuestionsCountNorthStar(1)
    await applicantQuestions.clickGoBackAndEditOnIneligiblePageNorthStar()

    // Less than or equal to 100.01 is ineligible
    await applicantQuestions.editQuestionFromReviewPage(
      'currency question text',
      /* northStarEnabled= */ true,
    )
    await applicantQuestions.answerCurrencyQuestion('100.01')
    await applicantQuestions.clickContinue()

    await applicantQuestions.expectIneligiblePage(true)
    await applicantQuestions.expectIneligibleQuestionsCountNorthStar(2)
    await validateAccessibility(page)
    await validateScreenshot(page, 'ineligible-multiple-eligibility-questions')
  })
})
