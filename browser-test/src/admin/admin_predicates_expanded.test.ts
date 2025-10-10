import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

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

      await validateScreenshot(
        page.locator('#edit-predicate'),
        'predicate-zero-state',
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
})
