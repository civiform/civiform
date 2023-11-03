import {
  createTestContext,
  dropTables,
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  seedQuestions,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'

describe('csv export for multioption question', () => {
  const ctx = createTestContext()

  beforeAll(async () => {
    const {page} = ctx
    await dropTables(page)
    await seedQuestions(page)
  })
  it('test multioption csv into its own column', async () => {
    const {page, adminQuestions, adminPrograms, applicantQuestions} = ctx

    const noApplyFilters = false

    await loginAsAdmin(page)
    const programName = 'Checkbox question program for export'
    await adminQuestions.createCheckboxQuestion(
      {
        questionName: 'csv-color',
        questionText: 'Sample question text',
        helpText: 'Sample question help text',
        options: [
          {adminName: 'red admin', text: 'red'},
          {adminName: 'green admin', text: 'green'},
          {adminName: 'orange admin', text: 'orange'},
          {adminName: 'blue admin', text: 'blue'},
        ],
        maxNum: 3,
        minNum: 2,
      },
      /* clickSubmit= */ true,
    )
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['Name', 'csv-color'],
      programName,
    )
    await logout(page)

    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.answerNameQuestion('Jane', 'Doe')
    await applicantQuestions.answerCheckboxQuestion(['blue', 'red'])
    await applicantQuestions.clickNext()

    // Applicant submits answers from review page.
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    await loginAsAdmin(page)
    await adminQuestions.gotoQuestionEditPage('csv-color')
    await adminQuestions.deleteMultiOptionAnswer(0)
    await adminQuestions.deleteMultiOptionAnswer(1)
    await waitForPageJsLoad(page)

    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(2, {
      adminName: 'black admin',
      text: 'black',
    })
    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(3, {
      adminName: 'white admin',
      text: 'white',
    })
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await waitForPageJsLoad(page)
    await adminPrograms.publishProgram(programName)

    await logout(page)

    await applicantQuestions.clickApplyProgramButton(programName)
    await page.click('text="Continue"')
    await waitForPageJsLoad(page)
    await applicantQuestions.answerNameQuestion('John', 'Do')
    await applicantQuestions.answerCheckboxQuestion(['black', 'green'])
    await applicantQuestions.clickNext()
    await page.click('text="Submit"')

    await logout(page)
    await loginAsProgramAdmin(page)

    await adminPrograms.viewApplications(programName)
    const csvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(csvContent).toContain(
      'Applicant ID,Application ID,Applicant Language,Submit Time,Submitter Type,TI Email,TI Organization,Status,name (first_name),name (middle_name),name (last_name),csvcolor (red admin),csvcolor (blue admin),csvcolor (green admin),csvcolor (black admin),csvcolor (white admin)',
    )
    expect(csvContent).toContain(
      ',John,,Do,Not An Option At Program Version,Not Selected,Selected,Selected,Not Selected',
    )
    expect(csvContent).toContain(
      ',Jane,,Doe,Selected,Selected,Not Selected,Not An Option At Program Version,Not An Option At Program Version',
    )
  })
})

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
      options: [
        {adminName: 'op1 admin', text: 'op1'},
        {adminName: 'op2 admin', text: 'op2'},
        {adminName: 'op3 admin', text: 'op3'},
      ],
    })
    await adminQuestions.createCheckboxQuestion(
      {
        questionName: 'csv-color',
        questionText: 'Sample question text',
        helpText: 'Sample question help text',
        options: [
          {adminName: 'red admin', text: 'red'},
          {adminName: 'green admin', text: 'green'},
          {adminName: 'orange admin', text: 'orange'},
          {adminName: 'blue admin', text: 'blue'},
        ],
      },
      /* clickSubmit= */ true,
    )
    await adminQuestions.addDateQuestion({questionName: 'csv-date'})
    await adminQuestions.addCurrencyQuestion({questionName: 'csv-currency'})
    await adminQuestions.exportQuestion('Name')
    await adminQuestions.exportQuestion('dropdown-csv-download')
    await adminQuestions.exportQuestion('csv-date')
    await adminQuestions.exportQuestion('csv-currency')
    await adminQuestions.exportQuestion('csv-color')
    await adminPrograms.addAndPublishProgramWithQuestions(
      [
        'Name',
        'dropdown-csv-download',
        'csv-date',
        'csv-currency',
        'csv-color',
      ],
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
    await applicantQuestions.answerCheckboxQuestion(['blue'])
    await applicantQuestions.clickNext()

    // Applicant submits answers from review page.
    await applicantQuestions.submitFromReviewPage()

    await logout(page)
    await loginAsProgramAdmin(page)

    await adminPrograms.viewApplications(programName)
    const csvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(csvContent).toContain(
      'sarah,,smith,op2 admin,05/10/2021,1000.00,Selected,Not Selected,Not Selected,Not Selected',
    )

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
    await applicantQuestions.answerCheckboxQuestion(['red'])
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.returnToProgramsFromSubmissionPage()

    // Apply to the program again as the same user
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // #######################################
    // Test program applications export
    // #######################################
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    const postEditCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(postEditCsvContent).toContain(
      'sarah,,smith,op2 admin,05/10/2021,1000.00',
    )
    expect(postEditCsvContent).toContain(
      'Gus,,Guest,op2 admin,01/01/1990,2000.00',
    )

    const numberOfGusEntries =
      postEditCsvContent.split('Gus,,Guest,op2 admin,01/01/1990,2000.00')
        .length - 1
    expect(numberOfGusEntries).toEqual(2)

    const postEditJsonContent = await adminPrograms.getJson(noApplyFilters)
    expect(postEditJsonContent.length).toEqual(3)
    // program name used in JSON export is the admin name. Which we slugified for this test.
    expect(postEditJsonContent[0].program_name).toEqual(
      'test-program-for-export',
    )
    expect(postEditJsonContent[0].language).toEqual('en-US')
    expect(
      postEditJsonContent[0].application.csvcurrency.currency_dollars,
    ).toEqual(2000.0)
    expect(
      postEditJsonContent[0].application.dropdowncsvdownload.selection,
    ).toEqual('op2 admin')
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
    await adminPrograms.filterProgramApplications({searchFragment: 'SARA'})
    const filteredCsvContent = await adminPrograms.getCsv(applyFilters)
    expect(filteredCsvContent).toContain(
      'sarah,,smith,op2 admin,05/10/2021,1000.00',
    )
    expect(filteredCsvContent).not.toContain(
      'Gus,,Guest,op2 admin,01/01/1990,2000.00',
    )
    const filteredJsonContent = await adminPrograms.getJson(applyFilters)
    expect(filteredJsonContent.length).toEqual(1)
    expect(filteredJsonContent[0].application.name.first_name).toEqual('sarah')
    // Ensures that choosing not to apply filters continues to return all
    // results.
    const allCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(allCsvContent).toContain('sarah,,smith,op2 admin,05/10/2021,1000.00')
    expect(allCsvContent).toContain('Gus,,Guest,op2 admin,01/01/1990,2000.00')
    const allJsonContent = await adminPrograms.getJson(noApplyFilters)
    expect(allJsonContent.length).toEqual(3)
    expect(allJsonContent[0].application.name.first_name).toEqual('Gus')
    expect(allJsonContent[1].application.name.first_name).toEqual('Gus')
    expect(allJsonContent[2].application.name.first_name).toEqual('sarah')

    await logout(page)

    // #######################################
    // Test pdf applications export
    // #######################################
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.filterProgramApplications({searchFragment: 'SARA'})
    await adminPrograms.viewApplicationForApplicant('smith, sarah')
    await validateScreenshot(page, 'applications-page')

    const pdfFile = await adminPrograms.getPdf()
    expect(pdfFile.length).toBeGreaterThan(1)

    await logout(page)

    // #######################################
    // Test demography export
    // #######################################
    await loginAsAdmin(page)
    await adminPrograms.gotoAdminProgramsPage()
    const demographicsCsvContent = await adminPrograms.getDemographicsCsv()

    // Export doesn't let us control if Status Tracking is on or off through
    // browser tests, and different environments have it configured differently
    // so test for both situations.
    if (demographicsCsvContent.includes('Status')) {
      expect(demographicsCsvContent).toContain(
        'Opaque ID,Program,Submitter Type,TI Email (Opaque),TI Organization,Create Time,Submit Time,Status,name (first_name),name (middle_name),name (last_name),csvcolor (blue admin),csvcolor (red admin),csvcolor (green admin),csvcolor (orange admin),csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number)',
      )
      expect(demographicsCsvContent).toContain(
        ',sarah,,smith,Selected,Not Selected,Not Selected,Not Selected,1000.00,05/10/2021,op2 admin',
      )
    } else {
      expect(demographicsCsvContent).toContain(
        'Opaque ID,Program,Submitter Type,TI Email (Opaque),TI Organization,Create Time,Submit Time,Status,name (first_name),name (middle_name),name (last_name),csvcolor (red admin),csvcolor (green admin),csvcolor (orange admin),csvcolor (blue admin),csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number)',
      )
      expect(demographicsCsvContent).toContain(
        'sarah,,smith,1000.00,05/10/2021,op2 admin',
      )
    }

    await adminQuestions.createNewVersion('Name')
    await adminQuestions.exportQuestionOpaque('Name')
    await adminPrograms.publishProgram(programName)

    await adminPrograms.gotoAdminProgramsPage()
    const newDemographicsCsvContent = await adminPrograms.getDemographicsCsv()

    if (demographicsCsvContent.includes('Status')) {
      expect(newDemographicsCsvContent).toContain(
        'Opaque ID,Program,Submitter Type,TI Email (Opaque),TI Organization,Create Time,Submit Time,Status,csvcolor (blue admin),csvcolor (red admin),csvcolor (green admin),csvcolor (orange admin),csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number),name (first_name),name (middle_name),name (last_name)',
      )
    } else {
      expect(newDemographicsCsvContent).toContain(
        'Opaque ID,Program,Submitter Type,TI Email (Opaque),TI Organization,Create Time,Submit Time,csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number),name (first_name),name (middle_name),name (last_name)',
      )
    }
    expect(newDemographicsCsvContent).not.toContain(',sarah,,smith')
    expect(newDemographicsCsvContent).toContain(',op2 admin,')
    expect(newDemographicsCsvContent).toContain(',1600,')

    if (isLocalDevEnvironment()) {
      // The hashed values "sarah", empty value, "smith", with the dev secret key.
      expect(newDemographicsCsvContent).toContain(
        '5009769596aa83552389143189cec81abfc8f56abc1bb966715c47ce4078c403,057ba03d6c44104863dc7361fe4578965d1887360f90a0895882e58a6248fc86,6eecddf47b5f7a90d41ccc978c4c785265242ce75fe50be10c824b73a25167ba',
      )
    }
  })

  it('download finished application', async () => {
    const {page, adminQuestions, adminPrograms, applicantQuestions} = ctx

    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'application_exportable')

    const programName = 'Test program'
    await adminQuestions.addNameQuestion({questionName: 'Name'})
    await adminPrograms.addAndPublishProgramWithQuestions(['Name'], programName)

    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.answerNameQuestion('sarah', 'smith')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await validateScreenshot(page, 'application-confirmation')
    await applicantQuestions.downloadFromConfirmationPage()

    await logout(page)
    await loginAsProgramAdmin(page)
  })
})
