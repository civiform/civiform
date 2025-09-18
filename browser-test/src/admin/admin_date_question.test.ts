import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin, waitForPageJsLoad} from '../support'

test.describe('Create date question with validation parameters', () => {
  test('Edit date question with date validation settings', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    await test.step('Go to edit page for date question', async () => {
      await adminQuestions.gotoAdminQuestionsPage()

      await page.click('#create-question-button')
      await page.click('#create-date-question')
      await waitForPageJsLoad(page)
    })

    await test.step('Expect default min and max date types are shown', async () => {
      // Verify validation parameters section is visible
      const questionSettings = page.getByTestId('question-settings')
      await expect(questionSettings).toContainText('Validation parameters')

      // Verify default values are selected
      expect(await page.locator('#min-date-type').inputValue()).toContain('ANY')
      expect(await page.locator('#max-date-type').inputValue()).toContain('ANY')

      // Verify date pickers are not visible
      await expect(page.locator('#min-custom-date-fieldset')).toBeHidden()
      await expect(page.locator('#max-custom-date-fieldset')).toBeHidden()
    })

    await test.step('Expect min date picker is visible iff min date type is custom', async () => {
      // Change min date type to custom
      await page.selectOption('#min-date-type', {value: 'CUSTOM'})

      // Verify min date picker is visible
      await expect(page.locator('#min-custom-date-fieldset')).toBeVisible()

      // Change min date type to any
      await page.selectOption('#min-date-type', {value: 'ANY'})

      // Verify min date picker is hidden
      await expect(page.locator('#min-custom-date-fieldset')).toBeHidden()
    })

    await test.step('Expect max date picker is visible iff max date type is custom', async () => {
      // Change max date type to custom
      await page.selectOption('#max-date-type', {value: 'CUSTOM'})

      // Verify max date picker is visible
      await expect(page.locator('#max-custom-date-fieldset')).toBeVisible()

      // Change max date type to application submission date
      await page.selectOption('#max-date-type', {value: 'APPLICATION_DATE'})

      // Verify max date picker is hidden
      await expect(page.locator('#max-custom-date-fieldset')).toBeHidden()
    })

    await test.step('Expect switching date type clears custom date', async () => {
      // Select custom date type
      await page.selectOption('#min-date-type', {value: 'CUSTOM'})
      await page.selectOption('#max-date-type', {value: 'CUSTOM'})
      // Set a custom date
      await page.locator('#min-custom-date-day').fill('1')
      await page.locator('#min-custom-date-month').selectOption('2')
      await page.locator('#min-custom-date-year').fill('2025')
      await page.locator('#max-custom-date-day').fill('2')
      await page.locator('#max-custom-date-month').selectOption('3')
      await page.locator('#max-custom-date-year').fill('2025')
      // Verify date is set
      await expect(page.locator('#min-custom-date-day')).toHaveValue('1')
      await expect(page.locator('#min-custom-date-month')).toHaveValue('2')
      await expect(page.locator('#min-custom-date-year')).toHaveValue('2025')
      await expect(page.locator('#max-custom-date-day')).toHaveValue('2')
      await expect(page.locator('#max-custom-date-month')).toHaveValue('3')
      await expect(page.locator('#max-custom-date-year')).toHaveValue('2025')

      // Change min date type to any then back to custom
      await page.selectOption('#min-date-type', {value: 'ANY'})
      await page.selectOption('#min-date-type', {value: 'CUSTOM'})

      // Verify only min custom date is cleared, max is unchanged
      await expect(page.locator('#min-custom-date-day')).toHaveValue('')
      await expect(page.locator('#min-custom-date-month')).toHaveValue('')
      await expect(page.locator('#min-custom-date-year')).toHaveValue('')
      await expect(page.locator('#max-custom-date-day')).toHaveValue('2')
      await expect(page.locator('#max-custom-date-month')).toHaveValue('3')
      await expect(page.locator('#max-custom-date-year')).toHaveValue('2025')

      // Change max date type to any then back to custom
      await page.selectOption('#max-date-type', {value: 'ANY'})
      await page.selectOption('#max-date-type', {value: 'CUSTOM'})

      // Verify max custom date is cleared
      await expect(page.locator('#max-custom-date-day')).toHaveValue('')
      await expect(page.locator('#max-custom-date-month')).toHaveValue('')
      await expect(page.locator('#max-custom-date-year')).toHaveValue('')
    })
  })

  test('Date validation settings are prepopulated with saved values', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    const questionName = 'date-with-validation'

    await test.step('Create date question', async () => {
      await adminQuestions.addDateQuestion({
        questionName: questionName,
        questionText: 'date with validation',
        helpText: 'date with validation help text',
      })
    })

    await test.step('Edit question to add date validation settings and save', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)
      await waitForPageJsLoad(page)
      // Set min date to custom date
      await page.selectOption('#min-date-type', {value: 'CUSTOM'})
      await page.locator('#min-custom-date-day').fill('3')
      await page.locator('#min-custom-date-month').selectOption('4')
      await page.locator('#min-custom-date-year').fill('2024')
      // Set max date to application date
      await page.selectOption('#max-date-type', {value: 'APPLICATION_DATE'})
      await adminQuestions.clickSubmitButtonAndNavigate('Update')
    })

    await test.step('Expect saved date validation settings are prepopulated in edit page', async () => {
      await adminQuestions.gotoAdminQuestionsPage()
      await adminQuestions.expectDraftQuestionExist(questionName)
      await adminQuestions.gotoQuestionEditPage(questionName)
      await waitForPageJsLoad(page)

      // Verify min custom date is populated
      expect(await page.locator('#min-date-type').inputValue()).toContain(
        'CUSTOM',
      )
      await expect(page.locator('#min-custom-date-fieldset')).toBeVisible()
      await expect(page.locator('#min-custom-date-day')).toHaveValue('3')
      await expect(page.locator('#min-custom-date-month')).toHaveValue('4')
      await expect(page.locator('#min-custom-date-year')).toHaveValue('2024')
      // Verify max date is application date and date picker is hidden
      expect(await page.locator('#max-date-type').inputValue()).toContain(
        'APPLICATION_DATE',
      )
      await expect(page.locator('#max-custom-date-fieldset')).toBeHidden()
    })
  })
})
