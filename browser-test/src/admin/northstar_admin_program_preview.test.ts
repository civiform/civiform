import { test } from '../support/civiform_fixtures'
import { enableFeatureFlag, loginAsAdmin, validateScreenshot, waitForPageJsLoad } from '../support'

test.describe('admin program preview', 
  {tag: ['@northstar']}, 
  () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('preview program and use back button', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)

    await adminQuestions.addEmailQuestion({questionName: 'email-q'})
    const programName = 'test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlockUsingSpec(programName, {
      description: 'description',
      questions: [{name: 'email-q'}],
    })
    await adminPrograms.gotoEditDraftProgramPage(programName)

    await validateScreenshot(page, 'northstar-admin-edit-draft-program-page')
    await page.click('button:has-text("Preview as applicant")')
    await waitForPageJsLoad(page)

    await validateScreenshot(page, 'northstar-admin-program-preview-application-review-page')
    await page.isVisible('a:has-text("Back to admin view")')

    await applicantQuestions.clickContinue()

    await validateScreenshot(page, 'northstar-admin-program-preview-application-block-edit-page')
    await page.click('a:has-text("Back to admin view")')

    await adminPrograms.expectProgramBlockEditPage(programName)
  })

})
