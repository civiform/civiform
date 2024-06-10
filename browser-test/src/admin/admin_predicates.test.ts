import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  isHermeticTestEnvironment,
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
  test('add a hide predicate', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)

    // Add a program with two screens
    await adminQuestions.addTextQuestion({questionName: 'hide-predicate-q'})
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
      questions: [{name: 'hide-predicate-q'}],
    })
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      name: 'Screen 2',
      description: 'screen with predicate',
      questions: [{name: 'hide-other-q'}],
    })

    // Edit predicate for second block
    await adminPrograms.goToEditBlockVisibilityPredicatePage(
      programName,
      'Screen 2',
    )

    await adminPredicates.clickAddConditionButton()
    await validateToastMessage(page, 'Please select a question')

    await adminPredicates.addPredicates({
      questionName: 'hide-predicate-q',
      action: 'hidden if',
      scalar: 'text',
      operator: 'is equal to',
      value: 'hide me',
    })
    await adminPredicates.expectPredicateDisplayTextContains(
      'Screen 2 is hidden if "hide-predicate-q" text is equal to "hide me"',
    )
    await validateScreenshot(page, 'hide-predicate')

    // Publish the program
    await adminPrograms.publishProgram(programName)

    // Switch to the applicantQuestions.view and apply to the program
    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName)

    // Initially fill out the first screen so that the next screen will be shown
    await applicantQuestions.answerTextQuestion('show me')
    await applicantQuestions.clickNext()

    // Fill out the second screen
    await applicantQuestions.answerTextQuestion(
      'will be hidden and not submitted',
    )
    await applicantQuestions.clickNext()

    // We should be on the review page, with an answer to Screen 2's question
    expect(await page.innerText('#application-summary')).toContain(
      'conditional question',
    )

    // Return to the first screen and answer it so that the second screen is hidden
    await page.click('text=Edit') // first screen edit
    await applicantQuestions.answerTextQuestion('hide me')
    await applicantQuestions.clickNext()

    // We should be on the review page
    expect(await page.innerText('#application-summary')).not.toContain(
      'conditional question',
    )
    await applicantQuestions.submitFromReviewPage()

    // Visit the program admin page and assert the hidden question does not show
    await logout(page)
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())

    const applicationText = await adminPrograms
      .applicationFrameLocator()
      .locator('#application-view')
      .innerText()
    expect(applicationText).not.toContain('Screen 2')
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
    await adminQuestions.addTextQuestion({
      questionName: 'show-predicate-q',
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
      questions: [{name: 'show-predicate-q'}],
    })
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      name: 'Screen 2',
      description: 'screen with predicate',
      questions: [{name: 'show-other-q'}],
    })

    // Edit predicate for second screen
    await adminPrograms.goToEditBlockVisibilityPredicatePage(
      programName,
      'Screen 2',
    )
    await adminPredicates.addPredicates({
      questionName: 'show-predicate-q',
      action: 'shown if',
      scalar: 'text',
      operator: 'is equal to',
      value: 'show me',
    })
    await adminPredicates.expectPredicateDisplayTextContains(
      'Screen 2 is shown if "show-predicate-q" text is equal to "show me"',
    )
    await validateScreenshot(page, 'show-predicate')

    // Publish the program
    await adminPrograms.publishProgram(programName)

    // Switch to the applicantQuestions.view and apply to the program
    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName)

    // Initially fill out the first screen so that the next screen will be hidden
    await applicantQuestions.answerTextQuestion('hide next screen')
    await applicantQuestions.clickNext()

    // We should be on the review page, with no Screen 2 questions shown. We should
    // be able to submit the application
    expect(await page.innerText('#application-summary')).not.toContain(
      'conditional question',
    )
    expect((await page.innerText('.cf-submit-button')).toLowerCase()).toContain(
      'submit',
    )

    // Return to the first screen and answer it so that the second screen is shown
    await page.click('text=Edit') // first screen edit
    await applicantQuestions.answerTextQuestion('show me')
    await applicantQuestions.clickNext()

    // The second screen should now appear, and we must fill it out
    await applicantQuestions.answerTextQuestion('hello world!')
    await applicantQuestions.clickNext()
    await validateScreenshot(page, 'program-summary-page')

    // We should be on the review page
    expect(await page.innerText('#application-summary')).toContain(
      'conditional question',
    )
    await applicantQuestions.submitFromReviewPage()

    // Visit the program admin page and assert the conditional question is shown
    await logout(page)
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)

    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    expect(
      await adminPrograms
        .applicationFrameLocator()
        .locator('#application-view')
        .innerText(),
    ).toContain('Screen 2')
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
      'Screen 1 is eligible if "eligibility-predicate-q" text is equal to "eligible"',
    )
    await validateScreenshot(page, 'eligibility-predicate')

    await page.click(`a:has-text("Back")`)
    await validateScreenshot(page, 'block-settings-page')

    // Publish the program
    await adminPrograms.publishProgram(programName)

    // Switch to the applicantQuestions.view and apply to the program
    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName)

    // Initially fill out the first screen so that it is ineligible
    await applicantQuestions.answerTextQuestion('ineligble')
    await applicantQuestions.clickNext()
    await applicantQuestions.expectIneligiblePage()
    await validateScreenshot(page, 'ineligible')

    // Begin waiting for the popup before clicking the link, otherwise
    // the popup may fire before the wait is registered, causing the test to flake.
    const popupPromise = page.waitForEvent('popup')
    await page.click('text=program details')
    const popup = await popupPromise
    const popupURL = await popup.evaluate('location.href')

    // Verify if the program details page Url to be the external link"
    expect(popupURL).toMatch('https://www.usa.gov/')
    // Return to the screen and fill it out to be eligible.
    await page.goBack()
    await applicantQuestions.answerTextQuestion('eligible')
    await applicantQuestions.clickNext()

    // We should be on the review page, and able to submit the application
    expect((await page.innerText('.cf-submit-button')).toLowerCase()).toContain(
      'submit',
    )
    await applicantQuestions.submitFromReviewPage()

    // Visit the program admin page and assert the question is shown
    await logout(page)
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)

    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    expect(
      await adminPrograms
        .applicationFrameLocator()
        .locator('#application-view')
        .innerText(),
    ).toContain('Screen 1')
  })

  // TODO(https://github.com/civiform/civiform/issues/4167): Enable integration testing of ESRI functionality
  if (isHermeticTestEnvironment()) {
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
        scalar: 'service_area',
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
      /* fullPage= */ false,
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

    let predicateDisplay = await page.innerText('.cf-display-predicate')
    await validateScreenshot(
      page,
      'eligibility-predicates-multi-values-multi-questions-predicate-saved',
    )
    expect(predicateDisplay).toContain('Screen 1 is eligible if any of:')
    expect(predicateDisplay).toContain(
      '"currency-question" currency is less than $10.00',
    )
    expect(predicateDisplay).toContain(
      '"date-question" date is earlier than 2021-01-01',
    )
    expect(predicateDisplay).toContain(
      '"currency question" currency is less than $20.00',
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
    expect(predicateDisplay).toContain('Screen 2 is hidden if any of:')
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
    await applicantQuestions.applyProgram(programName)

    // For each screen:
    // - enter and submit a disallowed answer
    // - verify the other screens aren't shown and the review page is shown
    // - go back
    // - enter and submit an allowed answer to go to the next screen

    await test.step('Apply screen 1', async () => {
      // "hidden" first name is not allowed.
      await applicantQuestions.answerNameQuestion('hidden', 'next', 'screen')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerNameQuestion('show', 'next', 'screen')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 2', async () => {
      // "blue" or "green" are allowed.
      await applicantQuestions.answerTextQuestion('red')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerTextQuestion('blue')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 3', async () => {
      // 42 is allowed.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('42')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 4', async () => {
      // 123 or 456 are allowed.
      await applicantQuestions.answerNumberQuestion('11111')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('123')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 5', async () => {
      // Greater than 100.01 is allowed
      await applicantQuestions.answerCurrencyQuestion('100.01')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('100.02')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 6', async () => {
      // Earlier than 2021-01-01 is allowed
      await applicantQuestions.answerDateQuestion('2021-01-01')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2020-12-31')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 7', async () => {
      // On or later than 2023-01-01 is allowed
      await applicantQuestions.answerDateQuestion('2022-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2023-01-01')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 8', async () => {
      // Age greater than 90 is allowed
      await applicantQuestions.answerDateQuestion('2022-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerDateQuestion('1930-01-01')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 9', async () => {
      // Age less than 50.5 is allowed
      await applicantQuestions.answerDateQuestion('1930-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2022-01-01')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 10', async () => {
      // Age between 1 and 90 is allowed
      await applicantQuestions.answerDateQuestion('1920-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2000-01-01')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 11', async () => {
      // "dog" or "cat" are allowed.
      await applicantQuestions.answerCheckboxQuestion(['rabbit'])
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerCheckboxQuestion(['cat'])
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 12', async () => {
      // number between 10 and 20 is allowed
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('15')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 13', async () => {
      // date between 2020-05-20 and 2024-05-20 is allowed
      await applicantQuestions.answerDateQuestion('2019-01-01')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2022-05-20')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 14', async () => {
      // currency between 4.25 and 9.99 is allowed
      await applicantQuestions.answerCurrencyQuestion('2.00')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('5.50')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 15', async () => {
      await applicantQuestions.answerTextQuestion('last one!')
      await applicantQuestions.clickNext()
    })

    // We should now be on the summary page
    await applicantQuestions.submitFromReviewPage()
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
    await applicantQuestions.applyProgram(programName)

    // For each condition:
    // - submit an invalid option
    // - verify the ineligible page is shown
    // - go back
    // - enter an allowed value

    await test.step('Apply screen 1', async () => {
      // "hidden" first name is not allowed.
      await applicantQuestions.answerNameQuestion('hidden', 'next', 'screen')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('name question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerNameQuestion('show', 'next', 'screen')
      await applicantQuestions.clickNext()
      await validateScreenshot(page, 'toast-message-may-qualify')
      await validateToastMessage(page, 'may qualify')
    })

    await test.step('Apply screen 2', async () => {
      // "blue" or "green" are allowed.
      await applicantQuestions.answerTextQuestion('red')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('text question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerTextQuestion('blue')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
    })

    await test.step('Apply screen 3', async () => {
      // 42 is allowed.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('number question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('42')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
    })

    await test.step('Apply screen 4', async () => {
      // 123 or 456 are allowed.
      await applicantQuestions.answerNumberQuestion('11111')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('number question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('123')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')

      await applicantQuestions.clickReview()
      await validateScreenshot(page, 'review-page-no-ineligible-banner')
      await validateToastMessage(page, '')
      await applicantQuestions.clickContinue()
    })

    await test.step('Apply screen 5', async () => {
      // Greater than 100.01 is allowed
      await applicantQuestions.answerCurrencyQuestion('100.01')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion(
        'currency question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('100.02')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
    })

    await test.step('Apply screen 6', async () => {
      // Earlier than 2021-01-01 is allowed
      await applicantQuestions.answerDateQuestion('2021-01-01')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2020-12-31')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
    })

    await test.step('Apply screen 7', async () => {
      // On or later than 2023-01-01 is allowed
      await applicantQuestions.answerDateQuestion('2022-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2023-01-01')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
    })

    await test.step('Apply screen 8', async () => {
      // Age greater than 90 is allowed
      await applicantQuestions.answerDateQuestion('2022-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('1930-01-01')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 9', async () => {
      // Age less than 50.5 is allowed
      await applicantQuestions.answerDateQuestion('1930-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2022-01-01')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 10', async () => {
      // Age between 1 and 90 is allowed
      await applicantQuestions.answerDateQuestion('1920-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2000-01-01')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 11', async () => {
      // "dog" or "cat" are allowed.
      await applicantQuestions.answerCheckboxQuestion(['rabbit'])
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion(
        'checkbox question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCount(1)

      await applicantQuestions.clickGoBackAndEditOnIneligiblePage()
      await validateScreenshot(page, 'review-page-has-ineligible-banner')
      await validateToastMessage(page, 'may not qualify')

      await applicantQuestions.editQuestionFromReviewPage(
        'checkbox question text',
      )
      await applicantQuestions.answerCheckboxQuestion(['cat'])
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 12', async () => {
      // Age between 1 and 90 is allowed
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('number question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('15')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 13', async () => {
      // date between 2020-05-20 and 2024-05-20 is allowed
      await applicantQuestions.answerDateQuestion('2019-01-01')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2022-05-20')
      await applicantQuestions.clickNext()
    })

    await test.step('Apply screen 14', async () => {
      // currency between 4.25 and 9.99 is allowed
      await applicantQuestions.answerCurrencyQuestion('2.00')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion(
        'currency question text',
      )
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('5.50')
      await applicantQuestions.clickNext()
    })

    // We should now be on the review page with a completed form and no banner should show.
    await validateScreenshot(page, 'review-page-no-ineligible-banner-completed')
    await validateToastMessage(page, '')

    await applicantQuestions.submitFromReviewPage()
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
    await applicantQuestions.applyProgram(programName)

    // 'Hidden' name is ineligible
    await applicantQuestions.answerNameQuestion('hidden', 'next', 'screen')
    await applicantQuestions.clickNext()
    await applicantQuestions.expectIneligiblePage()
    await applicantQuestions.expectIneligibleQuestionsCount(1)
    await applicantQuestions.clickGoBackAndEditOnIneligiblePage()

    // Less than or equal to 100.01 is ineligible
    await applicantQuestions.answerQuestionFromReviewPage(
      'currency question text',
    )
    await applicantQuestions.answerCurrencyQuestion('100.01')
    await applicantQuestions.clickNext()

    await applicantQuestions.expectIneligiblePage()
    await applicantQuestions.expectIneligibleQuestionsCount(2)
    await validateAccessibility(page)
    await validateScreenshot(page, 'ineligible-multiple-eligibility-questions')
  })
})
