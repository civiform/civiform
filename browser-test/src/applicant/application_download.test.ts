import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  seedQuestions,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

test.describe('csv export for multioption question', () => {
  test.beforeEach(async ({page}) => {
    await seedQuestions(page)
    await page.goto('/')
  })
  test('multioption csv into its own column', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    const noApplyFilters = false

    await loginAsAdmin(page)
    const programName = 'Checkbox question program for export'
    await adminQuestions.createCheckboxQuestion(
      {
        questionName: 'csv-color',
        questionText: 'Sample question text',
        helpText: 'Sample question help text',
        options: [
          {adminName: 'red_admin', text: 'red'},
          {adminName: 'green_admin', text: 'green'},
          {adminName: 'orange_admin', text: 'orange'},
          {adminName: 'blue_admin', text: 'blue'},
        ],
        maxNum: 3,
        minNum: 2,
      },
      /* clickSubmit= */ true,
    )
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['Sample Name Question', 'csv-color'],
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
    // deleting red and orange
    await adminQuestions.deleteMultiOptionAnswer(0)
    await adminQuestions.deleteMultiOptionAnswer(1)
    await waitForPageJsLoad(page)
    // adding black and white
    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(2, {
      adminName: 'black_admin',
      text: 'black',
    })
    await page.click('#add-new-option')
    await adminQuestions.fillMultiOptionAnswer(3, {
      adminName: 'white_admin',
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
      'Applicant ID,' +
        'Application ID,' +
        'Applicant Language,' +
        'Submit Time,' +
        'Submitter Type,' +
        'TI Email,' +
        'TI Organization,' +
        'Status,' +
        'csvcolor (selections - red_admin),' +
        'csvcolor (selections - green_admin),' +
        'csvcolor (selections - orange_admin),' +
        'csvcolor (selections - blue_admin),' +
        'csvcolor (selections - black_admin),' +
        'csvcolor (selections - white_admin),' +
        'sample name question (first_name),' +
        'sample name question (middle_name),' +
        'sample name question (last_name),' +
        'sample name question (suffix),',
    )
    // colors headers are - red,green,orange,blue,black,white
    expect(csvContent).toContain(
      'NOT_AN_OPTION_AT_PROGRAM_VERSION,' +
        'SELECTED,' +
        'NOT_AN_OPTION_AT_PROGRAM_VERSION,' +
        'NOT_SELECTED,' +
        'SELECTED,' +
        'NOT_SELECTED,' +
        'John,' +
        ',' +
        'Do,' +
        ',',
    )
    expect(csvContent).toContain(
      'SELECTED,NOT_SELECTED,NOT_SELECTED,SELECTED,NOT_AN_OPTION_AT_PROGRAM_VERSION,NOT_AN_OPTION_AT_PROGRAM_VERSION,Jane,,Doe,,',
    )
  })
})

test.describe('normal application flow', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'bulk_status_update_enabled')
    await seedQuestions(page)
    await page.goto('/')
  })

  test('all major steps', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    test.slow()

    const noApplyFilters = false
    const applyFilters = true

    await loginAsAdmin(page)

    const programName = 'Test program for export'
    await adminQuestions.addDropdownQuestion({
      questionName: 'dropdown-csv-download',
      options: [
        {adminName: 'op1_admin', text: 'op1'},
        {adminName: 'op2_admin', text: 'op2'},
        {adminName: 'op3_admin', text: 'op3'},
      ],
    })
    await adminQuestions.createCheckboxQuestion(
      {
        questionName: 'csv-color',
        questionText: 'Sample question text',
        helpText: 'Sample question help text',
        options: [
          {adminName: 'red_admin', text: 'red'},
          {adminName: 'green_admin', text: 'green'},
          {adminName: 'orange_admin', text: 'orange'},
          {adminName: 'blue_admin', text: 'blue'},
        ],
      },
      /* clickSubmit= */ true,
    )
    await adminQuestions.addDateQuestion({questionName: 'csv-date'})
    await adminQuestions.addCurrencyQuestion({questionName: 'csv-currency'})
    await adminQuestions.exportQuestion('Sample Name Question')
    await adminQuestions.exportQuestion('dropdown-csv-download')
    await adminQuestions.exportQuestion('csv-date')
    await adminQuestions.exportQuestion('csv-currency')
    await adminQuestions.exportQuestion('csv-color')
    await adminPrograms.addAndPublishProgramWithQuestions(
      [
        'Sample Name Question',
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
      'NOT_SELECTED,' + 'NOT_SELECTED,' + 'NOT_SELECTED,' + 'SELECTED',
      +'1000.00,' +
        '05/10/2021,' +
        'op2_admin,' +
        'sarah,' +
        ',' +
        'smith,' +
        ',',
    )

    await logout(page)
    await loginAsAdmin(page)

    // Change export visibility of a question
    await adminQuestions.createNewVersion('dropdown-csv-download')
    await adminQuestions.gotoQuestionEditPage('dropdown-csv-download')
    await page.click(
      '#question-settings .multi-option-question-field-remove-button',
    )
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
    // Edit one answer on the application to prevent a duplicate exception
    await applicantQuestions.clickEdit()
    await applicantQuestions.answerNumberQuestion('1500')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // #######################################
    // Test program applications export
    // #######################################
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    const postEditCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(postEditCsvContent).toContain(
      '1000.00,05/10/2021,op2_admin,,sarah,,smith,,',
    )
    expect(postEditCsvContent).toContain(
      '2000.00,01/01/1990,op2_admin,1500,Gus,,Guest,,',
    )

    const numberOfGusEntries = [...postEditCsvContent.matchAll('Gus')].length
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
    ).toEqual('op2_admin')
    expect(
      postEditJsonContent[0].application.sample_name_question.first_name,
    ).toEqual('Gus')
    expect(
      postEditJsonContent[0].application.sample_name_question.middle_name,
    ).toBeNull()
    expect(
      postEditJsonContent[0].application.sample_name_question.last_name,
    ).toEqual('Guest')
    expect(postEditJsonContent[0].application.csvdate.date).toEqual(
      '1990-01-01',
    )
    expect(postEditJsonContent[0].application.numbercsvdownload.number).toEqual(
      1500,
    )

    // Finds a partial text match on applicant name, case insensitive.
    await adminPrograms.filterProgramApplications({searchFragment: 'SARA'})
    const filteredCsvContent = await adminPrograms.getCsv(applyFilters)
    expect(filteredCsvContent).toContain(
      '1000.00,05/10/2021,op2_admin,sarah,,smith,,',
    )
    expect(filteredCsvContent).not.toContain('Gus')
    const filteredJsonContent = await adminPrograms.getJson(applyFilters)
    expect(filteredJsonContent.length).toEqual(1)
    expect(
      filteredJsonContent[0].application.sample_name_question.first_name,
    ).toEqual('sarah')
    // Ensures that choosing not to apply filters continues to return all
    // results.
    const allCsvContent = await adminPrograms.getCsv(noApplyFilters)
    expect(allCsvContent).toContain(
      '1000.00,05/10/2021,op2_admin,,sarah,,smith,,',
    )
    expect(allCsvContent).toContain(
      '2000.00,01/01/1990,op2_admin,1500,Gus,,Guest,,',
    )
    expect(allCsvContent).toContain(
      '2000.00,01/01/1990,op2_admin,1600,Gus,,Guest,,',
    )
    const allJsonContent = await adminPrograms.getJson(noApplyFilters)
    expect(allJsonContent.length).toEqual(3)
    expect(
      allJsonContent[0].application.sample_name_question.first_name,
    ).toEqual('Gus')
    expect(
      allJsonContent[1].application.sample_name_question.first_name,
    ).toEqual('Gus')
    expect(
      allJsonContent[2].application.sample_name_question.first_name,
    ).toEqual('sarah')

    await logout(page)

    // #######################################
    // Test pdf applications export
    // #######################################
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.filterProgramApplications({searchFragment: 'SARA'})
    await adminPrograms.viewApplicationForApplicant('smith, sarah')
    await validateScreenshot(page, 'applications-page')

    const pdfFile = await adminPrograms.getApplicationPdf()
    expect(pdfFile.length).toBeGreaterThan(1)

    await page.getByRole('link', {name: 'Back'}).click()

    await logout(page)

    // #######################################
    // Test applies filters even when filter button is not clicked
    // #######################################
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)

    await adminPrograms.filterProgramApplications({
      searchFragment: 'SARA',
      clickFilterButton: false,
    })
    const filteredContent = await adminPrograms.getCsv(applyFilters)
    expect(filteredContent).toContain(
      '1000.00,05/10/2021,op2_admin,sarah,,smith,,',
    )
    expect(filteredContent).not.toContain('Gus')

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
        'Opaque ID,Program,Submitter Type,TI Email (Opaque),TI Organization,Create Time,Submit Time,Status,sample name question (first_name),sample name question (middle_name),sample name question (last_name),sample name question (suffix),csvcolor (selections - red_admin),csvcolor (selections - green_admin),csvcolor (selections - orange_admin),csvcolor (selections - blue_admin),csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number)',
      )
      expect(demographicsCsvContent).toContain(
        'sarah,,smith,,NOT_SELECTED,NOT_SELECTED,NOT_SELECTED,SELECTED,1000.00,05/10/2021,op2_admin,',
      )
    } else {
      expect(demographicsCsvContent).toContain(
        'Opaque ID,Program,Submitter Type,TI Email (Opaque),TI Organization,Create Time,Submit Time,Status,sample name question (first_name),sample name question (middle_name),sample name question (last_name),sample name question (suffix),csvcolor (selections - red_admin),csvcolor (selections - green_admin),csvcolor (selections - orange_admin),csvcolor (selections - blue_admin),csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number)',
      )
      expect(demographicsCsvContent).toContain(
        'sarah,,smith,,1000.00,05/10/2021,op2_admin',
      )
    }

    await adminQuestions.createNewVersion('Sample Name Question')
    await adminQuestions.exportQuestionOpaque('Sample Name Question')
    await adminPrograms.publishProgram(programName)

    await adminPrograms.gotoAdminProgramsPage()
    const newDemographicsCsvContent = await adminPrograms.getDemographicsCsv()

    if (demographicsCsvContent.includes('Status')) {
      expect(newDemographicsCsvContent).toContain(
        'Opaque ID,Program,Submitter Type,TI Email (Opaque),TI Organization,Create Time,Submit Time,Status,csvcolor (selections - red_admin),csvcolor (selections - green_admin),csvcolor (selections - orange_admin),csvcolor (selections - blue_admin),csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number),sample name question (first_name),sample name question (middle_name),sample name question (last_name),sample name question (suffix)',
      )
    } else {
      expect(newDemographicsCsvContent).toContain(
        'Opaque ID,Program,Submitter Type,TI Email (Opaque),TI Organization,Create Time,Submit Time,csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number),sample name question (first_name),sample name question (middle_name),sample name question (last_name),sample name question (name_suffix)',
      )
    }
    expect(newDemographicsCsvContent).not.toContain(',sarah,,smith')
    expect(newDemographicsCsvContent).toContain(',op2_admin,')
    expect(newDemographicsCsvContent).toContain(',1600,')

    if (isLocalDevEnvironment()) {
      // The hashed values "sarah", empty value, "smith", with the dev secret key.
      expect(newDemographicsCsvContent).toContain(
        '5009769596aa83552389143189cec81abfc8f56abc1bb966715c47ce4078c403,057ba03d6c44104863dc7361fe4578965d1887360f90a0895882e58a6248fc86,6eecddf47b5f7a90d41ccc978c4c785265242ce75fe50be10c824b73a25167ba',
      )
    }
  })

  test('download finished application', async ({
    page,
    adminPrograms,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'application_exportable')

    const programName = 'Test program'
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['Sample Name Question'],
      programName,
    )

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
