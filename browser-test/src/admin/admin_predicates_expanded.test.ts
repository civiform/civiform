import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'
import {waitForHtmxReady} from '../support/wait'

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

    await test.step('Validate eligibility null state', async () => {
      // Edit eligibility predicate
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
        /* expandedFormLogicEnabled= */ true,
      )

      await expect(
        page.locator('#predicate-operator-node-select-null-state'),
      ).toBeVisible()
      await expect(page.locator('#predicate-operator-node-select')).toBeHidden()
      await expect(
        page.locator('#predicate-operator-node-select-null-state'),
      ).toContainText('Applicant is always eligible')
      await validateScreenshot(
        page.locator('#edit-predicate'),
        'eligibility-predicate-null-state',
      )
    })

    await test.step('Add condition', async () => {
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

      await expect(
        page.locator('#predicate-operator-node-select-null-state'),
      ).toBeVisible()
      await expect(page.locator('#predicate-operator-node-select')).toBeHidden()
      await expect(
        page.locator('#predicate-operator-node-select-null-state'),
      ).toContainText('This screen is always shown')
      await validateScreenshot(
        page.locator('#edit-predicate'),
        'visibility-predicate-null-state',
      )
    })

    await test.step('Add condition', async () => {
      await adminPredicates.clickAddConditionButton()

      await adminPredicates.expectCondition(1)
      await validateScreenshot(
        page.locator('#edit-predicate'),
        'edit-visibility-predicate',
      )
    })
  })

  test(`Create service area predicate`, async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    // This test is separated out because address questions have special logic
    // for populating the value options.
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName =
      'Create and edit an eligibility predicate with address question'
    const questionText = 'address question'

    await test.step('Create a program with a question to use in the predicate', async () => {
      const questionName = 'address-q'
      await adminQuestions.addAddressQuestion({
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

    await test.step('Choosing a question updates value options', async () => {
      await adminPredicates.selectQuestion(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        questionText,
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

  test(`Create number predicate`, async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)

    const programName =
      'Create and edit an eligibility predicate with number question'
    const questionText = 'how many burritos have you eaten today?'

    await test.step('Create a program with a question to use in the predicate', async () => {
      const questionName = 'number-q'
      await adminQuestions.addNumberQuestion({
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

    await test.step('Selecting a single value operator sets input type to number', async () => {
      await adminPredicates.selectQuestion(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        questionText,
      )

      await adminPredicates.selectOperator(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        'EQUAL_TO',
      )

      const inputElementLocator = page.locator(
        '#condition-1-subcondition-1-value',
      )

      await expect(inputElementLocator).not.toHaveAttribute('hidden')
      await expect(inputElementLocator).toHaveAttribute('type', 'number')
      await inputElementLocator.fill('1234')
      await expect(inputElementLocator).toHaveValue('1234')

      await validateScreenshot(
        page.getByTestId('condition-1'),
        'values-with-number-question-selected',
      )
    })

    await test.step('Selecting a multiple value operator shows hint text and changes input type', async () => {
      await adminPredicates.selectOperator(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        'IN',
      )

      const hintTextElementLocator = page.locator(
        '#condition-1-subcondition-1-valueHintText',
      )
      const inputElementLocator = page.locator(
        '#condition-1-subcondition-1-value',
      )

      await expect(hintTextElementLocator).not.toHaveAttribute('hidden')

      await expect(inputElementLocator).toHaveAttribute('type', 'text')
      await inputElementLocator.fill('123abc,')
      await expect(inputElementLocator).toHaveValue('123abc,')

      await validateScreenshot(
        page.locator('#condition-1-subcondition-1-valueHintText'),
        'value-hint-text',
      )
    })

    await test.step('Selecting the between operator populates multiple values', async () => {
      await adminPredicates.selectOperator(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        'BETWEEN',
      )
      await waitForHtmxReady(page)

      const inputElementLocator = page.locator(
        '#condition-1-subcondition-1-value',
      )
      const secondInputElementLocator = page.locator(
        '#condition-1-subcondition-1-secondValue',
      )

      await expect(secondInputElementLocator).not.toHaveAttribute('hidden')
      await inputElementLocator.fill('1000')
      await secondInputElementLocator.fill('1234')

      await expect(secondInputElementLocator).toHaveValue('1234')

      await validateScreenshot(
        page.getByTestId('condition-1'),
        'multiple-values-with-number-question-selected',
      )
    })
  })

  test(`Create currency predicate`, async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)

    const programName =
      'Create and edit an eligibility predicate with number question'
    const questionText = 'how much should a house cost?'

    await test.step('Create a program with a question to use in the predicate', async () => {
      const questionName = 'currency-q'
      await adminQuestions.addCurrencyQuestion({
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

    await test.step('Entering text in number question applies filtering', async () => {
      await adminPredicates.selectQuestion(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        questionText,
      )

      await adminPredicates.selectOperator(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        'EQUAL_TO',
      )

      const inputElementLocator = page.locator(
        '#condition-1-subcondition-1-value',
      )

      await expect(inputElementLocator).not.toHaveAttribute('hidden')
      await inputElementLocator.fill('3.50')
      await expect(inputElementLocator).toHaveValue('3.50')

      await validateScreenshot(
        page.getByTestId('condition-1'),
        'values-with-currency-question-selected',
      )
    })

    await test.step('Selecting the between operator populates multiple values', async () => {
      await adminPredicates.selectOperator(
        /* conditionId= */ 1,
        /* subconditionId= */ 1,
        'BETWEEN',
      )

      const secondInputElementLocator = page.locator(
        '#condition-1-subcondition-1-secondValue',
      )

      await expect(secondInputElementLocator).not.toHaveAttribute('hidden')
      await secondInputElementLocator.fill('4.75')
      await expect(secondInputElementLocator).toHaveValue('4.75')

      await validateScreenshot(
        page.getByTestId('condition-1'),
        'multiple-values-with-currency-question-selected',
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
