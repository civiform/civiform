import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('create and edit predicates', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'expanded_form_logic_enabled')
  })

  test('predicate page program header', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programName = 'Predicate page program header'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name: 'Screen 1',
      description: 'first screen',
      questions: [],
    })

    await adminPrograms.goToEditBlockEligibilityPredicatePage(
      programName,
      'Screen 1',
      /* expandedFormLogicEnabled= */ true,
    )

    expect(await page.innerText('#program-title')).toContain(programName)
    await validateScreenshot(
      page.locator('.cf-legacy-program-header'),
      'program-header',
    )
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
      const questionName = 'predicate-q'
      await adminQuestions.addTextQuestion({questionName: questionName})
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
        'edit-predicate',
      )
    })
  })
})
