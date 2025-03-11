import {test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

test.describe('admin preview as applicant', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('preview as applicant and use to admin back button', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
  }) => {
    const programName = 'test program'
    await test.step("create test program", async () => {
      await adminQuestions.addEmailQuestion({questionName: 'email-q'})
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        description: 'description',
        questions: [{name: 'email-q'}],
      })
    })

    await test.step("preview as applicant and check that back to admin button is visible", async () => {
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await page.click('button:has-text("Preview as applicant")')
      await waitForPageJsLoad(page)
  
      await validateScreenshot(
        page,
        'northstar-admin-program-preview-application-review-page',
      )
      await page.isVisible('a:has-text("Back to admin view")')
    })

    await test.step("navigate in applicant preview", async () => {
      await applicantQuestions.clickContinue()
    
      await page.isVisible('a:has-text("Back to admin view")')

      await validateScreenshot(
        page,
        'northstar-admin-program-preview-application-block-edit-page',
      )
    }) 

    await test.step("navigate back to admin view", async () => {
      await page.click('a:has-text("Back to admin view")')
  
      await adminPrograms.expectProgramBlockEditPage(programName)  
    })
  })
})
