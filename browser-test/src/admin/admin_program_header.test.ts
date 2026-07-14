import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'
import {SAMPLE_PROGRAMS} from '../support/seeding'

test.describe('admin program page headers', () => {
  test('predicate page program header', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await seeding.seedProgramsAndCategories()
    await enableFeatureFlag(page, 'expanded_form_logic_enabled')
    await loginAsAdmin(page)

    const programName = SAMPLE_PROGRAMS.minimal
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
