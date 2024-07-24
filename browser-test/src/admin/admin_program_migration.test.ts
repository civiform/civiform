import {expect, test} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  seedProgramsAndCategories,
  validateScreenshot,
} from '../support'

test.describe('program migration', () => {
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
      await enableFeatureFlag(page, 'program_migration_enabled')
      await adminPrograms.gotoExportProgramPage(programName, 'DRAFT')

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

  test('import errors', async ({page, adminProgramMigration}) => {
    await test.step('load import page', async () => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_migration_enabled')
      await adminProgramMigration.goToImportPage()
    })

    await test.step('malformed: missing "', async () => {
      await adminProgramMigration.submitProgramJson(
        '{"adminName: "mismatched-double-quote"}',
      )
      await adminProgramMigration.expectAlert('Error processing JSON')
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
      await adminProgramMigration.expectAlert('Error processing JSON')
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
      await adminProgramMigration.expectAlert('Error processing JSON')
    })

    await test.step('malformed: missing required program info', async () => {
      await adminProgramMigration.clickButton('Try again')
      // The JSON itself is correctly formatted but it doesn't have all the fields
      // that we need to build a ProgramDefinition
      await adminProgramMigration.submitProgramJson(
        '{"program": {"adminName": "missing-fields", "adminDescription": "missing-fields-description"}}',
      )
      await adminProgramMigration.expectAlert('Error processing JSON')
      await validateScreenshot(
        page,
        'import-page-with-error-missing-program-fields',
      )
    })
  })

  test('export then import', async ({
    page,
    adminPrograms,
    adminProgramMigration,
  }) => {
    await test.step('seed programs', async () => {
      // Force this to be disabled for the time being. I think it's causing intermittent issues
      // because the flag may have been enabled in a different run
      await disableFeatureFlag(page, 'multiple_file_upload_enabled')

      await seedProgramsAndCategories(page)
      await page.goto('/')
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_migration_enabled')
    })

    let downloadedComprehensiveProgram: string
    await test.step('export comprehensive program', async () => {
      await adminPrograms.gotoExportProgramPage(
        'Comprehensive Sample Program',
        'DRAFT',
      )
      downloadedComprehensiveProgram =
        await adminProgramMigration.downloadJson()
      expect(downloadedComprehensiveProgram).toContain(
        'comprehensive-sample-program',
      )
    })

    let downloadedMinimalProgram: string
    await test.step('export minimal program', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.gotoExportProgramPage(
        'Minimal Sample Program',
        'DRAFT',
      )
      downloadedMinimalProgram = await adminProgramMigration.downloadJson()
      expect(downloadedMinimalProgram).toContain('minimal-sample-program')
    })

    await test.step('import comprehensive program', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await adminProgramMigration.goToImportPage()
      await validateScreenshot(page.locator('main'), 'import-page-no-data')

      await adminProgramMigration.submitProgramJson(
        downloadedComprehensiveProgram,
      )

      // Assert the title and admin name are shown
      await expect(
        page.getByRole('heading', {
          name: 'Program name: Comprehensive Sample Program',
        }),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {
          name: 'Admin name: comprehensive-sample-program',
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
      // Assert all the questions are shown
      const allQuestions = page.getByTestId('question-div')
      await expect(allQuestions).toHaveCount(17)
      // Check that all the expected fields are shown on at least one question
      const programDataDiv = page.locator('#program-data')
      // question text
      await expect(programDataDiv).toContainText('What is your address?')
      // question help text
      await expect(programDataDiv).toContainText('help text')
      // admin name
      await expect(programDataDiv).toContainText(
        'Admin name: Sample Address Question',
      )
      // admin description
      await expect(programDataDiv).toContainText(
        'Admin description: description',
      )
      // question type
      await expect(programDataDiv).toContainText('Question type: ADDRESS')
      // question options (for multi-select question)
      await expect(programDataDiv).toContainText('Toaster')
      await expect(programDataDiv).toContainText('Pepper Grinder')
      await expect(programDataDiv).toContainText('Garlic Press')
    })

    await test.step('delete the program and start over without saving', async () => {
      await adminProgramMigration.clickButton('Delete and start over')
      await adminProgramMigration.expectImportPage()
      await expect(page.getByRole('textbox')).toHaveValue('')
    })

    await test.step('attempt to save the program and see error', async () => {
      await adminProgramMigration.submitProgramJson(
        downloadedComprehensiveProgram,
      )
      await adminProgramMigration.clickButton('Save')
      // a db error is thrown because there is already a draft of this program
      await adminProgramMigration.expectAlert(
        'Unable to save program. Please try again or contact your IT team for support.',
      )
      await validateScreenshot(page, 'error-saving-program')
    })

    await test.step('publish the program and attempt to import it again', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllDrafts()
      await adminPrograms.gotoExportProgramPage(
        'Comprehensive Sample Program',
        'ACTIVE',
      )
      downloadedComprehensiveProgram =
        await adminProgramMigration.downloadJson()
      await adminPrograms.gotoAdminProgramsPage()
      await adminProgramMigration.goToImportPage()
      await adminProgramMigration.submitProgramJson(
        downloadedComprehensiveProgram,
      )
      await adminProgramMigration.clickButton('Save')
      // the save is successful because no draft already exists
      await adminProgramMigration.expectAlert(
        'Your program has been successfully imported',
      )
      await validateScreenshot(page, 'saved-program-success')
    })

    await test.step('import another program and go directly to program edit page', async () => {
      await adminProgramMigration.clickButton('Import another program')
      await adminProgramMigration.expectImportPage()
      await expect(page.getByRole('textbox')).toHaveValue('')
      // replace the question admin name to avoid draft collision
      downloadedMinimalProgram = downloadedMinimalProgram.replace(
        'Sample Name Question',
        'Sample Name Question 2',
      )
      await adminProgramMigration.submitProgramJson(downloadedMinimalProgram)
      await adminProgramMigration.clickButton('Save')
      await adminProgramMigration.clickButton('View program')
      // confirm we are on the page to edit the program
      await expect(page.locator('#program-title')).toContainText(
        'Minimal Sample Program',
      )
      await expect(page.locator('#header_edit_button')).toBeVisible()
    })
  })
})
