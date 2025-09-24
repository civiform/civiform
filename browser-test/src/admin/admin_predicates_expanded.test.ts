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

    // Create a program with a question to use in the predicate
    const questionName = 'predicate-q'
    await adminQuestions.addTextQuestion({questionName: questionName})
    const programName = 'Create and edit a new predicate'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      description: 'first screen',
      questions: [{name: questionName}],
    })

    // Validate empty state without predicate
    await adminPrograms.goToBlockInProgram(programName, 'Screen 1')
    expect(await page.innerText('#eligibility-predicate')).toContain(
      'This screen does not have any eligibility conditions',
    )

    // Edit eligibility predicate
    await adminPrograms.goToEditBlockEligibilityPredicatePage(
      programName,
      'Screen 1',
      /* expandedFormLogicEnabled= */ true,
    )

    await adminPredicates.clickAddConditionButton()

    await adminPredicates.expectCondition(1)

    await validateScreenshot(page, 'edit-predicate')
  })
})
