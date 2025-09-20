import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  loginAsAdmin,
  logout,
} from '../../support'

test.describe('Date question autofill functionality', {tag: ['@northstar']}, () => {
  const programName = 'Test program for date autofill'

  test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
    await loginAsAdmin(page)
    
    await adminQuestions.addDateQuestion({
      questionName: 'date_autofill_test',
      questionText: 'What is today\'s date?',
      minDateType: 'APPLICATION_DATE',
      maxDateType: 'APPLICATION_DATE',
    })

    // Create a program with this question
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'first block', [
      'date_autofill_test',
    ])
    await adminPrograms.publishProgram(programName)
    await logout(page)
  })

  test('autofills current date when both min and max date are APPLICATION_DATE', async ({
    page,
    applicantQuestions,
  }) => {
    await applicantQuestions.applyProgram(programName, /* northStarEnabled= */ true)

    // Get today's date in the format expected by the date input
    const today = new Date()
    const todayString = today.toISOString().split('T')[0] // YYYY-MM-DD format

    // Check that the date input is autofilled with today's date
    const dateInput = page.locator('input[type="date"]')
    await expect(dateInput).toHaveValue(todayString)
  })

  test('does not autofill when applicant already has a date value', async ({
    page,
    applicantQuestions,
  }) => {
    await applicantQuestions.applyProgram(programName, /* northStarEnabled= */ true)

    const customDate = '2023-12-25'
    await page.fill('input[type="date"]', customDate)
    await applicantQuestions.clickContinue()

    await applicantQuestions.clickEdit()

    const dateInput = page.locator('input[type="date"]')
    await expect(dateInput).toHaveValue(customDate)
  })

  test('does not autofill when only one of min/max date is APPLICATION_DATE', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.addDateQuestion({
      questionName: 'date_partial_validation_test',
      questionText: 'What is your birthday?',
      minDateType: 'APPLICATION_DATE',
      maxDateType: 'ANY',
    })

    const partialProgramName = 'Test program for partial date validation'
    await adminPrograms.addProgram(partialProgramName)
    await adminPrograms.editProgramBlock(partialProgramName, 'first block', [
      'date_partial_validation_test',
    ])
    await adminPrograms.publishProgram(partialProgramName)
    await logout(page)

    await applicantQuestions.applyProgram(partialProgramName, /* northStarEnabled= */ true)

    const dateInput = page.locator('input[type="date"]')
    await expect(dateInput).toHaveValue('')
  })
})
