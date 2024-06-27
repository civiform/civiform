import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  seedPrograms,
  validateScreenshot,
} from '../support'

test.describe('program migration', () => {
  test('export a program', async ({
    page,
    adminPrograms,
    adminProgramMigration,
    adminQuestions,
  }) => {
    const programName = 'Program 2'
    const dateQuestionText = 'What is your birthday?'
    const emailQuestionText = 'What is your email?'
    const phoneQuestionText = 'What is your phone number?'
    const idQuestionText = 'What is your id number?'
    const block1Description = 'Birthday block'
    const block2Description = 'Key information block'
    const generateJSONButton = 'Generate JSON'

    await test.step('add two active programs', async () => {
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

      await adminPrograms.addProgram('Program 1')

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

      await adminPrograms.publishAllDrafts()
    })

    await test.step('load export page', async () => {
      await enableFeatureFlag(page, 'program_migration_enabled')
      await adminProgramMigration.goToExportPage()

      // The "Generate JSON" button is disabled by default
      await expect(
        page.getByRole('button', {name: generateJSONButton}),
      ).toBeDisabled()
      await validateScreenshot(page.locator('main'), 'export-page')
    })

    await test.step('generate json for program 2', async () => {
      await adminProgramMigration.selectProgramToExport('program-2')
      // Selecting a program enables the "Generate JSON" button
      await expect(
        page.getByRole('button', {name: generateJSONButton}),
      ).toBeEnabled()

      await adminProgramMigration.generateJson()
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
      await adminProgramMigration.expectImportError()
      await validateScreenshot(
        page.locator('main'),
        'import-page-with-error-parse',
      )
    })

    await test.step('malformed: not matching {}', async () => {
      await adminProgramMigration.submitProgramJson(
        '{"adminName": "mismatched-brackets"',
      )
      await adminProgramMigration.expectImportError()
    })

    await test.step('malformed: missing ,', async () => {
      await adminProgramMigration.submitProgramJson(
        '{"adminName": "missing-comma" "adminDescription": "missing-comma-description"}',
      )
      await adminProgramMigration.expectImportError()
    })

    await test.step('malformed: missing program field', async () => {
      // The JSON itself is correctly formatted but it should have a top-level "program" field
      await adminProgramMigration.submitProgramJson(
        '{"adminName": "missing-program-field", "adminDescription": "missing-field-description"}',
      )
      await adminProgramMigration.expectImportError()
    })

    await test.step('malformed: missing required program info', async () => {
      // The JSON itself is correctly formatted but it doesn't have all the fields
      // that we need to build a ProgramDefinition
      await adminProgramMigration.submitProgramJson(
        '{"program": {"adminName": "missing-fields", "adminDescription": "missing-fields-description"}}',
      )
      await adminProgramMigration.expectImportError()
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
    await test.step('seed comprehensive program', async () => {
      await seedPrograms(page)
      await page.goto('/')
      await loginAsAdmin(page)
      await adminPrograms.publishAllDrafts()
      await enableFeatureFlag(page, 'program_migration_enabled')
    })

    let downloadedProgram: string
    await test.step('export comprehensive program', async () => {
      await adminProgramMigration.goToExportPage()
      await adminProgramMigration.selectProgramToExport(
        'comprehensive-sample-program',
      )
      await adminProgramMigration.generateJson()
      downloadedProgram = await adminProgramMigration.downloadJson()
      expect(downloadedProgram).toContain('comprehensive-sample-program')
    })

    await test.step('import comprehensive program', async () => {
      await adminProgramMigration.goToImportPage()
      await validateScreenshot(page.locator('main'), 'import-page-no-data')

      // replace the admin name to avoid collision
      downloadedProgram = downloadedProgram.replace(
        'comprehensive-sample-program',
        'comprehensive-sample-program-2',
      )
      // replace the program title so can confirm new program was imported
      downloadedProgram = downloadedProgram.replace(
        'Comprehensive Sample Program',
        'Comprehensive Sample Program 2',
      )

      await adminProgramMigration.submitProgramJson(downloadedProgram)

      // Assert the new title and admin name are shown
      await expect(
        page.getByRole('heading', {
          name: 'Program name: Comprehensive Sample Program 2',
        }),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {
          name: 'Admin name: comprehensive-sample-program-2',
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

    await validateScreenshot(
      page.locator('main'),
      'import-page-with-data',
      /* fullPage= */ false,
    )

    await test.step('save the imported program', async () => {
      await adminProgramMigration.saveProgram()
      await adminPrograms.expectProgramExist(
        'Comprehensive Sample Program 2',
        'desc',
      )
    })
  })
})
