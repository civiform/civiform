import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'
import {waitForHtmxReady} from '../support/wait'
import {QuestionType} from '../support/admin_questions'

test.describe('create and edit predicates', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'expanded_form_logic_enabled')
  })

  test('Create and edit a new predicate', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Create and edit a new predicate'
    const questionText = 'text question'

    await test.step('Create a program with a question to use in the predicate', async () => {
      const questionName = 'predicate-q'
      await adminQuestions.addTextQuestion({
        questionName: questionName,
        questionText: questionText,
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [{name: questionName}],
      })

      // Validate empty state without predicate
      await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
      await expect(page.locator('#eligibility-predicate')).toContainText(
        'This screen does not have any eligibility conditions',
      )
    })

    await test.step('Navigate to edit predicate and create a new condition', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(1)
      await validateScreenshot(page.getByTestId('condition-1'), 'new-condition')
    })

    await test.step('Choosing a question updates scalar and operator options', async () => {
      await adminPredicates.selectQuestion(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        questionText,
      )

      const operatorsForTextQuestion = [
        'EQUAL_TO',
        'IN',
        'NOT_EQUAL_TO',
        'NOT_IN',
      ]
      for (const operator of operatorsForTextQuestion) {
        await expect(
          page
            .getByLabel('State', {id: 'condition-1-subcondition-1-operator'})
            .locator(`option[value="${operator}"]`),
        ).not.toHaveAttribute('hidden')
      }

      const hiddenOperators = [
        'AGE_BETWEEN',
        'ANY_OF',
        'BETWEEN',
        'LESS_THAN',
        'IN_SERVICE_AREA',
      ]
      for (const operator of hiddenOperators) {
        await expect(
          page
            .getByLabel('State', {id: 'condition-1-subcondition-1-operator'})
            .locator(`option[value="${operator}"]`),
        ).toHaveAttribute('hidden')
      }

      await validateScreenshot(
        page.getByTestId('condition-1'),
        'condition-with-question-selected',
      )
    })

    await test.step('Add a subcondition', async () => {
      await adminPredicates.clickAddSubconditionButton(/* conditionId= */ 1)
      await adminPredicates.expectSubcondition(1, 2)
      await validateScreenshot(
        page.getByTestId('condition-1'),
        'condition-with-multiple-subconditions',
      )
    })
  })

  test('Create and edit an eligibility predicate', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Create and edit an eligibility predicate'

    await test.step('Create a program with a question to use in the predicate', async () => {
      const questionName = 'predicate-q'
      await adminQuestions.addTextQuestion({
        questionName: questionName,
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [{name: questionName}],
      })
    })

    await test.step('Validate empty state without predicate', async () => {
      await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
      await expect(page.locator('#eligibility-predicate')).toContainText(
        'This screen does not have any eligibility conditions',
      )
      await validateScreenshot(
        page.locator('#eligibility-predicate'),
        'eligibility-predicate-empty-state',
      )
    })

    await test.step('Edit eligibility predicate', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.clickAddConditionButton()

      await adminPredicates.expectCondition(1)
      await validateScreenshot(
        page.locator('#edit-predicate'),
        'edit-eligibility-predicate',
      )
    })
  })

  test('Create and edit a visibility predicate', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Create and edit a visibility predicate'

    await test.step('Create a program with a question to use in the predicate', async () => {
      const questionName = 'predicate-q'
      await adminQuestions.addTextQuestion({
        questionName: questionName,
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [{name: questionName}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 2',
        description: 'second screen',
        questions: [],
      })
    })

    await test.step('Validate empty state without predicate', async () => {
      await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
      await expect(page.locator('#visibility-predicate')).toContainText(
        'This screen is always shown',
      )
      await validateScreenshot(
        page.locator('#visibility-predicate'),
        'visibility-predicate-empty-state',
      )
    })

    await test.step('Edit visibility predicate', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 2',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.clickAddConditionButton()

      await adminPredicates.expectCondition(1)
      await validateScreenshot(
        page.locator('#edit-predicate'),
        'edit-visibility-predicate',
      )
    })
  })

  test('Populate predicate values across question types', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Populate predicate values across question types'

    /**
     * Map of question types to that question type's corresponding testing data
     *    @param questionName: The (backend, admin) name of the question
     *    @param questionText: The question text displayed to the applicant
     *    @param firstValue: The default value to fill in an input field, or to be selected from a dropdown
     *    @param secondValue: The default value to fill in a second input field. Optional, for question types that support BETWEEN operators.
     *    @param defaultInputType: The default input type for the question type. Optional, for question types that use input tags.
     *    @param defaultInputMode: The default inputmode for the question type. Optional, for question types that use input tags.
     */
    const programQuestions = new Map<
      QuestionType,
      {
        questionName: string
        questionText: string
        firstValue: string
        secondValue?: string
        defaultInputType?: string
        defaultInputMode?: string
      }
    >([
      [
        QuestionType.ADDRESS,
        {
          questionName: 'address-q',
          questionText: 'address question text',
          firstValue: 'Seattle',
        },
      ],
      [
        QuestionType.CURRENCY,
        {
          questionName: 'currency-q',
          questionText: 'currency question text',
          firstValue: '3.50',
          secondValue: '4.75',
          defaultInputType: 'currency',
          defaultInputMode: 'decimal',
        },
      ],
      [
        QuestionType.DATE,
        {
          questionName: 'date-q',
          questionText: 'date question text',
          firstValue: '1970-01-01',
          secondValue: '2000-01-01',
          defaultInputType: 'date',
          defaultInputMode: 'numeric',
        },
      ],
      [
        QuestionType.EMAIL,
        {
          questionName: 'email-q',
          questionText: 'email question text',
          firstValue: 'email@fake-email.gov',
          defaultInputType: 'email',
          defaultInputMode: 'text',
        },
      ],
      [
        QuestionType.NUMBER,
        {
          questionName: 'number-q',
          questionText: 'number question text',
          firstValue: '18',
          secondValue: '25',
          defaultInputType: 'number',
          defaultInputMode: 'decimal',
        },
      ],
    ])

    await test.step('Create program and add questions', async () => {
      for (const [questionType, questionData] of programQuestions) {
        await adminQuestions.addQuestionForType(
          questionType,
          questionData.questionName,
          questionData.questionText,
        )
      }
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: programQuestions
          .values()
          .map((questionData) => ({name: questionData.questionName}))
          .toArray(),
      })
    })

    await test.step('Add new condition', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(1)
    })

    // Test single-value operator on applicable question types.
    for (const questionType of [
      QuestionType.CURRENCY,
      QuestionType.DATE,
      QuestionType.EMAIL,
      QuestionType.NUMBER,
    ]) {
      await test.step(`Select ${questionType} question and validate single-value operator behavior`, async () => {
        const questionData = programQuestions.get(questionType)!
        await adminPredicates.selectQuestion(
          /* conditionId= */ 1,
          /* subconditionId= */ 1,
          questionData.questionText,
        )
        await adminPredicates.selectOperator(
          /* conditionId= */ 1,
          /* subconditionId= */ 1,
          'EQUAL_TO',
        )
        await waitForHtmxReady(page)

        const inputElementLocator = page.locator(
          `#condition-1-subcondition-1-value[type=${questionData.defaultInputType!}]`,
        )
        const secondInputElementLocator = page.locator(
          `#condition-1-subcondition-1-secondValue[type=${questionData.defaultInputType!}]`,
        )

        await expect(inputElementLocator).toBeVisible()
        await expect(secondInputElementLocator).toBeHidden()

        await expect(inputElementLocator).toHaveAttribute(
          'type',
          questionData.defaultInputType!,
        )
        await expect(inputElementLocator).toHaveAttribute(
          'inputmode',
          questionData.defaultInputMode!,
        )

        await inputElementLocator.fill(questionData.firstValue)
        await expect(inputElementLocator).toHaveValue(questionData.firstValue)

        await validateScreenshot(
          page.getByTestId('condition-1'),
          `single-value-with-${questionType}-question-selected`,
        )
      })

      // This step is needed, because sequentially changing question types seems to trip up inline-style checkers.
      await test.step('refresh page and re-add condition', async () => {
        await page.reload()
        await adminPredicates.clickAddConditionButton()
        await adminPredicates.expectCondition(1)
      })
    }

    // Test question types that allow multiple input fields with the BETWEEN operator
    for (const questionType of [
      QuestionType.CURRENCY,
      QuestionType.DATE,
      QuestionType.NUMBER,
    ]) {
      await test.step(`Select ${questionType} question and validate BETWEEN operator behavior`, async () => {
        const questionData = programQuestions.get(questionType)!
        await adminPredicates.selectQuestion(
          /* conditionId= */ 1,
          /* subconditionId= */ 1,
          questionData.questionText,
        )
        await adminPredicates.selectOperator(
          /* conditionId= */ 1,
          /* subconditionId= */ 1,
          'BETWEEN',
        )
        await waitForHtmxReady(page)

        const inputElementLocator = page.locator(
          `#condition-1-subcondition-1-value[type=${questionData.defaultInputType!}]`,
        )
        const secondInputElementLocator = page.locator(
          `#condition-1-subcondition-1-secondValue[type=${questionData.defaultInputType!}]`,
        )

        await expect(inputElementLocator).toBeVisible()
        await expect(secondInputElementLocator).toBeVisible()

        await expect(inputElementLocator).toHaveAttribute(
          'type',
          questionData.defaultInputType!,
        )
        await expect(inputElementLocator).toHaveAttribute(
          'inputmode',
          questionData.defaultInputMode!,
        )
        await expect(secondInputElementLocator).toHaveAttribute(
          'type',
          questionData.defaultInputType!,
        )
        await expect(secondInputElementLocator).toHaveAttribute(
          'inputmode',
          questionData.defaultInputMode!,
        )

        await inputElementLocator.fill(questionData.firstValue)
        await secondInputElementLocator.fill(questionData.secondValue!)
        await expect(inputElementLocator).toHaveValue(questionData.firstValue)
        await expect(secondInputElementLocator).toHaveValue(
          questionData.secondValue!,
        )

        await validateScreenshot(
          page.getByTestId('condition-1'),
          `multiple-values-with-${questionType}-question-selected`,
        )
      })

      // This step is needed, because sequentially changing question types seems to trip up inline-style checkers.
      await test.step('refresh page and re-add condition', async () => {
        await page.reload()
        await adminPredicates.clickAddConditionButton()
        await adminPredicates.expectCondition(1)
      })
    }

    await test.step('Select date question and validate age operator behavior', async () => {
      const questionData = programQuestions.get(QuestionType.DATE)!
      await adminPredicates.selectQuestion(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        questionData.questionText,
      )
      await adminPredicates.selectOperator(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        'AGE_BETWEEN',
      )

      const inputElementLocator = page.locator(
        '#condition-1-subcondition-1-value[type="number"]',
      )
      const secondInputElementLocator = page.locator(
        '#condition-1-subcondition-1-secondValue[type="number"]',
      )

      await expect(secondInputElementLocator).toBeVisible()
      await expect(secondInputElementLocator).toBeVisible()
      await inputElementLocator.fill('18')
      await secondInputElementLocator.fill('25')

      await validateScreenshot(
        page.getByTestId('condition-1'),
        `multiple-values-with-age-question-selected`,
      )
    })

    // Test question types that allow CSV inputs with the IN / NOT_IN operators
    for (const questionType of [
      QuestionType.DATE,
      QuestionType.EMAIL,
      QuestionType.NUMBER,
    ]) {
      await test.step('refresh page and re-add condition', async () => {
        await page.reload()
        await adminPredicates.clickAddConditionButton()
        await adminPredicates.expectCondition(1)
      })

      await test.step(`Select ${questionType} question and validate CSV operator behavior`, async () => {
        const questionData = programQuestions.get(questionType)!
        await adminPredicates.selectQuestion(
          /* conditionId= */ 1,
          /* subconditionId= */ 1,
          questionData.questionText,
        )

        await adminPredicates.selectOperator(
          /* conditionId= */ 1,
          /* subconditionId= */ 1,
          'IN',
        )

        const valueHintTextLocator = page.locator(
          '#condition-1-subcondition-1-valueHintText',
        )
        const inputElementLocator = page.locator(
          '#condition-1-subcondition-1-value[type="text"]',
        )

        await expect(valueHintTextLocator).toBeVisible()
        await expect(valueHintTextLocator).toHaveText(
          'Enter a list of comma-seperated values. For example, "item1,item2,item3".',
        )
        await expect(inputElementLocator).toHaveAttribute('type', 'text')
        await expect(inputElementLocator).toHaveAttribute('inputmode', 'text')
      })
    }

    await test.step('Validate value hint text screenshot', async () => {
      await validateScreenshot(
        page.locator('#condition-1-subcondition-1-valueHintText'),
        'value-hint-text',
      )
    })

    await test.step('Trigger address correction toggle and add new condition', async () => {
      await adminPrograms.goToBlockInProgram(programName, 'Screen 1')

      await adminPrograms.clickAddressCorrectionToggle()
      await expect(adminPrograms.getAddressCorrectionToggle()).toHaveValue(
        'true',
      )

      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(1)
    })

    await test.step('Choosing an address question updates value options', async () => {
      const questionData = programQuestions.get(QuestionType.ADDRESS)!
      await adminPredicates.selectQuestion(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        questionData.questionText,
      )
      await expect(
        page
          .getByLabel('Value(s)', {id: 'condition-1-subcondition-1-value'})
          .locator(`option[value="Seattle"]`),
      ).not.toHaveAttribute('hidden')

      await validateScreenshot(
        page.getByTestId('condition-1'),
        'values-with-address-question-selected',
      )
    })
  })

  test('No available questions on screen', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Create and edit a new predicate'

    await test.step('create a new program with no available questions', async () => {
      const questionName = 'file-question'
      await adminQuestions.addFileUploadQuestion({questionName: questionName})
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [{name: questionName}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 2',
        description: 'second screen',
        questions: [],
      })

      // Validate empty state without predicate
      await adminPrograms.goToBlockInProgram(programName, 'Screen 2')
      await expect(page.locator('#visibility-predicate')).toContainText(
        'This screen is always shown',
      )
    })

    await test.step('no edit eligibility button', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.expectNoAddConditionButton()
      await validateScreenshot(page, 'no-available-eligibility-questions')
    })

    await test.step('no edit visibility button', async () => {
      // Edit visibility predicate
      await adminPrograms.goToEditBlockVisibilityPredicatePage(
        programName,
        'Screen 2',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.expectNoAddConditionButton()
      await validateScreenshot(page, 'no-available-visibility-questions')
    })
  })

  test('Bad HTMX request', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Create and edit an eligibility predicate'

    await test.step('Create a program with a question to use in the predicate', async () => {
      const questionName = 'predicate-q'
      await adminQuestions.addTextQuestion({
        questionName: questionName,
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [{name: questionName}],
      })
    })

    await test.step('Add first predicate', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )
      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(1)
    })

    await test.step('Submit bad HTMX request', async () => {
      // Reformat the request URL to produce an HTMX failure
      await page.route('**/hx/editCondition', async (route, request) => {
        const newUrl = request
          .url()
          .toString()
          .replace('ELIGIBILITY', 'BLAHBLAHBLAH')
        const headers = {
          ...request.headers(),
          'hx-current-url': newUrl,
        }
        await route.continue({
          url: newUrl,
          headers,
        })
      })

      await adminPredicates.clickAddConditionButton()

      // Check that the correct alert is added to the DOM
      await waitForHtmxReady(page)
      await adminPredicates.expectHtmxError()
      await validateScreenshot(
        page.locator('.usa-alert--warning'),
        'htmx-alert-failed-request',
      )
    })
  })
})
