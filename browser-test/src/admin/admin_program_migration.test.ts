import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  seedPrograms,
  validateScreenshot,
} from '../support'
import {readFileSync} from 'fs'

test.describe('program migration', () => {
  test('export a program', async ({
    page,
    adminPrograms,
    adminProgramMigration,
    adminQuestions,
  }) => {
    const programName = 'program-2'
    const dateQuestionText = 'What is your birthday?'
    const emailQuestionText = 'What is your email?'
    const phoneQuestionText = 'What is your phone number?'
    const block1Description = 'Birthday block'
    const block2Description = 'Key information block'

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

      await adminPrograms.addProgram('program-1')

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, block1Description, [
        'date-q',
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
      await validateScreenshot(page, 'export-page')
    })

    await test.step('export program 2', async () => {
      await adminProgramMigration.selectProgramToExport('program-2')
      const downloadedProgram = await adminProgramMigration.downloadProgram()
      expect(downloadedProgram).toContain(programName)
      expect(downloadedProgram).toContain(block1Description)
      expect(downloadedProgram).toContain(block2Description)
      // TODO(#7087): Once we export the questions, assert that all the questions
      // are in the downloaded program.
    })
  })

  test('import a program', async ({page, adminProgramMigration}) => {
    await test.step('load import page', async () => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'program_migration_enabled')
      await adminProgramMigration.goToImportPage()
      await validateScreenshot(page, 'import-page-no-data')
    })

    await test.step('import a program', async () => {
      const sampleJson = readFileSync(
        'src/assets/import-program-sample.json',
        'utf8',
      )
      await adminProgramMigration.submitProgramJson(sampleJson)

      await adminProgramMigration.expectProgramImported('Import Sample Program')
      // The import page currently shows question IDs, so this screenshot needs
      // to be based on data that comes from a pre-created JSON file instead of
      // a runtime-downloaded JSON file, as the IDs could change at runtime.
      // Eventually, we likely won't show the question IDs and could take a
      // screenshot based on runtime-downloaded JSON.
      await validateScreenshot(page, 'import-page-with-data')
    })
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
      await validateScreenshot(page, 'import-page-with-error-parse')
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
      downloadedProgram = await adminProgramMigration.downloadProgram()
      expect(downloadedProgram).toContain('comprehensive-sample-program')
    })

    await test.step('import comprehensive program', async () => {
      await adminProgramMigration.goToImportPage()
      await adminProgramMigration.submitProgramJson(downloadedProgram)

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
      await expect(page.getByText('Question ID:')).toHaveCount(17)
      // TODO(#7087): Once we export the questions, assert that more question information is shown.
    })
  })
})
