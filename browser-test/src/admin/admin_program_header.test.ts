import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('admin program page headers', {tag: ['@northstar']}, () => {
  test('predicate page program header', async ({page, adminPrograms}) => {
    await enableFeatureFlag(page, 'expanded_form_logic_enabled')
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

    await expect(page.locator('#program-title')).toContainText(programName)
    await validateScreenshot(
      page.locator('.cf-legacy-program-header'),
      'program-header',
    )
  })
})
