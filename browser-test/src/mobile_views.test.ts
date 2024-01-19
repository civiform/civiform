// This file tests a few view on mobile as a sanity check. https://github.com/civiform/civiform/issues/4416
// tracks adding a more comprehensive set of tests.

import {
  createTestContext,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('views render well on mobile', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  const primaryProgramName = 'Application index primary program'

  const firstQuestionText = 'This is the first question'
  const secondQuestionText = 'This is the second question'

  beforeAll(async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)

    // Create a program with two questions on separate blocks so that an applicant can partially
    // complete an application.
    await adminPrograms.addProgram(primaryProgramName)
    await adminQuestions.addTextQuestion({
      questionName: 'first-q',
      questionText: firstQuestionText,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'second-q',
      questionText: secondQuestionText,
    })
    await adminPrograms.addProgramBlock(primaryProgramName, 'first block', [
      'first-q',
    ])
    await adminPrograms.addProgramBlock(primaryProgramName, 'second block', [
      'second-q',
    ])

    await adminPrograms.publishAllDrafts()
    await logout(page)

    // Use mobile view for all tests in this file. We must set it here so that the
    // admin prep work is done with the desktop view.
    ctx.useMobile = true
  })

  it('initial load of CiviForm', async () => {
    const {page} = ctx
    await validateAccessibility(page)

    await validateScreenshot(page, 'mobile-initial-load')
  })

  it('modal', async () => {
    const {page} = ctx
    await validateAccessibility(page)

    // Click the apply button but don't click through the login prompt modal
    await page.click(
      `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
    )

    await validateScreenshot(page, 'mobile-modal', /* fullPage= */ false)
  })

  it('apply page', async () => {
    const {page, applicantQuestions} = ctx
    await validateAccessibility(page)

    // Click the apply button and click through the login prompt modal
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)

    await validateScreenshot(page, 'mobile-apply-page')
  })
})
