import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  disableFeatureFlag,
  loginAsAdmin,
  waitForPageJsLoad,
} from '../support'

test.describe('create dropdown question with options', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(
      page,
      'ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED',
    )
  })

  test('add remove buttons work correctly', async ({page, adminQuestions}) => {
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

  test('options scoring works correctly', async ({page, adminQuestions}) => {
    const questionName = 'favorite ice cream'

    await test.step(`setup`, async () => {
      await enableFeatureFlag(page, 'ANSWER_OPTION_SCORING_ENABLED')

      await loginAsAdmin(page)

      await page.click('text=Questions')
      await waitForPageJsLoad(page)

      await page.click('#create-question-button')
      await page.click('#create-dropdown-question')
      await waitForPageJsLoad(page)

      // Fill in basic info
      await page.fill('text=Question Text', 'questionText')
      await page.fill('text=Question help text', 'helpText')
      await page.fill('text=Administrative identifier', questionName)
      await page.fill(
        'text=Question note for administrative use only',
        'description',
      )
    })

    await test.step(`add options with scores`, async () => {
      // Add three options
      await page.click('#add-new-option')
      await adminQuestions.fillMultiOptionAnswer(0, {
        adminName: 'chocolate_admin',
        text: 'chocolate',
        score: '1',
      })
      await page.click('#add-new-option')
      await adminQuestions.fillMultiOptionAnswer(1, {
        adminName: 'vanilla_admin',
        text: 'vanilla',
        score: '2',
      })
      await page.click('#add-new-option')
      await adminQuestions.fillMultiOptionAnswer(2, {
        adminName: 'strawberry_admin',
        text: 'strawberry',
        score: '3',
      })
      await adminQuestions.clickSubmitButtonAndNavigate('Create')
      await adminQuestions.expectDraftQuestionExist(questionName)
    })

    await test.step(`edit the question and check scores were preserved`, async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestions.expectExistingMultiOptionAnswer(0, {
        adminName: 'chocolate_admin',
        text: 'chocolate',
        score: '1',
      })
      await adminQuestions.expectExistingMultiOptionAnswer(1, {
        adminName: 'vanilla_admin',
        text: 'vanilla',
        score: '2',
      })
      await adminQuestions.expectExistingMultiOptionAnswer(2, {
        adminName: 'strawberry_admin',
        text: 'strawberry',
        score: '3',
      })
    })

    await test.step(`trigger missing score error`, async () => {
      await adminQuestions.changeMultiOptionScore(1, '')
      await adminQuestions.clickSubmitButtonAndNavigate('Update')
      await expect(
        page.getByRole('alert').filter({
          hasText:
            'Error: When creating a scored question, all options must include scores.',
        }),
      ).toBeVisible()
    })

    await test.step(`remove all scores and submit successfully`, async () => {
      await adminQuestions.changeMultiOptionScore(0, '')
      await adminQuestions.changeMultiOptionScore(1, '')
      await adminQuestions.changeMultiOptionScore(2, '')
      await adminQuestions.clickSubmitButtonAndNavigate('Update')
      await adminQuestions.expectDraftQuestionExist(questionName)
    })

    await test.step(`confirm scores are preserved even when flag is turned off`, async () => {
      // add scores back in
      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestions.changeMultiOptionScore(0, '4')
      await adminQuestions.changeMultiOptionScore(1, '5')
      await adminQuestions.changeMultiOptionScore(2, '6')
      await adminQuestions.clickSubmitButtonAndNavigate('Update')
      await adminQuestions.expectDraftQuestionExist(questionName)

      // disable flag and edit question
      await disableFeatureFlag(page, 'ANSWER_OPTION_SCORING_ENABLED')
      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestions.expectMultiOptionScoreInputHidden(1)
      await adminQuestions.expectMultiOptionScoreInputHidden(2)
      await adminQuestions.expectMultiOptionScoreInputHidden(3)
      await adminQuestions.changeMultiOptionAnswer(0, 'pistachio')
      await adminQuestions.clickSubmitButtonAndNavigate('Update')

      // toggle flag on, edit question, see old scores preserved
      await enableFeatureFlag(page, 'ANSWER_OPTION_SCORING_ENABLED')
      await adminQuestions.gotoQuestionEditPage(questionName)
      await adminQuestions.expectExistingMultiOptionAnswer(0, {
        adminName: 'chocolate_admin',
        text: 'pistachio',
        score: '4',
      })
      await adminQuestions.expectExistingMultiOptionAnswer(1, {
        adminName: 'vanilla_admin',
        text: 'vanilla',
        score: '5',
      })
      await adminQuestions.expectExistingMultiOptionAnswer(2, {
        adminName: 'strawberry_admin',
        text: 'strawberry',
        score: '6',
      })
    })
  })
})
