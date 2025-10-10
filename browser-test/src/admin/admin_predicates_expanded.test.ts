import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('create and edit predicates', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'expanded_form_logic_enabled')
  })

  test('create and edit a new predicate', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminPredicates,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Create and edit a new predicate'

    await test.step('Create a program with a question to use in the predicate', async () => {
      const firstScreenQuestionName = 'predicate-q'
      const secondScreenQuestionName = 'second-predicate-q'
      await adminQuestions.addTextQuestion({
        questionName: firstScreenQuestionName,
      })
      await adminQuestions.addTextQuestion({
        questionName: secondScreenQuestionName,
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'first screen',
        questions: [{name: firstScreenQuestionName}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        name: 'Screen 2',
        description: 'second screen',
        questions: [{name: secondScreenQuestionName}],
      })

      // Validate empty state without predicate
      await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
      await expect(page.locator('#eligibility-predicate')).toContainText(
        'This screen does not have any eligibility conditions',
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

  test('no available questions on screen', async ({
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
})
