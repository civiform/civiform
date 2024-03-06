import {test, expect} from '../fixtures/custom_fixture'
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
    await adminPrograms.editProgramBlock(programName, 'first screen', [
      'hide-predicate-q',
    ])
    await adminPrograms.addProgramBlock(programName, 'screen with predicate', [
      'hide-other-q',
    ])

    // Edit predicate for second block
    await adminPrograms.goToEditBlockVisibilityPredicatePage(
      programName,
      'Screen 2',
    )

    await adminPredicates.clickAddConditionButton()
    await validateToastMessage(page, 'Please select a question')

    await adminPredicates.addPredicate(
      'hide-predicate-q',
      'hidden if',
      'text',
      'is equal to',
      'hide me',
    )
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
    await adminQuestions.addTextQuestion({questionName: 'show-predicate-q'})
    await adminQuestions.addTextQuestion({
      questionName: 'show-other-q',
      description: 'desc',
      questionText: 'conditional question',
    })

    const programName = 'Create show predicate'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'first screen', [
      'show-predicate-q',
    ])
    await adminPrograms.addProgramBlock(programName, 'screen with predicate', [
      'show-other-q',
    ])

    // Edit predicate for second screen
    await adminPrograms.goToEditBlockVisibilityPredicatePage(
      programName,
      'Screen 2',
    )
    await adminPredicates.addPredicate(
      'show-predicate-q',
      'shown if',
      'text',
      'is equal to',
      'show me',
    )
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
    await adminPrograms.editProgramBlock(programName, 'first screen', [
      'eligibility-predicate-q',
    ])

    // Edit predicate for second screen
    await adminPrograms.goToEditBlockEligibilityPredicatePage(
      programName,
      'Screen 1',
    )

    // Add predicate with missing operator.
    await adminPredicates.addPredicate(
      'eligibility-predicate-q',
      /* action= */ null,
      'text',
      '',
      'eligible',
    )
    await adminPredicates.expectPredicateErrorToast('dropdowns')

    await adminPredicates.configurePredicate({
      questionName: 'eligibility-predicate-q',
      action: null,
      scalar: 'text',
      operator: 'is equal to',
      value: 'eligible',
    })
    await adminPredicates.clickSaveConditionButton()
    await adminPredicates.clickRemovePredicateButton('eligibility')

    // Add predicate with missing value.
    await adminPredicates.addPredicate(
      'eligibility-predicate-q',
      /* action= */ null,
      'text',
      'is equal to',
      '',
    )
    await adminPredicates.expectPredicateErrorToast('form fields')
    await validateScreenshot(page, 'predicate-error')

    await adminPredicates.configurePredicate({
      questionName: 'eligibility-predicate-q',
      action: null,
      scalar: 'text',
      operator: 'is equal to',
      value: 'eligible',
    })
    await adminPredicates.clickSaveConditionButton()
    await adminPredicates.clickRemovePredicateButton('eligibility')

    // Add predicate with missing operator and value.
    await adminPredicates.addPredicate(
      'eligibility-predicate-q',
      /* action= */ null,
      'text',
      '',
      '',
    )
    await adminPredicates.clickSaveConditionButton()
    await adminPredicates.expectPredicateErrorToast('form fields or dropdowns')

    await adminPredicates.configurePredicate({
      questionName: 'eligibility-predicate-q',
      action: null,
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
    test('add a service area validation predicate', async ({page, adminQuestions, adminPrograms, adminPredicates} ) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      // Add a program with two screens
      await adminQuestions.addAddressQuestion({
        questionName: 'eligibility-predicate-q',
      })

      const programName = 'Create eligibility predicate'
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, 'first screen', [
        'eligibility-predicate-q',
      ])

      await adminPrograms.clickAddressCorrectionToggle()

      // Edit predicate for second screen
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
      )

      // Add two to ensure the JS correctly handles multiple value rows
      await adminPredicates.addPredicates([
        {
          questionName: 'eligibility-predicate-q',
          scalar: 'service_area',
          operator: 'in service area',
          values: ['Seattle', 'Seattle'],
        },
      ])

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

  test.describe('test predicates', () => {
    test.beforeEach(async ( {page, adminQuestions} ) => {
      await loginAsAdmin(page)

      // DATE, STRING, LONG, LIST_OF_STRINGS, LIST_OF_LONGS
      await adminQuestions.addNameQuestion({questionName: 'single-string'})
      await adminQuestions.addTextQuestion({questionName: 'list of strings'})
      await adminQuestions.addNumberQuestion({questionName: 'single-long'})
      await adminQuestions.addNumberQuestion({questionName: 'list of longs'})
      await adminQuestions.addCurrencyQuestion({
        questionName: 'predicate-currency',
      })
      await adminQuestions.addDateQuestion({
        questionName: 'predicate-date-is-earlier-than',
      })
      await adminQuestions.addDateQuestion({
        questionName: 'predicate-date-on-or-after',
      })
      await adminQuestions.addDateQuestion({
        questionName: 'predicate-date-age-older-than',
      })
      await adminQuestions.addDateQuestion({
        questionName: 'predicate-date-age-younger-than',
      })
      await adminQuestions.addDateQuestion({
        questionName: 'predicate-date-age-between',
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'both sides are lists',
        options: [
          {adminName: 'dog_admin', text: 'dog'},
          {adminName: 'rabbit_admin', text: 'rabbit'},
          {adminName: 'cat_admin', text: 'cat'},
        ],
      })
      await adminQuestions.addTextQuestion({
        questionName: 'depends on previous',
      })

      await logout(page)
    })

    test('eligibility multiple values and multiple questions', async ({page, adminPrograms, adminPredicates}) => {
      await loginAsAdmin(page)

      const programName = 'Test multiple question and value predicate config'
      await adminPrograms.addProgram(programName)

      const questions = [
        'predicate-date-is-earlier-than',
        'predicate-currency',
        'list of longs',
        'list of strings',
        'predicate-date-age-older-than',
      ]

      await adminPrograms.editProgramBlock(programName, 'test-block', questions)

      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
      )

      await adminPredicates.addPredicates([
        {
          questionName: 'predicate-date-is-earlier-than',
          scalar: 'date',
          operator: 'is earlier than',
          values: ['2021-01-01', '2022-02-02'],
        },
        {
          questionName: 'predicate-currency',
          scalar: 'currency',
          operator: 'is less than',
          values: ['10', '20'],
        },
        // Question itself is a number question (single scalar answer)
        // but we specify multiple values for comparison.
        {
          questionName: 'list of longs',
          scalar: 'number',
          operator: 'is one of',
          values: ['1,2', '3,4'],
        },
        // Question itself is a text question (single scalar answer)
        // but we specify multiple values for comparison.
        {
          questionName: 'list of strings',
          scalar: 'text',
          operator: 'is not one of',
          values: ['one,two', 'three,four'],
        },
      ])

      let predicateDisplay = await page.innerText('.cf-display-predicate')
      await validateScreenshot(
        page,
        'eligibility-predicates-multi-values-multi-questions-predicate-saved',
      )
      expect(predicateDisplay).toContain('Screen 1 is eligible if any of:')
      expect(predicateDisplay).toContain(
        '"predicate-currency" currency is less than $10.00',
      )
      expect(predicateDisplay).toContain(
        '"predicate-date-is-earlier-than" date is earlier than 2021-01-01',
      )
      expect(predicateDisplay).toContain(
        '"predicate-currency" currency is less than $20.00',
      )
      expect(predicateDisplay).toContain(
        '"predicate-date-is-earlier-than" date is earlier than 2022-02-02',
      )

      await adminPredicates.clickEditPredicateButton('eligibility')
      await validateScreenshot(
        page,
        'eligibility-predicates-multi-values-multi-questions-predicate-edit',
      )

      await adminPredicates.configurePredicate({
        questionName: 'predicate-currency',
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
        '"predicate-currency" currency is greater than $100.00',
      )
      expect(predicateDisplay).toContain(
        '"predicate-currency" currency is greater than $200.00',
      )
    })

    test('visibility multiple values and multiple questions', async ({page, adminPrograms, adminPredicates} ) => {
      await loginAsAdmin(page)

      const programName = 'Test multiple question and value predicate config'
      await adminPrograms.addProgram(programName)

      await adminPrograms.editProgramBlock(programName, 'test-block', [
        'predicate-date-is-earlier-than',
        'predicate-currency',
      ])

      await adminPrograms.addProgramBlock(programName, 'show-hide', [])

      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 2',
      )

      await adminPredicates.addPredicates([
        {
          questionName: 'predicate-date-is-earlier-than',
          scalar: 'date',
          operator: 'is earlier than',
          values: ['2021-01-01', '2022-02-02'],
        },
        {
          questionName: 'predicate-currency',
          scalar: 'currency',
          operator: 'is less than',
          values: ['10', '20'],
        },
      ])

      let predicateDisplay = await page.innerText('.cf-display-predicate')
      await validateScreenshot(
        page,
        'visibility-predicates-multi-values-multi-questions-predicate-saved',
      )
      expect(predicateDisplay).toContain('Screen 2 is hidden if any of:')
      expect(predicateDisplay).toContain(
        '"predicate-currency" currency is less than $10.00',
      )
      expect(predicateDisplay).toContain(
        '"predicate-date-is-earlier-than" date is earlier than 2021-01-01',
      )
      expect(predicateDisplay).toContain(
        '"predicate-currency" currency is less than $20.00',
      )
      expect(predicateDisplay).toContain(
        '"predicate-date-is-earlier-than" date is earlier than 2022-02-02',
      )

      await adminPredicates.clickEditPredicateButton('visibility')
      await validateScreenshot(
        page,
        'visibility-predicates-multi-values-multi-questions-predicate-edit',
      )

      await adminPredicates.configurePredicate({
        questionName: 'predicate-currency',
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
        '"predicate-currency" currency is greater than $100.00',
      )
      expect(predicateDisplay).toContain(
        '"predicate-currency" currency is greater than $200.00',
      )
    })

    test('every visibility right hand type evaluates correctly', async ({page, adminPrograms, applicantQuestions, adminPredicates}) => {
      await loginAsAdmin(page)

      const programName = 'Test all visibility predicate types'
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, 'string', [
        'single-string',
      ])
      await adminPrograms.addProgramBlock(programName, 'list of strings', [
        'list of strings',
      ])
      await adminPrograms.addProgramBlock(programName, 'long', ['single-long'])
      await adminPrograms.addProgramBlock(programName, 'list of longs', [
        'list of longs',
      ])
      await adminPrograms.addProgramBlock(programName, 'currency', [
        'predicate-currency',
      ])
      await adminPrograms.addProgramBlock(
        programName,
        'is earlier than date question',
        ['predicate-date-is-earlier-than'],
      )
      await adminPrograms.addProgramBlock(
        programName,
        'on or after date question',
        ['predicate-date-on-or-after'],
      )
      await adminPrograms.addProgramBlock(programName, 'two lists', [
        'both sides are lists',
      ])
      await adminPrograms.addProgramBlock(programName, 'last', [
        'depends on previous',
      ])

      // Simple string predicate
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 2',
      )
      await adminPredicates.addPredicate(
        'single-string',
        'shown if',
        'first name',
        'is not equal to',
        'hidden',
      )

      // Single string one of a list of strings
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 3',
      )
      await adminPredicates.addPredicate(
        'list of strings',
        'shown if',
        'text',
        'is one of',
        'blue, green',
      )

      // Simple long predicate
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 4',
      )
      await adminPredicates.addPredicate(
        'single-long',
        'shown if',
        'number',
        'is equal to',
        '42',
      )

      // Single long one of a list of longs
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 5',
      )
      await adminPredicates.addPredicate(
        'list of longs',
        'shown if',
        'number',
        'is one of',
        '123, 456',
      )

      // Currency predicate
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 6',
      )
      await adminPredicates.addPredicate(
        'predicate-currency',
        'shown if',
        'currency',
        'is greater than',
        '100.01',
      )

      // Date predicate is before
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 7',
      )
      await adminPredicates.addPredicate(
        'predicate-date-is-earlier-than',
        'shown if',
        'date',
        'is earlier than',
        '2021-01-01',
      )

      // Date predicate is on or after
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 8',
      )
      await adminPredicates.addPredicate(
        'predicate-date-on-or-after',
        'shown if',
        'date',
        'is on or later than',
        '2023-01-01',
      )

      // Lists of strings on both sides (multi-option question checkbox)
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 9',
      )
      await adminPredicates.addPredicate(
        'both sides are lists',
        'shown if',
        'selections',
        'contains any of',
        'dog,cat',
      )

      await adminPrograms.publishProgram(programName)

      // Switch to applicantQuestions.view - if they answer each question according to the predicate,
      // the next screen will be shown.
      await logout(page)
      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(programName)

      // For each condition:
      // - submit an invalid option
      // - verify the other screens aren't show and the review page is shown
      // - go back
      // - enter an allowed value

      // "hidden" first name is not allowed.
      await applicantQuestions.answerNameQuestion('hidden', 'next', 'screen')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerNameQuestion('show', 'next', 'screen')
      await applicantQuestions.clickNext()

      // "blue" or "green" are allowed.
      await applicantQuestions.answerTextQuestion('red')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerTextQuestion('blue')
      await applicantQuestions.clickNext()

      // 42 is allowed.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('42')
      await applicantQuestions.clickNext()

      // 123 or 456 are allowed.
      await applicantQuestions.answerNumberQuestion('11111')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerNumberQuestion('123')
      await applicantQuestions.clickNext()

      // Greater than 100.01 is allowed
      await applicantQuestions.answerCurrencyQuestion('100.01')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerCurrencyQuestion('100.02')
      await applicantQuestions.clickNext()

      // Earlier than 2021-01-01 is allowed
      await applicantQuestions.answerDateQuestion('2021-01-01')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2020-12-31')
      await applicantQuestions.clickNext()

      // On or later than 2023-01-01 is allowed
      await applicantQuestions.answerDateQuestion('2022-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2023-01-01')
      await applicantQuestions.clickNext()

      // "dog" or "cat" are allowed.
      await applicantQuestions.answerCheckboxQuestion(['rabbit'])
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()
      await page.goBack()
      await applicantQuestions.answerCheckboxQuestion(['cat'])
      await applicantQuestions.clickNext()

      await applicantQuestions.answerTextQuestion('last one!')
      await applicantQuestions.clickNext()

      // We should now be on the summary page
      await applicantQuestions.submitFromReviewPage()
    })

    test('every eligibility right hand type evaluates correctly', async ( {page, adminPrograms, applicantQuestions, adminPredicates}) => {
      await enableFeatureFlag(page, 'save_on_all_actions')

      await loginAsAdmin(page)

      const programName = 'Test all eligibility predicate types'
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, 'string', [
        'single-string',
      ])
      await adminPrograms.addProgramBlock(programName, 'list of strings', [
        'list of strings',
      ])
      await adminPrograms.addProgramBlock(programName, 'long', ['single-long'])
      await adminPrograms.addProgramBlock(programName, 'list of longs', [
        'list of longs',
      ])
      await adminPrograms.addProgramBlock(programName, 'currency', [
        'predicate-currency',
      ])
      await adminPrograms.addProgramBlock(
        programName,
        'is earlier than date question',
        ['predicate-date-is-earlier-than'],
      )
      await adminPrograms.addProgramBlock(
        programName,
        'on or after date question',
        ['predicate-date-on-or-after'],
      )
      await adminPrograms.addProgramBlock(
        programName,
        'date question age is older than',
        ['predicate-date-age-older-than'],
      )
      await adminPrograms.addProgramBlock(
        programName,
        'date question age is younger than',
        ['predicate-date-age-younger-than'],
      )
      await adminPrograms.addProgramBlock(
        programName,
        'date question age is between',
        ['predicate-date-age-between'],
      )
      await adminPrograms.addProgramBlock(programName, 'two lists', [
        'both sides are lists',
      ])

      // Simple string predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
      )
      await adminPredicates.addPredicate(
        'single-string',
        /* action= */ null,
        'first name',
        'is not equal to',
        'hidden',
      )

      // Single string one of a list of strings
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 2',
      )
      await adminPredicates.addPredicate(
        'list of strings',
        /* action= */ null,
        'text',
        'is one of',
        'blue, green',
      )

      // Simple long predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 3',
      )
      await adminPredicates.addPredicate(
        'single-long',
        /* action= */ null,
        'number',
        'is equal to',
        '42',
      )

      // Single long one of a list of longs
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 4',
      )
      await adminPredicates.addPredicate(
        'list of longs',
        /* action= */ null,
        'number',
        'is one of',
        '123, 456',
      )

      // Currency predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 5',
      )
      await adminPredicates.addPredicate(
        'predicate-currency',
        /* action= */ null,
        'currency',
        'is greater than',
        '100.01',
      )

      // Date predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 6',
      )
      await adminPredicates.addPredicate(
        'predicate-date-is-earlier-than',
        /* action= */ null,
        'date',
        'is earlier than',
        '2021-01-01',
      )

      // Date predicate is on or after
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 7',
      )
      await adminPredicates.addPredicate(
        'predicate-date-on-or-after',
        /* action= */ null,
        'date',
        'is on or later than',
        '2023-01-01',
      )

      // Date predicate age is greater than
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 8',
      )
      await adminPredicates.addPredicate(
        'predicate-date-age-older-than',
        /* action= */ null,
        'date',
        'age is older than',
        '90',
      )

      // ensure the edit page renders without errors
      await adminPredicates.clickEditPredicateButton('eligibility')
      expect(await page.innerText('h1')).toContain(
        'Configure eligibility conditions',
      )
      await validateScreenshot(page, 'predicate-age-greater-than-edit')
      await adminPredicates.clickSaveConditionButton()

      // Date predicate age is less than
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 9',
      )
      await adminPredicates.addPredicate(
        'predicate-date-age-younger-than',
        /* action= */ null,
        'date',
        'age is younger than',
        '50',
      )

      // ensure the edit page renders without errors
      await adminPredicates.clickEditPredicateButton('eligibility')
      expect(await page.innerText('h1')).toContain(
        'Configure eligibility conditions',
      )
      await adminPredicates.clickSaveConditionButton()

      // Date predicate age is between
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 10',
      )
      await adminPredicates.addPredicate(
        'predicate-date-age-between',
        /* action= */ null,
        'date',
        'age is between',
        '1,90',
      )

      // ensure the edit page renders without errors
      await adminPredicates.clickEditPredicateButton('eligibility')
      expect(await page.innerText('h1')).toContain(
        'Configure eligibility conditions',
      )
      await validateScreenshot(page, 'predicate-age-between-edit')
      await adminPredicates.clickSaveConditionButton()

      // Lists of strings on both sides (multi-option question checkbox)
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 11',
      )
      await adminPredicates.addPredicate(
        'both sides are lists',
        /* action= */ null,
        'selections',
        'contains any of',
        'dog,cat',
      )

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
      await applicantQuestions.clickReviewWithoutSaving()
      await validateScreenshot(page, 'review-page-no-ineligible-banner')
      await validateToastMessage(page, '')
      await applicantQuestions.clickContinue()

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

      // Age greater than 90 is allowed
      await applicantQuestions.answerDateQuestion('2022-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('1930-01-01')
      await applicantQuestions.clickNext()

      // Age less than 50 is allowed
      await applicantQuestions.answerDateQuestion('1930-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2022-01-01')
      await applicantQuestions.clickNext()

      // Age between 1 and 90 is allowed
      await applicantQuestions.answerDateQuestion('1920-12-31')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await applicantQuestions.expectIneligibleQuestion('date question text')
      await applicantQuestions.expectIneligibleQuestionsCount(1)
      await page.goBack()
      await applicantQuestions.answerDateQuestion('2000-01-01')
      await applicantQuestions.clickNext()

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

      // We should now be on the review page with a completed form and no banner should show.
      await validateScreenshot(
        page,
        'review-page-no-ineligible-banner-completed',
      )
      await validateToastMessage(page, '')
      await applicantQuestions.submitFromReviewPage()
    })

    test('multiple questions ineligible', async ({page, adminPrograms, adminPredicates, applicantQuestions}) => {
      await loginAsAdmin(page)
      const programName = 'Multiple ineligible program'
      await adminPrograms.addProgram(programName)

      // Name predicate
      await adminPrograms.editProgramBlock(programName, 'name', [
        'single-string',
      ])
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
      )
      await adminPredicates.addPredicate(
        'single-string',
        /* action= */ null,
        'first name',
        'is not equal to',
        'hidden',
      )

      // Currency predicate
      await adminPrograms.addProgramBlock(programName, 'currency', [
        'predicate-currency',
      ])
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 2',
      )
      await adminPredicates.addPredicate(
        'predicate-currency',
        /* action= */ null,
        'currency',
        'is greater than',
        '100.01',
      )

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
      await validateScreenshot(
        page,
        'ineligible-multiple-eligibility-questions',
      )
    })
  })
})
