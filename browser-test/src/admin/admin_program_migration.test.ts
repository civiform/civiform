import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'
import {
  ProgramLifecycle,
  ProgramType,
  ProgramVisibility,
} from '../support/admin_programs'

test.describe('program migration', () => {
  // These values should be kept in sync with USWDS Alert style classes in views/style/BaseStyles.java.
  const ALERT_WARNING = 'usa-alert--warning'
  const ALERT_ERROR = 'usa-alert--error'
  const ALERT_SUCCESS = 'usa-alert--success'

  test('export a program', async ({
    page,
    adminPrograms,
    adminProgramMigration,
    adminQuestions,
  }) => {
    const programName = 'Program 1'
    const dateQuestionText = 'What is your birthday?'
    const emailQuestionText = 'What is your email?'
    const phoneQuestionText = 'What is your phone number?'
    const idQuestionText = 'What is your id number?'
    const block1Description = 'Birthday block'
    const block2Description = 'Key information block'

    await test.step('add draft program', async () => {
      await loginAsAdmin(page)

      await adminQuestions.addDateQuestion({
        questionName: 'date-q',
        questionText: dateQuestionText,
      })
      await adminQuestions.addEmailQuestion({
        questionName: 'email-q',
        questionText: emailQuestionText,
      })
      await adminQuestions.addPhoneQuestion({
        questionName: 'phone-q',
        questionText: phoneQuestionText,
      })
      await adminQuestions.addIdQuestion({
        questionName: 'id-q',
        questionText: idQuestionText,
        minNum: 1,
        maxNum: 5,
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, block1Description, [
        'date-q',
        'id-q',
      ])
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: block2Description,
        questions: [
          {name: 'email-q', isOptional: false},
          {name: 'phone-q', isOptional: true},
        ],
      })
    })

    await test.step('load export page', async () => {
      await adminPrograms.goToExportProgramPage(
        programName,
        ProgramLifecycle.DRAFT,
      )

      const jsonPreview = await adminProgramMigration.expectJsonPreview()
      expect(jsonPreview).toContain(programName)
      expect(jsonPreview).toContain(block1Description)
      expect(jsonPreview).toContain(block2Description)
      expect(jsonPreview).toContain(dateQuestionText)
      expect(jsonPreview).toContain(emailQuestionText)
      expect(jsonPreview).toContain(phoneQuestionText)
      expect(jsonPreview).toContain(idQuestionText)
      expect(jsonPreview).toContain('validationPredicates')
      expect(jsonPreview).toContain('"type" : "id"')
      expect(jsonPreview).toContain('"minLength" : 1')
      expect(jsonPreview).toContain('"maxLength" : 5')
    })
    await test.step('download json for program 2', async () => {
      const downloadedProgram = await adminProgramMigration.downloadJson()
      expect(downloadedProgram).toContain(programName)
      expect(downloadedProgram).toContain(block1Description)
      expect(downloadedProgram).toContain(block2Description)
      expect(downloadedProgram).toContain(dateQuestionText)
      expect(downloadedProgram).toContain(emailQuestionText)
      expect(downloadedProgram).toContain(phoneQuestionText)
      expect(downloadedProgram).toContain(idQuestionText)
      expect(downloadedProgram).toContain('validationPredicates')
      expect(downloadedProgram).toContain('"type" : "id"')
      expect(downloadedProgram).toContain('"minLength" : 1')
      expect(downloadedProgram).toContain('"maxLength" : 5')
    })
    await test.step('click back button to return to programs index page', async () => {
      await adminProgramMigration.clickBackButton()
      await adminPrograms.expectAdminProgramsPage()
    })

    // TODO(#7582): Add a test to test that clicking the "Copy JSON" button works
  })

  test('import errors', async ({
    page,
    adminPrograms,
    adminProgramMigration,
    adminTiGroups,
    seeding,
  }) => {
    test.slow()

    await test.step('load import page', async () => {
      await loginAsAdmin(page)
      await adminPrograms.gotoAdminProgramsPage()
      await adminProgramMigration.goToImportPage()
    })

    await test.step('malformed: missing "', async () => {
      await adminProgramMigration.submitProgramJson(
        '{"adminName: "mismatched-double-quote"}',
      )
      await adminProgramMigration.expectAlert(
        'Error processing JSON',
        ALERT_ERROR,
      )
      await validateScreenshot(
        page.locator('main'),
        'import-page-with-error-parse',
      )
    })

    await test.step('malformed: not matching {}', async () => {
      await adminProgramMigration.clickButton('Try again')
      await adminProgramMigration.submitProgramJson(
        '{"adminName": "mismatched-brackets"',
      )
      await adminProgramMigration.expectAlert(
        'Error processing JSON',
        ALERT_ERROR,
      )
    })

    await test.step('malformed: missing ,', async () => {
      await adminProgramMigration.clickButton('Try again')
      await adminProgramMigration.submitProgramJson(
        '{"adminName": "missing-comma" "adminDescription": "missing-comma-description"}',
      )
      await adminProgramMigration.expectAlert('Error processing JSON')
    })

    await test.step('malformed: missing program field', async () => {
      await adminProgramMigration.clickButton('Try again')
      // The JSON itself is correctly formatted but it should have a top-level "program" field
      await adminProgramMigration.submitProgramJson(
        '{"adminName": "missing-program-field", "adminDescription": "missing-field-description"}',
      )
      await adminProgramMigration.expectAlert(
        'Error processing JSON',
        ALERT_ERROR,
      )
    })

    await test.step('malformed: missing required program info', async () => {
      await adminProgramMigration.clickButton('Try again')
      // The JSON itself is correctly formatted but it doesn't have all the fields
      // that we need to build a ProgramDefinition
      await adminProgramMigration.submitProgramJson(
        '{"program": {"adminName": "missing-fields", "adminDescription": "missing-fields-description"}}',
      )
      await adminProgramMigration.expectAlert(
        'Error processing JSON',
        ALERT_ERROR,
      )
      await validateScreenshot(
        page,
        'import-page-with-error-missing-program-fields',
      )
    })

    await seeding.seedProgramsAndCategories()
    await page.goto('/')
    await adminPrograms.goToExportProgramPage(
      'Comprehensive Sample Program',
      ProgramLifecycle.DRAFT,
    )
    const downloadedComprehensiveProgram =
      await adminProgramMigration.downloadJson()

    await test.step('error: program already exists', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await adminProgramMigration.goToImportPage()
      await adminProgramMigration.submitProgramJson(
        downloadedComprehensiveProgram,
      )
      await adminProgramMigration.expectAlert(
        'This program already exists in our system.',
        ALERT_ERROR,
      )
      await validateScreenshot(
        page,
        'import-page-with-error-program-already-exists',
      )
    })

    await test.step('error: invalid program slug', async () => {
      // this tests that we will catch errors that bubble up from programService.validateProgramDataForCreate
      // there are other errors that might bubble up (such as a blank program name) but we don't need to test them all
      await adminProgramMigration.clickButton('Try again')
      // replace the program slug with an invalid slug to trigger an error
      const comprehensiveProgramBadName =
        downloadedComprehensiveProgram.replace(
          'comprehensive-sample-program',
          'comprehensive-sample-program ##4L!',
        )
      await adminProgramMigration.submitProgramJson(comprehensiveProgramBadName)
      await adminProgramMigration.expectAlert(
        'One or more program errors occured:',
        ALERT_ERROR,
      )
      await adminProgramMigration.expectAlert(
        'A program slug may only contain lowercase letters, numbers, and dashes.',
        ALERT_ERROR,
      )
    })

    await test.step('error: invalid question admin name', async () => {
      // this tests that we will catch errors that bubble up from the questionDefinition.validate
      // there are other errors that might bubble up (such as a blank question text) but we don't need to test them all
      await adminProgramMigration.clickButton('Try again')
      // set the program admin name back to a valid admin name
      const comprehensiveProgramBadQuestionName = downloadedComprehensiveProgram
        .replace(
          'comprehensive-sample-program',
          'comprehensive-sample-program-new',
        )
        // replace the question admin name with a blank string to trigger an error
        .replace('Sample Address Question', '')
        // replace one of the multi-option question admin names with an invalid admin name
        // we should not be validating multi-option question admin names as a part of program migration
        .replace('chocolate', 'Chocolate ice cream')

      await adminProgramMigration.submitProgramJson(
        comprehensiveProgramBadQuestionName,
      )
      const alert = await adminProgramMigration.expectAlert(
        'One or more question errors occured:',
        ALERT_ERROR,
      )
      await expect(alert).toContainText(
        'Administrative identifier cannot be blank.',
      )
      await expect(alert).not.toContainText(
        'Multi-option admin names can only contain lowercase letters, numbers, underscores, and dashes',
      )
    })

    await test.step('error: SELECT_TI visibility not allowed', async () => {
      await adminTiGroups.gotoAdminTIPage()
      await adminTiGroups.fillInGroupBasics('groupOne', 'groupOne description')
      await adminTiGroups.editGroup('groupOne')
      await adminTiGroups.addGroupMember('groupOne@bar.com')
      await adminPrograms.addProgram(
        'New Program',
        'program description',
        'short program description',
        'https://usa.gov',
        ProgramVisibility.SELECT_TI,
        'admin description',
        ProgramType.DEFAULT,
        'groupOne',
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.goToExportProgramPage('New Program', 'DRAFT')
      let downloadedProgram = await adminProgramMigration.downloadJson()
      await adminPrograms.gotoAdminProgramsPage()
      await adminProgramMigration.goToImportPage()
      // replace admin name to avoid collision
      downloadedProgram = downloadedProgram.replace(
        'new-program',
        'new-new-program',
      )
      await adminProgramMigration.submitProgramJson(downloadedProgram)
      await adminProgramMigration.expectAlert(
        "Display mode 'SELECT_TI' is not allowed.",
      )
    })

    await test.step('error: question in program not defined in questions', async () => {
      await adminProgramMigration.clickButton('Try again')
      // set the program admin name to a valid admin name
      const comprehensiveProgramQuestionNotDefined =
        downloadedComprehensiveProgram
          .replace(
            'comprehensive-sample-program',
            'comprehensive-sample-program-new',
          )
          // replace the first occurrence of a question ID with a different one, so it doesn't match the question definition's ID
          .replace(/"id" : [0-9]+,[\s]+"optional"/m, '"id" : 9999, "optional"')

      await adminProgramMigration.submitProgramJson(
        comprehensiveProgramQuestionNotDefined,
      )

      const alert = await adminProgramMigration.expectAlert(
        'One or more question errors occured:',
        ALERT_ERROR,
      )
      await expect(alert).toContainText('Question ID 9999 is not defined.')
      await expect(alert).not.toContainText(
        'Administrative identifier cannot be blank.',
      )
    })

    await test.step('error: overwriting question with existing drafts', async () => {
      await adminProgramMigration.clickButton('Try again')
      // set the program admin name to a valid admin name
      const comprehensiveProgramQuestionNotDefined =
        downloadedComprehensiveProgram.replace(
          'comprehensive-sample-program',
          'comprehensive-sample-program-new',
        )
      await adminProgramMigration.submitProgramJson(
        comprehensiveProgramQuestionNotDefined,
      )
      await adminProgramMigration.selectToplevelOverwriteExisting()

      await adminProgramMigration.clickButtonWithSpinner('Save')

      const alert = await adminProgramMigration.expectAlert(
        'Error saving program',
        ALERT_ERROR,
      )
      await expect(alert).toContainText(
        'Overwriting question definitions is only supported when there are no existing drafts. Please publish all drafts and try again.',
      )
    })
  })

  test('export then import', async ({
    page,
    adminPrograms,
    adminProgramMigration,
    seeding,
  }) => {
    await test.step('seed programs', async () => {
      await seeding.seedProgramsAndCategories()
      await page.goto('/')
      await loginAsAdmin(page)
    })

    let downloadedComprehensiveProgram: string
    await test.step('export comprehensive program', async () => {
      await adminPrograms.goToExportProgramPage(
        'Comprehensive Sample Program',
        ProgramLifecycle.DRAFT,
      )
      downloadedComprehensiveProgram =
        await adminProgramMigration.downloadJson()
      expect(downloadedComprehensiveProgram).toContain(
        'comprehensive-sample-program',
      )
    })

    await test.step('edit the comprehensive program JSON', () => {
      // Replace the admin name so you don't get an error
      downloadedComprehensiveProgram = downloadedComprehensiveProgram.replace(
        'comprehensive-sample-program',
        'comprehensive-sample-program-new',
      )
      // Replace the program title so we can validate the new one shows up
      downloadedComprehensiveProgram = downloadedComprehensiveProgram.replace(
        'Comprehensive Sample Program',
        'Comprehensive Sample Program New',
      )
      // Replace one admin name of a question so we can see the "New Question" badge
      downloadedComprehensiveProgram = downloadedComprehensiveProgram.replace(
        'Sample Checkbox',
        'Sample Checkbox-new',
      )
      // Replace the question text of a question so we can overwrite its existing definition
      // or check that reusing the existing definition did not update its definition
      downloadedComprehensiveProgram = downloadedComprehensiveProgram.replace(
        'How many pets do you have?',
        'How many LARGE pets do you have?',
      )
      downloadedComprehensiveProgram = downloadedComprehensiveProgram.replace(
        'What is your favorite color?',
        'What is your LEAST favorite color?',
      )
    })

    await test.step('import comprehensive program', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await adminProgramMigration.goToImportPage()
      await adminProgramMigration.submitProgramJson(
        downloadedComprehensiveProgram,
      )
    })

    await test.step('check imported comprehensive program', async () => {
      // Assert the title and admin name are shown
      await expect(
        page.getByRole('heading', {
          name: 'Comprehensive Sample Program',
        }),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {
          name: 'Admin name: comprehensive-sample-program-new',
        }),
      ).toBeVisible()

      // Assert all the blocks are shown
      await expect(page.getByRole('heading', {name: 'Screen 1'})).toBeVisible()
      await expect(page.getByRole('heading', {name: 'Screen 2'})).toBeVisible()
      // 'exact: true' distinguishes this heading from the 'repeated screen for enumerator' heading
      await expect(
        page.getByRole('heading', {name: 'enumerator', exact: true}),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {name: 'repeated screen for enumerator'}),
      ).toBeVisible()
      await expect(page.getByRole('heading', {name: 'Screen 3'})).toBeVisible()
      await expect(
        page.getByRole('heading', {name: 'Screen with Predicate'}),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {name: 'file upload'}),
      ).toBeVisible()

      // Assert the warning about duplicate question names is shown
      await adminProgramMigration.expectAlert(
        'This program will add 1 new question to the question bank and contains 16 duplicate questions that must be resolved.',
        ALERT_WARNING,
      )

      // Assert all the questions are shown
      const allQuestions = page.getByTestId('question-div')
      await expect(allQuestions).toHaveCount(17)
      // Check that all the expected fields are shown (on at least one question)
      const addressQuestion = page.getByTestId(
        'question-admin-name-Sample Address Question',
      )
      // Duplicate badge
      await expect(addressQuestion).toContainText('Duplicate Question')
      // Question text & help text
      await expect(addressQuestion).toContainText('What is your address?')
      await expect(addressQuestion).toContainText('help text')
      // admin name (should be the same as the imported admin name)
      await expect(addressQuestion).toContainText(
        'Admin ID: Sample Address Question',
      )

      // Check another question for multi-select and "New Question" badge
      const checkboxQuestion = page.getByTestId(
        'question-admin-name-Sample Checkbox-new Question',
      )
      // Question options (for multi-select question)
      await expect(checkboxQuestion).toContainText('Toaster')
      await expect(checkboxQuestion).toContainText('Pepper Grinder')
      await expect(checkboxQuestion).toContainText('Garlic Press')
      // Badges
      await expect(checkboxQuestion).toContainText('New Question')

      // Check that the universal badge is shown (on at least one question)
      await adminPrograms.expectQuestionCardUniversalBadgeState(
        'Sample Date Question',
        true,
      )
      // The default option is "reuse", but we test the top-level selector changes all questions to "overwrite"
      await adminProgramMigration.selectToplevelOverwriteExisting()
      await adminProgramMigration.expectAllQuestionsHaveDuplicateHandlingOption(
        adminProgramMigration.OVERWRITE_EXISTING,
      )
      // Select "reuse" and "duplicate" on a few questions to validate the UI, also resetting the top-level selector
      await adminProgramMigration.selectDuplicateHandlingForQuestions(
        new Map([
          ['Sample Address Question', adminProgramMigration.USE_EXISTING],
          ['Sample Number Question', adminProgramMigration.USE_EXISTING],
          ['Sample Currency Question', adminProgramMigration.CREATE_DUPLICATE],
          ['Sample Email Question', adminProgramMigration.CREATE_DUPLICATE],
        ]),
      )
      await adminProgramMigration.expectOptionSelected(
        page.getByTestId('toplevel-duplicate-handling'),
        'Decide for each duplicate question individually',
      )

      // Check that the UI looks the same
      await validateScreenshot(page.locator('main'), 'import-page-with-data')
    })

    await test.step('delete the program and start over without saving', async () => {
      await adminProgramMigration.clickButton('Delete and start over')
      await adminProgramMigration.expectImportPage()
      await expect(page.getByRole('textbox')).toHaveValue('')
    })

    await test.step('submit the comprehensive sample program JSON', async () => {
      // Publish all drafts so we can overwrite questions without an error
      await adminPrograms.publishAllDrafts()
      await adminProgramMigration.goToImportPage()
      await adminProgramMigration.submitProgramJson(
        downloadedComprehensiveProgram,
      )
      await adminProgramMigration.expectAlert(
        'This program will add 1 new question to the question bank and contains 16 duplicate questions that must be resolved.',
        ALERT_WARNING,
      )
    })

    await test.step('save the comprehensive sample program', async () => {
      // Select "overwrite" and "duplicate" on a few questions to validate the backend behavior
      await adminProgramMigration.selectDuplicateHandlingForQuestions(
        new Map([
          // Has the same definition - basically just won't create a duplicate
          ['Sample Address Question', adminProgramMigration.OVERWRITE_EXISTING],
          // Changes the question text
          ['Sample Number Question', adminProgramMigration.OVERWRITE_EXISTING],
          ['Sample Currency Question', adminProgramMigration.CREATE_DUPLICATE],
          ['Sample Email Question', adminProgramMigration.CREATE_DUPLICATE],
        ]),
      )
      await adminProgramMigration.clickButtonWithSpinner('Save')
      await adminProgramMigration.expectAlert(
        'Your program has been successfully imported',
        ALERT_SUCCESS,
      )
      await validateScreenshot(page, 'saved-program-success')
    })

    await test.step('confirm info on program edit page', async () => {
      await adminProgramMigration.clickButton('View program')
      await expect(page.locator('#program-title')).toContainText(
        'Comprehensive Sample Program New',
      )
      await expect(page.locator('#header_edit_button')).toBeVisible()
      await page.getByText('Screen 1').click()
      // We overwrote this definition, so we shouldn't see a duplicate name
      await expect(
        page.getByTestId('question-admin-name-Sample Address Question'),
      ).toContainText('Sample Address Question')
      await expect(
        page.getByTestId('question-admin-name-Sample Address Question'),
      ).not.toContainText('Sample Address Question -_-')
      // We created duplicates, so we should see the de-duped/suffixed name
      await expect(
        page.getByTestId('question-admin-name-Sample Currency Question -_- a'),
      ).toContainText('Sample Currency Question -_- a')
      await page.getByText('Screen 2').click()
      await expect(
        page.getByTestId('question-admin-name-Sample Email Question -_- a'),
      ).toContainText('Sample Email Question -_- a')
      // We overwrote this definition, so we should see the updated question text
      await expect(
        page.getByTestId('question-admin-name-Sample Number Question'),
      ).toContainText('How many LARGE pets do you have?')
      // We reused the existing definition, so we shouldn't see this text update
      await expect(
        page.getByTestId('question-admin-name-Sample Text Question'),
      ).toContainText('What is your favorite color?')
      await expect(
        page.getByTestId('question-admin-name-Sample Text Question'),
      ).not.toContainText('What is your LEAST favorite color?')
    })

    await test.step('publish all programs', async () => {
      // Check that other program is in DRAFT
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.expectDraftProgram('Comprehensive Sample Program')

      // Confirm that we are able to publish all drafts
      await adminPrograms.publishAllDrafts()
      await adminPrograms.expectAdminProgramsPage()
      await adminPrograms.expectDoesNotHaveDraftProgram(
        'Comprehensive Sample Program',
      )
      await adminPrograms.expectActiveProgram('Comprehensive Sample Program')
      await adminPrograms.expectDoesNotHaveDraftProgram(
        'Comprehensive Sample Program New',
      )
      await adminPrograms.expectActiveProgram(
        'Comprehensive Sample Program New',
      )
    })
  })

  test('Importing with duplicate enumerators and repeated questions', async ({
    page,
    adminPrograms,
    adminProgramMigration,
    seeding,
  }) => {
    await test.step('seed programs', async () => {
      await seeding.seedProgramsAndCategories()
      await page.goto('/')
      await loginAsAdmin(page)
    })

    let downloadedComprehensiveProgram: string
    await test.step('export comprehensive program', async () => {
      await adminPrograms.goToExportProgramPage(
        'Comprehensive Sample Program',
        ProgramLifecycle.DRAFT,
      )
      downloadedComprehensiveProgram =
        await adminProgramMigration.downloadJson()
      expect(downloadedComprehensiveProgram).toContain(
        'comprehensive-sample-program',
      )
    })

    await test.step('edit the comprehensive program JSON', () => {
      // Replace the admin name so you don't get an error
      downloadedComprehensiveProgram = downloadedComprehensiveProgram.replace(
        'comprehensive-sample-program',
        'comprehensive-sample-program-new',
      )
      // Replace the program title so we can validate the new one shows up
      downloadedComprehensiveProgram = downloadedComprehensiveProgram.replace(
        'Comprehensive Sample Program',
        'Comprehensive Sample Program New',
      )
    })

    await test.step('import comprehensive program', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await adminProgramMigration.goToImportPage()
      await adminProgramMigration.submitProgramJson(
        downloadedComprehensiveProgram,
      )
    })

    await test.step('check imported comprehensive program', async () => {
      // Assert the title and admin name are shown
      await expect(
        page.getByRole('heading', {
          name: 'Comprehensive Sample Program New',
        }),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {
          name: 'Admin name: comprehensive-sample-program-new',
        }),
      ).toBeVisible()

      // Assert the warning about duplicate question names is shown
      await adminProgramMigration.expectAlert(
        'This program contains 17 duplicate questions that must be resolved.',
        ALERT_WARNING,
      )

      const enumAlert = page.getByRole('alert').filter({
        hasText:
          "Duplicate repeated questions of this enumerator will also be set to 'Create a new duplicate question.'",
      })
      const repeatedAlert = page.getByRole('alert').filter({
        hasText:
          "Some options are disabled because the associated enumerator is set to 'Create a new duplicate question.'",
      })
      // When all questions are on the default "use existing" option, we can still select any option for repeated questions
      await expect(enumAlert).toBeHidden()
      await expect(repeatedAlert).toBeHidden()
      await adminProgramMigration.selectDuplicateHandlingForQuestions(
        new Map([
          // Just confirm that we are able to click this option
          [
            'Sample Enumerated Date Question',
            adminProgramMigration.OVERWRITE_EXISTING,
          ],
        ]),
      )
      // Set the enumerator to "Create a new duplicate" to see how it affects its repeated question(s)
      await adminProgramMigration.selectDuplicateHandlingForQuestions(
        new Map([
          [
            'Sample Enumerator Question',
            adminProgramMigration.CREATE_DUPLICATE,
          ],
        ]),
      )
      // Now we see some alerts and cannot adjust the repeated question handling
      await expect(enumAlert).toBeVisible()
      await expect(repeatedAlert).toBeVisible()
      await adminProgramMigration.expectOptionSelected(
        page.getByTestId('question-admin-name-Sample Enumerated Date Question'),
        'Create a new duplicate question',
      )
      await adminProgramMigration.expectOptionsDisabledForQuestion(
        new Map([
          // We should no longer be able to select this
          [
            'Sample Enumerated Date Question',
            adminProgramMigration.OVERWRITE_EXISTING,
          ],
          [
            'Sample Enumerated Date Question',
            adminProgramMigration.USE_EXISTING,
          ],
        ]),
      )
      // Validate the visual alerts and disabled options
      await validateScreenshot(page, 'import-page-with-enumerator-duplicates')
      // Set the enumerator to something else to "unlock" the repeated Qs
      await adminProgramMigration.selectDuplicateHandlingForQuestions(
        new Map([
          ['Sample Enumerator Question', adminProgramMigration.USE_EXISTING],
        ]),
      )
      await expect(enumAlert).toBeHidden()
      await expect(repeatedAlert).toBeHidden()
      await adminProgramMigration.selectDuplicateHandlingForQuestions(
        new Map([
          // Just confirm that we are able to click this option
          [
            'Sample Enumerated Date Question',
            adminProgramMigration.OVERWRITE_EXISTING,
          ],
        ]),
      )
    })
  })
})
