import {test, expect} from '@playwright/test'
import {createTestContext, loginAsAdmin, waitForPageJsLoad} from './support'

test.describe('create dropdown question with options', () => {
  const ctx = createTestContext()

  test('add remove buttons work correctly', async () => {
    const {page, adminQuestions} = ctx

    await loginAsAdmin(page)

    await page.click('text=Questions')
    await waitForPageJsLoad(page)

    await page.click('#create-question-button')
    await page.click('#create-dropdown-question')
    await waitForPageJsLoad(page)

    // Verify question preview has default text.
    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      'Sample question text',
    )
    expect(await page.innerText('.cf-applicant-question-help-text')).toContain(
      '',
    )

    // Fill in basic info
    const questionName = 'favorite ice cream'
    await page.fill('text=Question Text', 'questionText')
    await page.fill('text=Question help text', 'helpText')
    await page.fill('text=Administrative identifier', questionName)
    await page.fill(
      'text=Question note for administrative use only',
      'description',
    )

    // Add three options
    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(0, {
      adminName: 'chocolate_admin',
      text: 'chocolate',
    })
    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(1, {
      adminName: 'vanilla_admin',
      text: 'vanilla',
    })
    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(2, {
      adminName: 'strawberry_admin',
      text: 'strawberry',
    })

    // Assert there are three options present
    let questionSettingsDiv = await page.innerHTML('#question-settings')
    // 2 inputs each for 3 options (option, optionAdminName) + hidden nextAvailableId
    expect(questionSettingsDiv.match(/<input/g)).toHaveLength(7)

    // Remove first option - use :visible to not select the hidden template
    // await page.click('button:has-text("Delete"):visible')
    await adminQuestions.deleteMultiOptionAnswer(0)

    // Assert there are only two options now
    questionSettingsDiv = await page.innerHTML('#question-settings')
    // 2 inputs each for 2 options (option, optionAdminName) + hidden nextAvailableId
    expect(questionSettingsDiv.match(/<input/g)).toHaveLength(5)
    // First option should now be vanilla
    await adminQuestions.expectNewMultiOptionAnswer(0, {
      adminName: 'vanilla_admin',
      text: 'vanilla',
    })

    // Verify question preview text has changed based on user input.
    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      'questionText',
    )
    expect(await page.innerText('.cf-applicant-question-help-text')).toContain(
      'helpText',
    )

    // Submit the form, then edit that question again
    await adminQuestions.clickSubmitButtonAndNavigate('Create')
    await adminQuestions.expectDraftQuestionExist(questionName)

    // Edit the question
    await adminQuestions.gotoQuestionEditPage(questionName)
    questionSettingsDiv = await page.innerHTML('#question-settings')
    // 3 inputs each for 2 options (option, optionAdminName, and optionId) + hidden nextAvailableId
    expect(questionSettingsDiv.match(/<input/g)).toHaveLength(7)
    // Check that admin names were set correctly
    await adminQuestions.expectExistingMultiOptionAnswer(0, {
      adminName: 'vanilla_admin',
      text: 'vanilla',
    })
    await adminQuestions.expectExistingMultiOptionAnswer(1, {
      adminName: 'strawberry_admin',
      text: 'strawberry',
    })

    // Edit an option
    await adminQuestions.changeMultiOptionAnswer(1, 'pistachio')
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.gotoQuestionEditPage(questionName)
    // Expect that the option text has changed but the admin name has not
    await adminQuestions.expectExistingMultiOptionAnswer(1, {
      adminName: 'strawberry_admin',
      text: 'pistachio',
    })

    // Remove the last option and add a new one, and assert the new option has the correct admin name
    await adminQuestions.deleteMultiOptionAnswer(1)
    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(1, {
      adminName: 'mango_admin',
      text: 'mango',
    })
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.gotoQuestionEditPage(questionName)
    // Expect that the option text has changed but the admin name has not
    await adminQuestions.expectExistingMultiOptionAnswer(1, {
      adminName: 'mango_admin',
      text: 'mango',
    })
  })
})
