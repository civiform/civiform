import {
  createTestContext,
  dropTables,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  seedQuestions,
  validateScreenshot,
} from './support'

describe('normal application flow', () => {
  const ctx = createTestContext()

  beforeAll(async () => {
    const {page} = ctx
    await dropTables(page)
    await seedQuestions(page)
  })

  it('all major steps', async () => {
    const {page, adminQuestions, adminPrograms, applicantQuestions} = ctx

    const noApplyFilters = false
    const applyFilters = true

    await loginAsAdmin(page)

    const programName = 'Test program for export'
    await adminQuestions.addDropdownQuestion({
      questionName: 'dropdown-csv-download',
      options: ['op1', 'op2', 'op3'],
    })
    await adminQuestions.addDateQuestion({questionName: 'csv-date'})
    await adminQuestions.addCurrencyQuestion({questionName: 'csv-currency'})
    await adminQuestions.exportQuestion('Name')
    await adminQuestions.exportQuestion('dropdown-csv-download')
    await adminQuestions.exportQuestion('csv-date')
    await adminQuestions.exportQuestion('csv-currency')
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['Name', 'dropdown-csv-download', 'csv-date', 'csv-currency'],
      programName,
    )

    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName)

    // Applicant fills out first application block.
    await applicantQuestions.answerNameQuestion('sarah', 'smith')
    await applicantQuestions.answerDropdownQuestion('op2')
    await applicantQuestions.answerDateQuestion('2021-05-10')
    await applicantQuestions.answerCurrencyQuestion('1000')
    await applicantQuestions.clickNext()

    // Applicant submits answers from review page.
    await applicantQuestions.submitFromReviewPage()

    await logout(page)
    await loginAsProgramAdmin(page)

    await adminPrograms.viewApplications(programName)
    const csvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(csvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00')

    await logout(page)
    await loginAsAdmin(page)

    // Change export visibility of a question
    await adminQuestions.createNewVersion('dropdown-csv-download')
    await adminQuestions.gotoQuestionEditPage('dropdown-csv-download')
    await page.click('#question-settings button:has-text("Delete"):visible')
    await page.click('text=Update')

    // Add a new question so that the program has multiple versions
    await adminQuestions.addNumberQuestion({
      questionName: 'number-csv-download',
    })
    await adminQuestions.exportQuestion('number-csv-download')
    await adminPrograms.editProgramBlock(programName, 'block description', [
      'number-csv-download',
    ])
    await adminPrograms.publishProgram(programName)

    await logout(page)

    // Apply to the program again, this time a different user
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.answerNameQuestion('Gus', 'Guest')
    await applicantQuestions.answerDropdownQuestion('op2')
    await applicantQuestions.answerDateQuestion('1990-01-01')
    await applicantQuestions.answerCurrencyQuestion('2000')
    await applicantQuestions.answerNumberQuestion('1600')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.returnToProgramsFromSubmissionPage()

    // Apply to the program again as the same user
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // #######################################
    // Test clear filter
    // #######################################
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    const postEditCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(postEditCsvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00')
    expect(postEditCsvContent).toContain('Gus,,Guest,op2,01/01/1990,2000.00')

    const numberOfGusEntries =
      postEditCsvContent.split('Gus,,Guest,op2,01/01/1990,2000.00').length - 1
    expect(numberOfGusEntries).toEqual(2)

    // Finds a partial text match on applicant name, case insensitive.
    await adminPrograms.filterProgramApplications({searchFragment: 'SARA'})
    const filteredCsvContent = await adminPrograms.getCsv(applyFilters)
    expect(filteredCsvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00')
    expect(filteredCsvContent).not.toContain(
      'Gus,,Guest,op2,01/01/1990,2000.00',
    )
    const filteredJsonContent = await adminPrograms.getJson(applyFilters)
    expect(filteredJsonContent.length).toEqual(1)
    expect(filteredJsonContent[0].application.name.first_name).toEqual('sarah')
    // Ensures that choosing not to apply filters continues to return all
    // results.
    const allCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(allCsvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00')
    expect(allCsvContent).toContain('Gus,,Guest,op2,01/01/1990,2000.00')
    const allJsonContent = await adminPrograms.getJson(noApplyFilters)
    expect(allJsonContent.length).toEqual(3)
    expect(allJsonContent[0].application.name.first_name).toEqual('Gus')
    expect(allJsonContent[1].application.name.first_name).toEqual('Gus')
    expect(allJsonContent[2].application.name.first_name).toEqual('sarah')

    // Checks that all applicants are displayed
    await adminPrograms.clearProgramApplications()
    const unfilteredCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(unfilteredCsvContent).toContain(
      'sarah,,smith,op2,05/10/2021,1000.00',
    )
    expect(unfilteredCsvContent).toContain('Gus,,Guest,op2,01/01/1990,2000.00')

    await validateScreenshot(page, 'applications-page')

    await logout(page)
  })
})
