import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'
import {waitForHtmxReady} from '../support/wait'
import {QuestionType} from '../support/admin_questions'
import {
  MultiValueSpec,
  SubconditionSpec,
  SubconditionValue,
} from '../support/admin_predicates'
import {assertNotNull} from '../support/helpers'

/**
 * Map of question types to that question type's corresponding testing data
 *    @param questionName: The (backend, admin) name of the question
 *    @param questionText: The question text displayed to the applicant
 *    @param firstValue: The default value to fill in an input field, or to be selected from a dropdown
 *    @param secondValue: The default value to fill in a second input field. Optional, for question types that support BETWEEN operators.
 *    @param defaultInputType: The default input type for the question type. Optional, for question types that use input tags.
 *    @param defaultInputMode: The default inputmode for the question type. Optional, for question types that use input tags.
 */
const PROGRAM_SAMPLE_QUESTIONS = new Map<
  QuestionType,
  {
    questionName: string
    questionText: string
    firstValue: string
    secondValue?: string
    defaultInputType?: string
    defaultInputMode?: string
    multiValueOptions?: MultiValueSpec[]
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
    QuestionType.CHECKBOX,
    {
      questionName: 'checkbox-q',
      questionText: 'checkbox question text',
      firstValue: 'N/A',
      multiValueOptions: [
        {adminName: 'pizza-bagel', text: 'Pizza Bagel', checked: true},
        {adminName: 'bagel-pizza', text: 'Bagel Pizza', checked: true},
        {adminName: 'pizza-pizza', text: 'Pizza Pizza', checked: true},
        {adminName: 'bagel-bagel', text: 'Bagel Bagel', checked: true},
      ],
    },
  ],
  [
    QuestionType.CURRENCY,
    {
      questionName: 'currency-q',
      questionText: 'currency question text',
      firstValue: '3.50',
      secondValue: '4.75',
      defaultInputType: 'number',
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
    QuestionType.DROPDOWN,
    {
      questionName: 'dropdown-q',
      questionText: 'dropdown question text',
      firstValue: 'N/A',
      multiValueOptions: [
        {adminName: 'pizza-bagel', text: 'Pizza Bagel', checked: true},
        {adminName: 'bagel-pizza', text: 'Bagel Pizza', checked: true},
        {adminName: 'pizza-pizza', text: 'Pizza Pizza', checked: true},
        {adminName: 'bagel-bagel', text: 'Bagel Bagel', checked: true},
      ],
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
    QuestionType.ID,
    {
      questionName: 'id-q',
      questionText: 'id question text',
      firstValue: 'A123456-ID',
      defaultInputType: 'text',
      defaultInputMode: 'text',
    },
  ],
  [
    QuestionType.NAME,
    {
      questionName: 'name-q',
      questionText: 'name question text',
      firstValue: 'Keanu',
      defaultInputType: 'text',
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
  [
    QuestionType.RADIO,
    {
      questionName: 'radio-q',
      questionText: 'radio question text',
      firstValue: 'N/A',
      multiValueOptions: [
        {adminName: 'pizza-bagel', text: 'Pizza Bagel', checked: true},
        {adminName: 'bagel-pizza', text: 'Bagel Pizza', checked: true},
        {adminName: 'pizza-pizza', text: 'Pizza Pizza', checked: true},
        {adminName: 'bagel-bagel', text: 'Bagel Bagel', checked: true},
      ],
    },
  ],
  [
    QuestionType.TEXT,
    {
      questionName: 'text-q',
      questionText: 'text question text',
      firstValue: 'apple',
      defaultInputType: 'text',
      defaultInputMode: 'text',
    },
  ],
])

test.describe('create and edit predicates', () => {
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

    await test.step('Navigate to edit predicate and save empty predicate', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.clickSaveAndExitButton()

      // Validate no predicate is saved
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

    // This step is needed, because sequentially changing question types seems to trip up inline-style checkers.
    await test.step('refresh page and re-add condition', async () => {
      await page.reload()
      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(1)
    })

    await test.step('Add a subcondition', async () => {
      await adminPredicates.clickAddSubconditionButton(/* conditionId= */ 1)
      await waitForHtmxReady(page)
      await adminPredicates.expectSubcondition(1, 2)
      await validateScreenshot(
        page.getByTestId('condition-1'),
        'condition-with-multiple-subconditions',
      )
    })

    await test.step('refresh page and add two conditions', async () => {
      await page.reload()
      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(1)
      await waitForHtmxReady(page)

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(2)
      await waitForHtmxReady(page)

      await validateScreenshot(
        page.locator('#predicate-conditions-list'),
        'multiple-conditions',
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
    const questionName = 'predicate-q'
    const questionText = 'Text question'

    await test.step('Create a program with a question to use in the predicate', async () => {
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

    await test.step('Validate eligibility null state', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.expectEligibilityNullState()
    })

    await test.step('Add condition', async () => {
      await adminPredicates.clickAddConditionButton()

      await waitForHtmxReady(page)

      await adminPredicates.expectCondition(1)
      await adminPredicates.expectDeleteAllConditionsButton()
      await validateScreenshot(
        page.locator('#edit-predicate'),
        'edit-eligibility-predicate',
      )
    })

    await test.step('Select question, save, and check predicate validation', async () => {
      await adminPredicates.selectQuestion(1, 1, questionText)

      await adminPredicates.clickSaveAndExitButton()

      await expect(page.locator('#edit-predicate')).toContainText(
        'Error: This field is required.',
      )
      await validateScreenshot(
        page.locator('#condition-1'),
        'edit-eligibility-predicate-with-validation-error',
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

      await expect(
        page.locator('#predicate-operator-node-select-null-state'),
      ).toBeVisible()
      await expect(page.locator('#predicate-operator-node-select')).toBeHidden()
      await expect(
        page.locator('#predicate-operator-node-select-null-state'),
      ).toContainText('This screen is always shown')
      await adminPredicates.expectNoDeleteAllConditionsButton()
      await validateScreenshot(
        page.locator('#edit-predicate'),
        'visibility-predicate-null-state',
      )
    })

    await test.step('Add condition', async () => {
      await adminPredicates.clickAddConditionButton()

      await adminPredicates.expectCondition(1)
      await adminPredicates.expectDeleteAllConditionsButton()
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

    await test.step('Create program and add questions', async () => {
      for (const [questionType, questionData] of PROGRAM_SAMPLE_QUESTIONS) {
        await adminQuestions.addQuestionForType(
          questionType,
          questionData.questionName,
          questionData.questionText,
          questionData.multiValueOptions,
        )
      }
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: PROGRAM_SAMPLE_QUESTIONS.values()
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
      QuestionType.ID,
      QuestionType.NAME,
      QuestionType.NUMBER,
      QuestionType.TEXT,
    ]) {
      const singleValueOperator =
        questionType === QuestionType.DATE ? 'IS_AFTER' : 'EQUAL_TO'
      const questionData = PROGRAM_SAMPLE_QUESTIONS.get(questionType)!

      await test.step(`Select ${questionType} question and validate single-value operator behavior`, async () => {
        await adminPredicates.configureSubcondition({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          operator: singleValueOperator,
          value: {firstValue: questionData.firstValue},
        })

        await adminPredicates.expectSubconditionEquals({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          operator: singleValueOperator,
          value: {firstValue: questionData.firstValue},
        })

        const inputElementLocator = page.locator(
          `#condition-1-subcondition-1-value[type=${questionData.defaultInputType!}]`,
        )

        await expect(inputElementLocator).toHaveAttribute(
          'type',
          questionData.defaultInputType!,
        )
        await expect(inputElementLocator).toHaveAttribute(
          'inputmode',
          questionData.defaultInputMode!,
        )
      })

      await test.step('Enter an empty string and check validation behavior', async () => {
        await adminPredicates.fillValue(1, 1, {
          firstValue: ' ',
        } as SubconditionValue)

        await adminPredicates.clickSaveAndExitButton()
        await waitForHtmxReady(page)

        const errorMessageLocator = page.locator(
          '#condition-1-subcondition-1-errorMessage',
        )
        const inputElementLocator = page.locator(
          `#condition-1-subcondition-1-value[type=${questionData.defaultInputType!}]`,
        )

        await expect(errorMessageLocator).toContainText(
          'Error: This field is required.',
        )
        await expect(errorMessageLocator).toBeVisible()
        await expect(inputElementLocator).toHaveAttribute(
          'aria-invalid',
          'true',
        )
        if (questionType === QuestionType.CURRENCY) {
          await expect(page.locator('.usa-input-group--error')).toBeVisible()
        } else {
          await expect(inputElementLocator).toContainClass('usa-input--error')
        }
      })
    }

    // Test question types that allow multiple input fields with the BETWEEN operator
    for (const questionType of [
      QuestionType.CURRENCY,
      QuestionType.DATE,
      QuestionType.NUMBER,
    ]) {
      const questionData = PROGRAM_SAMPLE_QUESTIONS.get(questionType)!

      await test.step(`Select ${questionType} question and validate BETWEEN operator behavior`, async () => {
        await adminPredicates.configureSubcondition({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          operator: 'BETWEEN',
          value: {
            firstValue: questionData.firstValue,
            secondValue: questionData.secondValue!,
          },
        })

        await adminPredicates.expectSubconditionEquals({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          operator: 'BETWEEN',
          value: {
            firstValue: questionData.firstValue,
            secondValue: questionData.secondValue!,
          },
        })

        const inputElementLocator = page.locator(
          `#condition-1-subcondition-1-value[type=${questionData.defaultInputType!}]:enabled`,
        )
        const secondInputElementLocator = page.locator(
          `#condition-1-subcondition-1-secondValue[type=${questionData.defaultInputType!}]:enabled`,
        )

        await expect(inputElementLocator).toHaveAttribute(
          'inputmode',
          questionData.defaultInputMode!,
        )
        await expect(secondInputElementLocator).toHaveAttribute(
          'inputmode',
          questionData.defaultInputMode!,
        )
      })

      await test.step('Enter an empty string in the second input and check validation behavior', async () => {
        await adminPredicates.fillValue(1, 1, {
          firstValue: questionData.firstValue,
          secondValue: ' ',
        } as SubconditionValue)

        await adminPredicates.clickSaveAndExitButton()
        await waitForHtmxReady(page)

        const errorMessageLocator = page.locator(
          '#condition-1-subcondition-1-errorMessage',
        )
        const inputElementLocator = page.locator(
          `#condition-1-subcondition-1-value[type=${questionData.defaultInputType!}]`,
        )
        const secondInputElementLocator = page.locator(
          `#condition-1-subcondition-1-secondValue[type=${questionData.defaultInputType!}]`,
        )

        await expect(errorMessageLocator).toContainText(
          'Error: This field is required.',
        )
        await expect(errorMessageLocator).toBeVisible()
        await expect(inputElementLocator).toHaveAttribute(
          'aria-invalid',
          'false',
        )
        await expect(secondInputElementLocator).toHaveAttribute(
          'aria-invalid',
          'true',
        )
        if (questionType === QuestionType.CURRENCY) {
          await expect(page.locator('.usa-input-group--error')).toBeVisible()
        } else {
          await expect(inputElementLocator).not.toContainClass(
            'usa-input--error',
          )
          await expect(secondInputElementLocator).toContainClass(
            'usa-input--error',
          )
        }
      })
    }

    await test.step('Select date question and validate age operator behavior', async () => {
      const questionData = PROGRAM_SAMPLE_QUESTIONS.get(QuestionType.DATE)!
      await adminPredicates.configureSubcondition({
        conditionId: 1,
        subconditionId: 1,
        questionText: questionData.questionText,
        operator: 'AGE_BETWEEN',
        value: {
          firstValue: '18',
          secondValue: '25',
        },
      })

      await adminPredicates.expectSubconditionEquals({
        conditionId: 1,
        subconditionId: 1,
        questionText: questionData.questionText,
        operator: 'AGE_BETWEEN',
        value: {
          firstValue: '18',
          secondValue: '25',
        },
      })

      await expect(
        page.locator(
          '#condition-1-subcondition-1-value[type="number"]:enabled',
        ),
      ).toHaveCount(1)

      await expect(
        page.locator(
          '#condition-1-subcondition-1-secondValue[type="number"]:enabled',
        ),
      ).toHaveCount(1)
    })

    // Test question types that allow CSV inputs with the IN / NOT_IN operators
    for (const questionType of [
      QuestionType.DATE,
      QuestionType.EMAIL,
      QuestionType.ID,
      QuestionType.NAME,
      QuestionType.NUMBER,
      QuestionType.TEXT,
    ]) {
      await test.step(`Select ${questionType} question and validate CSV operator behavior`, async () => {
        const questionData = PROGRAM_SAMPLE_QUESTIONS.get(questionType)!
        await adminPredicates.configureSubcondition({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          operator: 'IN',
          value: {firstValue: 'a,1,b,2,c,3,'},
        })

        await adminPredicates.expectSubconditionEquals({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          operator: 'IN',
          value: {firstValue: 'a,1,b,2,c,3,'},
        })

        const valueHintTextLocator = page.locator(
          '#condition-1-subcondition-1-valueHintText',
        )
        const inputElementLocator = page.locator(
          '#condition-1-subcondition-1-value[type="text"]:enabled',
        )

        await expect(valueHintTextLocator).toBeVisible()
        await expect(valueHintTextLocator).toHaveText(
          'Enter a list of comma-seperated values. For example, "item1,item2,item3".',
        )
        await expect(inputElementLocator).toHaveAttribute('inputmode', 'text')
      })
    }

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
      const questionData = PROGRAM_SAMPLE_QUESTIONS.get(QuestionType.ADDRESS)!
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
    })

    // Test multiple option question types
    for (const questionType of [
      QuestionType.CHECKBOX,
      QuestionType.DROPDOWN,
      QuestionType.RADIO,
    ]) {
      const questionData = PROGRAM_SAMPLE_QUESTIONS.get(questionType)!

      await test.step(`Select ${questionType} question and validate multi-select operator behavior`, async () => {
        await adminPredicates.configureSubcondition({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          value: {multiValues: questionData.multiValueOptions!},
        })

        await adminPredicates.expectSubconditionEquals({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          value: {multiValues: questionData.multiValueOptions!},
        })
      })

      await test.step('Add and delete subcondition and expect no change', async () => {
        await adminPredicates.addAndExpectSubcondition(1, 2)
        await adminPredicates.deleteAndExpectNoSubcondition(1, 2)

        await adminPredicates.expectSubconditionEquals({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          value: {multiValues: questionData.multiValueOptions!},
        })
      })

      await test.step('Delete and re-add first condition', async () => {
        await adminPredicates.clickDeleteConditionButton(1)
        await adminPredicates.clickAddConditionButton(1)
      })

      await test.step('Select nothing and check validation behavior', async () => {
        await adminPredicates.configureSubcondition({
          conditionId: 1,
          subconditionId: 1,
          questionText: questionData.questionText,
          value: {multiValues: [] as MultiValueSpec[]},
        })

        await adminPredicates.clickSaveAndExitButton()
        await waitForHtmxReady(page)

        const errorMessageLocator = page.locator(
          '#condition-1-subcondition-1-errorMessage',
        )

        await expect(errorMessageLocator).toContainText(
          'Error: You must select at least one option.',
        )
        await expect(errorMessageLocator).toBeVisible()
        await expect(page.locator('.usa-form-group--error')).toBeVisible()
      })
    }
  })

  test('Delete conditions', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    const nameQuestionValues = PROGRAM_SAMPLE_QUESTIONS.get(QuestionType.NAME)!
    const dateQuestionValues = PROGRAM_SAMPLE_QUESTIONS.get(QuestionType.DATE)!

    await loginAsAdmin(page)
    const programName = 'Create and edit a new predicate'

    await test.step('Create a program with a question to use in the predicate', async () => {
      await adminQuestions.addNameQuestion({
        questionName: nameQuestionValues.questionName,
        questionText: nameQuestionValues.questionText,
      })
      await adminQuestions.addDateQuestion({
        questionName: dateQuestionValues.questionName,
        questionText: dateQuestionValues.questionText,
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [
          {name: nameQuestionValues.questionName},
          {name: dateQuestionValues.questionName},
        ],
      })
    })

    await test.step('Add eligibility condition', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(1)

      await expect(
        page.locator('#condition-1-subcondition-1-question'),
      ).toBeFocused()
      await expect(
        page.locator('#condition-1-subcondition-1-ariaAnnounce'),
      ).toHaveAttribute('data-should-announce', 'true')
      await adminPredicates.selectConditionLogicalOperator(1, 'OR')
    })

    await test.step('Add second subcondition, select questions, and enter input', async () => {
      await adminPredicates.addAndExpectSubcondition(1, 2)
      await adminPredicates.expectConditionLogicalOperatorValues(1, 'OR')

      await adminPredicates.configureSubcondition({
        conditionId: 1,
        subconditionId: 1,
        questionText: nameQuestionValues.questionText,
        scalar: 'LAST_NAME',
        operator: 'EQUAL_TO',
        value: {firstValue: nameQuestionValues.firstValue},
      })

      await adminPredicates.configureSubcondition({
        conditionId: 1,
        subconditionId: 2,
        questionText: dateQuestionValues.questionText,
        operator: 'EQUAL_TO',
        value: {firstValue: dateQuestionValues.firstValue},
      })

      await waitForHtmxReady(page)
    })

    await test.step('Delete second subcondition and no change to first', async () => {
      await adminPredicates.deleteAndExpectNoSubcondition(1, 2)
      await adminPredicates.expectSubcondition(1, 1)

      await adminPredicates.expectSubconditionEquals({
        conditionId: 1,
        subconditionId: 1,
        questionText: nameQuestionValues.questionText,
        scalar: 'LAST_NAME',
        operator: 'EQUAL_TO',
        value: {firstValue: nameQuestionValues.firstValue},
      })
    })

    await test.step('Add second subcondition, select question, and enter values', async () => {
      await adminPredicates.addAndExpectSubcondition(1, 2)
      await adminPredicates.expectConditionLogicalOperatorValues(1, 'OR')

      await adminPredicates.configureSubcondition({
        conditionId: 1,
        subconditionId: 2,
        questionText: dateQuestionValues.questionText,
        operator: 'BETWEEN',
        value: {
          firstValue: dateQuestionValues.firstValue,
          secondValue: dateQuestionValues.secondValue!,
        },
      })
    })

    await test.step('Delete first subcondition - second becomes first', async () => {
      await adminPredicates.clickDeleteSubconditionButton(1, 1)
      await adminPredicates.expectSubcondition(1, 1)
      await adminPredicates.expectNoSubcondition(1, 2)
      await adminPredicates.expectAddSubconditionButton(1)

      await adminPredicates.expectSubconditionEquals({
        conditionId: 1,
        subconditionId: 1,
        questionText: dateQuestionValues.questionText,
        operator: 'BETWEEN',
        value: {
          firstValue: dateQuestionValues.firstValue,
          secondValue: dateQuestionValues.secondValue,
        },
      })
      await expect(
        page.locator('#condition-1-subcondition-1-ariaAnnounce'),
      ).toHaveAttribute('data-should-announce', 'true')
    })

    await test.step('Delete condition and validate null state', async () => {
      await adminPredicates.clickDeleteConditionButton(1)

      await waitForHtmxReady(page)

      await adminPredicates.expectEligibilityNullState()

      await validateScreenshot(
        page.locator('#edit-predicate'),
        'eligibility-predicate-null-state',
      )
    })

    await test.step('Add two eligibility conditions and select questions', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(1)
      await adminPredicates.selectQuestion(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        nameQuestionValues.questionText,
      )

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.expectCondition(2)
      await adminPredicates.selectQuestion(
        /* conditionId= */ 2,
        /* subconditionId= */ 1,
        dateQuestionValues.questionText,
      )
      await adminPredicates.selectRootLogicalOperator('OR')
    })

    await test.step('Delete first condition - second condition should become first', async () => {
      await adminPredicates.clickDeleteConditionButton(1)

      await waitForHtmxReady(page)

      await adminPredicates.expectCondition(1)
      await adminPredicates.expectNoCondition(2)

      await adminPredicates.expectAddConditionButton()
      await expect(
        page.locator('#predicate-operator-node-select-null-state'),
      ).toBeHidden()
      await expect(
        page.locator('#predicate-operator-node-select'),
      ).toBeVisible()
      await expect(
        page.locator('#condition-1-subcondition-1-question'),
      ).toContainText(dateQuestionValues.questionText)
      await expect(
        page.locator('#condition-1-subcondition-1-question'),
      ).toBeFocused()
      await expect(
        page.locator('#condition-1-subcondition-1-ariaAnnounce'),
      ).toHaveAttribute('data-should-announce', 'true')
    })

    await test.step('Add second condition, delete all conditions, and validate null state', async () => {
      await adminPredicates.addAndExpectCondition(2)
      await adminPredicates.expectRootLogicalOperatorValues('OR')

      await adminPredicates.clickDeleteAllConditionsButton()

      await adminPredicates.expectNoCondition(1)
      await adminPredicates.expectNoCondition(2)
      await adminPredicates.expectNoDeleteAllConditionsButton()
      await adminPredicates.expectAddConditionButton()
      await adminPredicates.expectEligibilityNullState()
    })
  })

  test('Save and restore predicate values', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')
    const programName = 'Saved and restored'

    const questionTypes: QuestionType[] = [
      QuestionType.ADDRESS,
      QuestionType.CHECKBOX,
      QuestionType.DATE,
      QuestionType.NAME,
    ]
    const testQuestionData = questionTypes
      .filter((key) => PROGRAM_SAMPLE_QUESTIONS.has(key))
      .map((key) => ({
        questionType: key,
        questionValue: PROGRAM_SAMPLE_QUESTIONS.get(key)!,
      }))

    const addressValues = assertNotNull(
      testQuestionData.find(
        (question) => question.questionType === QuestionType.ADDRESS,
      ),
    ).questionValue
    const checkboxValues = assertNotNull(
      testQuestionData.find(
        (question) => question.questionType === QuestionType.CHECKBOX,
      ),
    ).questionValue
    const dateValues = assertNotNull(
      testQuestionData.find(
        (question) => question.questionType === QuestionType.DATE,
      ),
    ).questionValue
    const nameValues = assertNotNull(
      testQuestionData.find(
        (question) => question.questionType === QuestionType.NAME,
      ),
    ).questionValue

    const subconditionConfigs: SubconditionSpec[] = [
      {
        conditionId: 1,
        subconditionId: 1,
        questionText: checkboxValues.questionText,
        value: {multiValues: checkboxValues.multiValueOptions!},
      },
      {
        conditionId: 1,
        subconditionId: 2,
        questionText: dateValues.questionText,
        operator: 'BETWEEN',
        value: {
          firstValue: dateValues.firstValue,
          secondValue: dateValues.secondValue!,
        },
      },
      {
        conditionId: 2,
        subconditionId: 1,
        questionText: nameValues.questionText,
        scalar: 'LAST_NAME',
        value: {firstValue: nameValues.firstValue},
      },
      {
        conditionId: 2,
        subconditionId: 2,
        questionText: addressValues.questionText,
        value: {},
      },
    ]

    await test.step('create a program and add questions', async () => {
      await adminPrograms.addProgram(programName)
      for (const question of testQuestionData) {
        await adminQuestions.addQuestionForType(
          question.questionType,
          question.questionValue.questionName,
          question.questionValue.questionText,
          question.questionValue.multiValueOptions,
        )
      }

      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: testQuestionData.map((questionData) => ({
          name: questionData.questionValue.questionName,
        })),
      })
    })

    await test.step('enable address correction', async () => {
      await adminPrograms.goToBlockInProgram(programName, 'Screen 1')

      await adminPrograms.clickAddressCorrectionToggle()
      await expect(adminPrograms.getAddressCorrectionToggle()).toHaveValue(
        'true',
      )
    })

    await test.step('fill eligibility predicate conditions and values', async () => {
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      // Add conditions and subconditions
      await adminPredicates.addAndExpectCondition(1)
      await adminPredicates.addAndExpectSubcondition(1, 2)

      await adminPredicates.addAndExpectCondition(2)
      await adminPredicates.addAndExpectSubcondition(2, 2)

      await adminPredicates.configureSubconditions(subconditionConfigs)
    })

    await test.step('select logical operators', async () => {
      await adminPredicates.selectRootLogicalOperator('OR')
      await adminPredicates.selectConditionLogicalOperator(1, 'OR')
    })

    await test.step('validate state', async () => {
      await adminPredicates.expectConditionAndSubconditions(1, [1, 2])
      await adminPredicates.expectConditionAndSubconditions(2, [1, 2])
      await adminPredicates.expectRootLogicalOperatorValues('OR')

      // Checking values
      await expect(
        page
          .getByLabel('Value(s)', {id: 'condition-2-subcondition-2-value'})
          .locator(`option[value="Seattle"]`),
      ).not.toHaveAttribute('hidden')

      await adminPredicates.expectSubconditionsEqual(subconditionConfigs)
      await adminPredicates.expectRootLogicalOperatorValues('OR')
      await adminPredicates.expectConditionLogicalOperatorValues(1, 'OR')
      await adminPredicates.expectConditionLogicalOperatorValues(2, 'AND')
    })

    await test.step('save and reload', async () => {
      await adminPredicates.clickSaveAndExitButton()
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )
    })

    await test.step('re-validate state', async () => {
      await adminPredicates.expectConditionAndSubconditions(1, [1, 2])
      await adminPredicates.expectConditionAndSubconditions(2, [1, 2])
      await adminPredicates.expectRootLogicalOperatorValues('OR')

      // Checking values
      await expect(
        page
          .getByLabel('Value(s)', {id: 'condition-2-subcondition-2-value'})
          .locator(`option[value="Seattle"]`),
      ).not.toHaveAttribute('hidden')

      await adminPredicates.expectSubconditionsEqual(subconditionConfigs)
      await adminPredicates.expectRootLogicalOperatorValues('OR')
      await adminPredicates.expectConditionLogicalOperatorValues(1, 'OR')
      await adminPredicates.expectConditionLogicalOperatorValues(2, 'AND')
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
      await adminPredicates.expectNoDeleteAllConditionsButton()
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
      await adminPredicates.expectNoDeleteAllConditionsButton()
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
      await adminPredicates.expectNoDeleteAllConditionsButton()
      await validateScreenshot(page, 'no-available-visibility-questions')
    })
  })

  test('Eligibility message', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Eligibility message'
    const eligibilityMessageLabel =
      'Display message shown to ineligible applicants'
    const questionValues = assertNotNull(
      PROGRAM_SAMPLE_QUESTIONS.get(QuestionType.DATE),
    )

    await test.step('Create a program', async () => {
      await adminQuestions.addDateQuestion({
        questionName: questionValues.questionName,
        questionText: questionValues.questionText,
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [{name: questionValues.questionName}],
      })
    })

    await test.step('Navigate to edit predicate and verify empty state', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await expect(page.getByLabel(eligibilityMessageLabel)).toBeVisible()
      await expect(page.getByLabel(eligibilityMessageLabel)).toBeEmpty()
    })

    await test.step('Set eligibility message', async () => {
      const eligibilityMessage = 'You are not eligible for this program.'
      await page.getByLabel(eligibilityMessageLabel).fill(eligibilityMessage)

      await adminPredicates.clickSaveAndExitButton()
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await expect(page.getByLabel(eligibilityMessageLabel)).toHaveValue(
        eligibilityMessage,
      )
    })

    await test.step('Remove eligibility message', async () => {
      await page.getByLabel(eligibilityMessageLabel).fill('')

      await adminPredicates.clickSaveAndExitButton()
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await expect(page.getByLabel(eligibilityMessageLabel)).toBeEmpty()
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
      await page.route('**/hx/addCondition', async (route, request) => {
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

  test('Exit predicate edit without saving', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Create and edit an eligibility predicate'

    await test.step('create a program with a question to use in the predicate', async () => {
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

    await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
    const editBlockURL = page.url()

    await test.step('enter empty predicate and exit without making changes', async () => {
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        /* blockName= */ 'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      // Cancel button shouldn't show a dialog, and should navigate us back automatically
      await adminPredicates.clickCancelButton()
      // Expect us to navigate back, allowing for arbitrary query strings
      await expect(page).toHaveURL(new RegExp(`.*${editBlockURL}?.*`))
    })

    await test.step('enter empty predicate, add condition, and confirm to exit', async () => {
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        /* blockName= */ 'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      page.once('dialog', (dialog) => dialog.accept())
      const dialogEventPromise = page.waitForEvent('dialog')

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.clickCancelButton()

      const dialogEvent = await dialogEventPromise
      expect(dialogEvent.message().toString()).toContain(
        'You have unsaved changes that will be lost.',
      )
      // Expect us to navigate back, allowing for arbitrary query strings
      await expect(page).toHaveURL(new RegExp(`.*${editBlockURL}?.*`))
    })

    await test.step('enter empty predicate, add condition, and dismiss confirmation', async () => {
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        /* blockName= */ 'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )
      const editEligibilityURL = page.url()

      page.once('dialog', (dialog) => dialog.dismiss())
      const dialogEventPromise = page.waitForEvent('dialog')

      await adminPredicates.clickAddConditionButton()
      await adminPredicates.clickCancelButton()

      const dialogEvent = await dialogEventPromise
      expect(dialogEvent.message().toString()).toContain(
        'You have unsaved changes that will be lost.',
      )

      // We should stay on the edit eligibility predicate.
      await page.waitForURL(editEligibilityURL)
    })

    await test.step('enter empty predicate, add and then delete condition, no confirmation needed', async () => {
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        /* blockName= */ 'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )
      await adminPredicates.clickAddConditionButton()
      await adminPredicates.clickDeleteConditionButton(1)

      // Cancel button shouldn't show a dialog, and should navigate us back automatically
      await adminPredicates.clickCancelButton()
      // Expect us to navigate back, allowing for arbitrary query strings
      await expect(page).toHaveURL(new RegExp(`.*${editBlockURL}?.*`))
    })
  })
})
