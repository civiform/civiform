import {
  startSession,
  seedCanonicalQuestions,
  dropTables,
  logout,
  loginAsTestUser,
  loginAsGuest,
  loginAsProgramAdmin,
  loginAsAdmin,
  selectApplicantLanguage,
  ApplicantQuestions,
  AdminQuestions,
  AdminPrograms,
  endSession,
  isLocalDevEnvironment,
} from './support'

describe('normal application flow', () => {
  beforeAll(async () => {
    const {page} = await startSession()
    await dropTables(page)
    await seedCanonicalQuestions(page)
  })

  it('all major steps', async () => {
    const {browser, page} = await startSession()
    // Timeout for clicks and element fills. If your selector fails to locate
    // the HTML element, the test hangs. If you find the tests time out, you
    // want to verify that your selectors are working as expected first.
    // Because all tests are run concurrently, it could be that your selector
    // selects a different entity from another test.
    page.setDefaultTimeout(4000)

    const noApplyFilters = false
    const applyFilters = true

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)
    const applicantQuestions = new ApplicantQuestions(page)

    const programName = 'test program for export'
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
    await selectApplicantLanguage(page, 'English')

    await applicantQuestions.applyProgram(programName)

    // Applicant fills out first application block.
    await applicantQuestions.answerNameQuestion('sarah', 'smith')
    await applicantQuestions.answerDropdownQuestion('op2')
    await applicantQuestions.answerDateQuestion('2021-05-10')
    await applicantQuestions.answerCurrencyQuestion('1000')
    await applicantQuestions.clickNext()

    // Applicant submits answers from review page.
    await applicantQuestions.submitFromReviewPage(programName)

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
    await page.click('#question-settings button:text("Remove"):visible')
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
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.answerNameQuestion('Gus', 'Guest')
    await applicantQuestions.answerDropdownQuestion('op2')
    await applicantQuestions.answerDateQuestion('1990-01-01')
    await applicantQuestions.answerCurrencyQuestion('2000')
    await applicantQuestions.answerNumberQuestion('1600')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage(programName)
    await applicantQuestions.returnToProgramsFromSubmissionPage()

    // Apply to the program again as the same user
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantQuestions.submitFromPreviewPage(programName)
    await logout(page)

    // #######################################
    // Test program applications export
    // #######################################
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    const postEditCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(postEditCsvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00')
    expect(postEditCsvContent).toContain('Gus,,Guest,op2,01/01/1990,2000.00')

    const numberOfGusEntries =
      postEditCsvContent.split('Gus,,Guest,op2,01/01/1990,2000.00').length - 1
    expect(numberOfGusEntries).toEqual(2)

    const postEditJsonContent = JSON.parse(
      await adminPrograms.getJson(noApplyFilters),
    )
    expect(postEditJsonContent.length).toEqual(3)
    expect(postEditJsonContent[0].program_name).toEqual(programName)
    expect(postEditJsonContent[0].language).toEqual('en-US')
    expect(
      postEditJsonContent[0].application.csvcurrency.currency_dollars,
    ).toEqual(2000.0)
    expect(
      postEditJsonContent[0].application.dropdowncsvdownload.selection,
    ).toEqual('op2')
    expect(postEditJsonContent[0].application.name.first_name).toEqual('Gus')
    expect(postEditJsonContent[0].application.name.middle_name).toBeNull()
    expect(postEditJsonContent[0].application.name.last_name).toEqual('Guest')
    expect(postEditJsonContent[0].application.csvdate.date).toEqual(
      '1990-01-01',
    )
    expect(postEditJsonContent[0].application.numbercsvdownload.number).toEqual(
      1600,
    )

    // Finds a partial text match on applicant name, case insensitive.
    await adminPrograms.filterProgramApplications('SARA')
    const filteredCsvContent = await adminPrograms.getCsv(applyFilters)
    expect(filteredCsvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00')
    expect(filteredCsvContent).not.toContain(
      'Gus,,Guest,op2,01/01/1990,2000.00',
    )
    const filteredJsonContent = JSON.parse(
      await adminPrograms.getJson(applyFilters),
    )
    expect(filteredJsonContent.length).toEqual(1)
    expect(filteredJsonContent[0].application.name.first_name).toEqual('sarah')
    // Ensures that choosing not to apply filters continues to return all
    // results.
    const allCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(allCsvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00')
    expect(allCsvContent).toContain('Gus,,Guest,op2,01/01/1990,2000.00')
    const allJsonContent = JSON.parse(
      await adminPrograms.getJson(noApplyFilters),
    )
    expect(allJsonContent.length).toEqual(3)
    expect(allJsonContent[0].application.name.first_name).toEqual('Gus')
    expect(allJsonContent[1].application.name.first_name).toEqual('Gus')
    expect(allJsonContent[2].application.name.first_name).toEqual('sarah')

    await logout(page)

    // #######################################
    // Test demography export
    // #######################################
    await loginAsAdmin(page)
    await adminPrograms.gotoAdminProgramsPage()
    const demographicsCsvContent = await adminPrograms.getDemographicsCsv()

    expect(demographicsCsvContent).toContain(
      'Opaque ID,Program,Submitter Email (Opaque),TI Organization,Create time,Submit time,name (first_name),name (middle_name),name (last_name),csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection)',
    )
    expect(demographicsCsvContent).toContain(
      'sarah,,smith,1000.00,05/10/2021,op2',
    )

    await adminQuestions.createNewVersion('Name')
    await adminQuestions.exportQuestionOpaque('Name')
    await adminPrograms.publishProgram(programName)

    await adminPrograms.gotoAdminProgramsPage()
    const newDemographicsCsvContent = await adminPrograms.getDemographicsCsv()

    expect(newDemographicsCsvContent).toContain(
      'Opaque ID,Program,Submitter Email (Opaque),TI Organization,Create time,Submit time,csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number),name (first_name),name (middle_name),name (last_name)',
    )
    expect(newDemographicsCsvContent).not.toContain(',sarah,,smith')
    expect(newDemographicsCsvContent).toContain(',op2,')
    expect(newDemographicsCsvContent).toContain(',1600,')

    if (isLocalDevEnvironment()) {
      // The hashed values "sarah", empty value, "smith", with the dev secret key.
      expect(newDemographicsCsvContent).toContain(
        '5009769596aa83552389143189cec81abfc8f56abc1bb966715c47ce4078c403,057ba03d6c44104863dc7361fe4578965d1887360f90a0895882e58a6248fc86,6eecddf47b5f7a90d41ccc978c4c785265242ce75fe50be10c824b73a25167ba',
      )
    }

    await endSession(browser)
  })
})
