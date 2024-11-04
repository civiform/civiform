import {test, expect} from './support/civiform_fixtures'
import {
  AdminPrograms,
  AdminQuestions,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {Page} from 'playwright'

test.describe('End to end enumerator test', () => {
  const programName = 'Ete enumerator program'

  test.describe('Admin page', () => {
    test('Updates enumerator elements in preview', async ({
      page,
      adminQuestions,
    }) => {
      await test.step('Load enumerator creation page', async () => {
        await loginAsAdmin(page)

        await adminQuestions.gotoAdminQuestionsPage()

        await page.click('#create-question-button')
        await page.click('#create-enumerator-question')
        await waitForPageJsLoad(page)
      })

      await test.step('Click add button and verify we get entity row and delete button', async () => {
        await page.click('button:text("Add Sample repeated entity type")')
        await validateScreenshot(page, 'enumerator-field')
      })

      await test.step('Update text when configuring question and ensure preview values update', async () => {
        await page.fill('text=Repeated Entity Type', 'New entity type')
        await validateScreenshot(page, 'enumerator-type-set')
      })

      await test.step('Verify question preview has the default values.', async () => {
        await adminQuestions.expectCommonPreviewValues({
          questionText: 'Sample question text',
          questionHelpText: '',
        })
        await adminQuestions.expectEnumeratorPreviewValues({
          entityNameInputLabelText: 'New entity type name #1',
          addEntityButtonText: 'Add New entity type',
          deleteEntityButtonText: 'Remove New entity type #1',
        })
      })
    })

    test('Create nested enumerator and repeated questions as admin', async ({
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      await setupEnumeratorQuestion(
        page,
        adminQuestions,
        adminPrograms,
        /* shouldValidateScreenshot= */ true,
      )
    })
  })

  test.describe('Applicant flow with North star flag disabled', () => {
    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setupEnumeratorQuestion(
        page,
        adminQuestions,
        adminPrograms,
        /* shouldValidateScreenshot= */ false,
      )
    })

    test('has no accessibility violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerNameQuestion('Porky', 'Pig')
      await applicantQuestions.clickNext()

      // Check that we are on the enumerator page
      await expect(page.locator('.cf-question-enumerator')).toBeVisible()

      // Validate that enumerators are accessible
      await validateAccessibility(page)

      // Adding enumerator answers causes a clone of a hidden DOM element. This element
      // should have unique IDs. If not, it will cause accessibility violations.
      // See https://github.com/civiform/civiform/issues/3565.
      await applicantQuestions.addEnumeratorAnswer('Bugs')
      await applicantQuestions.addEnumeratorAnswer('Daffy')
      await validateAccessibility(page)

      // Correspondingly, removing an element happens without a page refresh. Remove an
      // element and add another to ensure that element IDs remain unique.
      await applicantQuestions.deleteEnumeratorEntityByIndex(1)
      await applicantQuestions.addEnumeratorAnswer('Porky')
      await validateAccessibility(page)
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerNameQuestion('Porky', 'Pig')
      await applicantQuestions.clickNext()

      await applicantQuestions.addEnumeratorAnswer('Bugs')

      await validateScreenshot(page, 'enumerator')
    })

    test('validate screenshot with errors', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerNameQuestion('Porky', 'Pig')
      await applicantQuestions.clickNext()

      // Click next without adding an enumerator
      await applicantQuestions.clickNext()
      await validateScreenshot(page, 'enumerator-errors')
    })

    test('Renders the correct indexes for labels and buttons', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      // Fill in name question
      await applicantQuestions.answerNameQuestion('Porky', 'Pig')
      await applicantQuestions.clickNext()

      // Put some things in the enumerator question, they should be numbered in order
      await applicantQuestions.addEnumeratorAnswer('Bugs')
      await applicantQuestions.addEnumeratorAnswer('Daffy')
      await applicantQuestions.addEnumeratorAnswer('Goofy')
      await validateScreenshot(page, 'enumerator-indexes-with-multiple-fileds')

      // Remove the middle entry, the remaining entries should re-index
      await applicantQuestions.deleteEnumeratorEntityByIndex(1)
      await validateScreenshot(page, 'enumerator-indexes-after-removing-field')
    })

    test('Applicant can fill in lots of blocks, and then go back and delete some repeated entities', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      // Fill in name question
      await applicantQuestions.answerNameQuestion('Porky', 'Pig')
      await applicantQuestions.clickNext()

      // Put in two things in the enumerator question
      await applicantQuestions.addEnumeratorAnswer('Bugs')
      await applicantQuestions.addEnumeratorAnswer('Daffy')
      await applicantQuestions.clickNext()

      // FIRST REPEATED ENTITY
      // Answer name
      await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
      await applicantQuestions.clickNext()

      // Put one thing in the nested enumerator for enum one
      await applicantQuestions.addEnumeratorAnswer('Cartoon Character')
      await applicantQuestions.clickNext()

      // Answer the nested repeated question
      await applicantQuestions.answerNumberQuestion('100')
      await applicantQuestions.clickNext()

      // SECOND REPEATED ENTITY
      // Answer name
      await applicantQuestions.answerNameQuestion('Daffy', 'Duck')
      await applicantQuestions.clickNext()

      // Put an empty answer in the nested enumerator for enum two.
      await applicantQuestions.addEnumeratorAnswer('')
      await applicantQuestions.clickNext()

      // Oops! Can't have blank lines.
      // Verify that the error message is visible.
      await expect(
        page.locator('.cf-applicant-question-errors:visible'),
      ).toContainText('Error: Please enter a value for each line.')
      // Put two things in the nested enumerator for enum two
      await applicantQuestions.deleteEnumeratorEntityByIndex(1)
      await applicantQuestions.addEnumeratorAnswer('Banker')
      await applicantQuestions.addEnumeratorAnswer('Banker')
      await applicantQuestions.clickNext()

      // Oops! Can't have duplicates.
      // Verify that the error message is visible.
      await expect(
        page.locator('.cf-applicant-question-errors:visible'),
      ).toContainText('Error: Please enter a unique value for each line.')

      // Remove one of the 'Banker' entries and add 'Painter'.
      // the value attribute of the inputs isn't set, so we're clicking the second one.
      await applicantQuestions.deleteEnumeratorEntityByIndex(2)
      await applicantQuestions.addEnumeratorAnswer('Painter')
      await applicantQuestions.clickNext()

      // Answer two nested repeated text questions
      await applicantQuestions.answerNumberQuestion('31')
      await applicantQuestions.clickNext()
      await applicantQuestions.answerNumberQuestion('12')
      await applicantQuestions.clickNext()

      // Make sure the enumerator answers are in the review page
      await expect(page.locator('#application-summary')).toContainText(
        'Porky Pig',
      )
      await expect(page.locator('#application-summary')).toContainText(
        'Bugs Bunny',
      )
      await expect(page.locator('#application-summary')).toContainText(
        'Cartoon Character',
      )
      await expect(page.locator('#application-summary')).toContainText('100')
      await expect(page.locator('#application-summary')).toContainText(
        'Daffy Duck',
      )
      await expect(page.locator('#application-summary')).toContainText('Banker')
      await expect(page.locator('#application-summary')).toContainText(
        'Painter',
      )
      await expect(page.locator('#application-summary')).toContainText('31')
      await expect(page.locator('#application-summary')).toContainText('12')

      // Go back to delete enumerator answers
      await page.click(
        '.cf-applicant-summary-row:has(div:has-text("Household members")) a:has-text("Edit")',
      )
      await waitForPageJsLoad(page)

      await applicantQuestions.deleteEnumeratorEntity('Bugs')
      // Submit the answers by clicking next, and then go to review page.
      await applicantQuestions.clickNext()

      // Make sure that the removed enumerator is not present in the review page
      await expect(page.locator('#application-summary')).toContainText(
        'Porky Pig',
      )
      await expect(page.locator('#application-summary')).not.toContainText(
        'Bugs Bunny',
      )
      await expect(page.locator('#application-summary')).not.toContainText(
        'Cartoon Character',
      )
      await expect(page.locator('#application-summary')).not.toContainText(
        '100',
      )

      // Go back and add an enumerator answer.
      await page.click(
        '.cf-applicant-summary-row:has(div:has-text("Household members")) a:has-text("Edit")',
      )
      await waitForPageJsLoad(page)
      await applicantQuestions.addEnumeratorAnswer('Tweety')
      await applicantQuestions.clickNext()
      await applicantQuestions.answerNameQuestion('Tweety', 'Bird')
      await applicantQuestions.clickNext()
      await applicantQuestions.clickReview()

      // Review page should contain Daffy Duck and newly added Tweety Bird.
      await expect(page.locator('#application-summary')).toContainText(
        'Porky Pig',
      )
      await expect(page.locator('#application-summary')).toContainText(
        'Tweety Bird',
      )
      await expect(page.locator('#application-summary')).toContainText(
        'Daffy Duck',
      )
      await expect(page.locator('#application-summary')).toContainText('Banker')
      await expect(page.locator('#application-summary')).toContainText(
        'Painter',
      )
      await expect(page.locator('#application-summary')).toContainText('31')
      await expect(page.locator('#application-summary')).toContainText('12')
      // // Review page should not contain deleted enumerator info for Bugs Bunny.
      await expect(page.locator('#application-summary')).not.toContainText(
        'Bugs Bunny',
      )
      await expect(page.locator('#application-summary')).not.toContainText(
        'Cartoon Character',
      )
      await expect(page.locator('#application-summary')).not.toContainText(
        '100',
      )

      await logout(page)
    })

    test('Enumerator add button is enabled/disabled correctly', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Set up application', async () => {
        await applicantQuestions.applyProgram(programName)

        await applicantQuestions.answerNameQuestion('Porky', 'Pig')
        await applicantQuestions.clickNext()
      })

      await test.step('Add button is disabled when the maximum number of entities is entered', async () => {
        await applicantQuestions.addEnumeratorAnswer('Bugs')
        await applicantQuestions.addEnumeratorAnswer('Daffy')
        await applicantQuestions.addEnumeratorAnswer('Donald')
        await applicantQuestions.addEnumeratorAnswer('Tweety')

        await expect(
          page.locator('#enumerator-field-add-button'),
        ).toHaveAttribute('disabled')
      })

      await test.step('Add button is still disabled after navigating away and back', async () => {
        await applicantQuestions.clickNext()
        await applicantQuestions.clickPrevious()

        await expect(
          page.locator('#enumerator-field-add-button'),
        ).toHaveAttribute('disabled')
      })

      await test.step('Add button is enabled with less than the maximum entities', async () => {
        await applicantQuestions.deleteEnumeratorEntity('Tweety')

        await expect(page.locator('#enumerator-field-add-button')).toBeEnabled()
      })

      await test.step('Add button is disabled if an entity is blank', async () => {
        await applicantQuestions.addEnumeratorAnswer('')

        await expect(
          page.locator('#enumerator-field-add-button'),
        ).toBeDisabled()
      })

      await test.step('Add button is re-enabled when the blank entity is removed', async () => {
        await applicantQuestions.deleteEnumeratorEntity('')

        await expect(page.locator('#enumerator-field-add-button')).toBeEnabled()
      })

      await test.step('Add button is still enabled after navigating away and back', async () => {
        await applicantQuestions.clickNext()
        await applicantQuestions.clickPrevious()

        await expect(page.locator('#enumerator-field-add-button')).toBeEnabled()
      })

      await test.step('Add button is disabled when an existing entity is blanked', async () => {
        await applicantQuestions.editEnumeratorAnswer('Bugs', '')

        await expect(
          page.locator('#enumerator-field-add-button'),
        ).toBeDisabled()
      })

      await test.step('Add button is still disabled after trying to save', async () => {
        await applicantQuestions.clickNext()

        // Error shows because of the empty entity
        await expect(
          page.locator('.cf-applicant-question-errors'),
        ).toBeVisible()

        await expect(
          page.locator('#enumerator-field-add-button'),
        ).toBeDisabled()
      })
    })

    test('Applicant can navigate to previous blocks', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      // Fill in name question
      await applicantQuestions.answerNameQuestion('Porky', 'Pig')
      await applicantQuestions.clickNext()

      // Put in two things in the enumerator question
      await applicantQuestions.addEnumeratorAnswer('Bugs')
      await applicantQuestions.addEnumeratorAnswer('Daffy')
      await applicantQuestions.clickNext()

      // REPEATED ENTITY
      // Answer name
      await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
      await applicantQuestions.clickNext()

      // Put one thing in the nested enumerator for enum one
      await applicantQuestions.addEnumeratorAnswer('Cartoon Character')
      await applicantQuestions.clickNext()

      // Answer the nested repeated question
      await applicantQuestions.answerNumberQuestion('100')
      await applicantQuestions.clickNext()

      // Check previous navigation works
      // Click previous and see number question
      await applicantQuestions.clickPrevious()
      await applicantQuestions.checkNumberQuestionValue('100')

      // Click previous and see enumerator question
      await applicantQuestions.clickPrevious()
      await applicantQuestions.checkEnumeratorAnswerValue(
        'Cartoon Character',
        1,
      )

      // Click previous and see name question
      await applicantQuestions.clickPrevious()
      await applicantQuestions.checkNameQuestionValue('Bugs', 'Bunny')

      // Click previous and see enumerator question
      await applicantQuestions.clickPrevious()
      await applicantQuestions.checkEnumeratorAnswerValue('Daffy', 2)
      await applicantQuestions.checkEnumeratorAnswerValue('Bugs', 1)

      // Click previous and see name question
      await applicantQuestions.clickPrevious()
      await applicantQuestions.checkNameQuestionValue('Porky', 'Pig')

      await logout(page)
    })

    test('Create new version of enumerator and update repeated questions and programs', async ({
      page,
    }) => {
      await loginAsAdmin(page)

      const adminQuestions = new AdminQuestions(page)
      const adminPrograms = new AdminPrograms(page)

      await test.step('Create new version of enumerator', async () => {
        await adminQuestions.createNewVersion('enumerator-ete-householdmembers')
      })

      await test.step('Verify repeated questions are updated', async () => {
        await adminQuestions.expectDraftQuestionExist(
          'enumerator-ete-repeated-name',
        )
        await adminQuestions.expectDraftQuestionExist(
          'enumerator-ete-repeated-jobs',
        )
        await adminQuestions.expectDraftQuestionExist(
          'enumerator-ete-repeated-jobs-income',
        )
      })

      await test.step('Publish program', async () => {
        await adminPrograms.publishProgram(programName)
      })

      await logout(page)
    })
  })

  test.describe(
    'Applicant flow with North star flag enabled',
    {tag: ['@northstar']},
    () => {
      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setupEnumeratorQuestion(
          page,
          adminQuestions,
          adminPrograms,
          /* shouldValidateScreenshot= */ false,
        )
        await enableFeatureFlag(page, 'north_star_applicant_ui')
      })

      test('validate screenshot', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.answerNameQuestion('Porky', 'Pig')
        await applicantQuestions.clickContinue()

        await applicantQuestions.addEnumeratorAnswer('Bugs')

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'enumerator-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'enumerator-errors-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })
      })

      test('has no accessibility violations', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.answerNameQuestion('Porky', 'Pig')
        await applicantQuestions.clickContinue()

        // Check that we are on the enumerator page
        await expect(page.locator('.cf-question-enumerator')).toBeVisible()

        // Validate that enumerators are accessible
        await validateAccessibility(page)

        // Adding enumerator answers causes a clone of a hidden DOM element. This element
        // should have unique IDs. If not, it will cause accessibility violations.
        // See https://github.com/civiform/civiform/issues/3565.
        await applicantQuestions.addEnumeratorAnswer('Bugs')
        await applicantQuestions.addEnumeratorAnswer('Daffy')
        await validateAccessibility(page)

        // Correspondingly, removing an element happens without a page refresh. Remove an
        // element and add another to ensure that element IDs remain unique.
        await applicantQuestions.deleteEnumeratorEntityByIndex(1)
        await applicantQuestions.addEnumeratorAnswer('Porky')
        await validateAccessibility(page)
      })

      test('Renders the correct indexes for labels and buttons', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        // Fill in name question
        await applicantQuestions.answerNameQuestion('Porky', 'Pig')
        await applicantQuestions.clickContinue()

        // Put some things in the enumerator question, they should be numbered in order
        await applicantQuestions.addEnumeratorAnswer('Bugs')
        await applicantQuestions.addEnumeratorAnswer('Daffy')
        await applicantQuestions.addEnumeratorAnswer('Goofy')
        await validateScreenshot(
          page,
          'enumerator-indexes-with-multiple-fields-northstar',
        )

        // Remove the middle entry, the remaining entries should re-index
        await applicantQuestions.deleteEnumeratorEntityByIndex(1)
        await validateScreenshot(
          page,
          'enumerator-indexes-after-removing-field-northstar',
        )
      })

      test('Applicant can fill in lots of blocks, and then go back and delete some repeated entities', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        // Fill in name question
        await applicantQuestions.answerNameQuestion('Porky', 'Pig')
        await applicantQuestions.clickContinue()

        // Put in two things in the enumerator question
        await applicantQuestions.addEnumeratorAnswer('Bugs')
        await applicantQuestions.addEnumeratorAnswer('Daffy')
        await applicantQuestions.clickContinue()

        // FIRST REPEATED ENTITY
        // Answer name
        await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
        await applicantQuestions.clickContinue()

        // Put one thing in the nested enumerator for enum one
        await applicantQuestions.addEnumeratorAnswer('Cartoon Character')
        await applicantQuestions.clickContinue()

        // Answer the nested repeated question
        await applicantQuestions.answerNumberQuestion('100')
        await applicantQuestions.clickContinue()

        // SECOND REPEATED ENTITY
        // Answer name
        await applicantQuestions.answerNameQuestion('Daffy', 'Duck')
        await applicantQuestions.clickContinue()

        // Put an empty answer in the nested enumerator for enum two.
        await applicantQuestions.addEnumeratorAnswer('')
        await applicantQuestions.clickContinue()

        // Oops! Can't have blank lines.
        // Verify that the error message is visible.
        await expect(
          page.locator('.cf-applicant-question-errors:visible'),
        ).toContainText('Error: Please enter a value for each line.')
        // Put two things in the nested enumerator for enum two
        await applicantQuestions.deleteEnumeratorEntityByIndex(1)
        await applicantQuestions.addEnumeratorAnswer('Banker')
        await applicantQuestions.addEnumeratorAnswer('Banker')
        await applicantQuestions.clickContinue()

        // Oops! Can't have duplicates.
        // Verify that the error message is visible.
        await expect(
          page.locator('.cf-applicant-question-errors:visible'),
        ).toContainText('Error: Please enter a unique value for each line.')

        // Remove one of the 'Banker' entries and add 'Painter'.
        // the value attribute of the inputs isn't set, so we're clicking the second one.
        await applicantQuestions.deleteEnumeratorEntityByIndex(2)
        await applicantQuestions.addEnumeratorAnswer('Painter')
        await applicantQuestions.clickContinue()

        // Answer two nested repeated text questions
        await applicantQuestions.answerNumberQuestion('31')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerNumberQuestion('12')
        await applicantQuestions.clickContinue()

        // Make sure the enumerator answers are in the review page
        await expect(page.locator('.application-summary')).toContainText(
          'Porky Pig',
        )
        await expect(page.locator('.application-summary')).toContainText(
          'Bugs Bunny',
        )
        await expect(page.locator('.application-summary')).toContainText(
          'Cartoon Character',
        )
        await expect(page.locator('.application-summary')).toContainText('100')
        await expect(page.locator('.application-summary')).toContainText(
          'Daffy Duck',
        )
        await expect(page.locator('.application-summary')).toContainText(
          'Banker',
        )
        await expect(page.locator('.application-summary')).toContainText(
          'Painter',
        )
        await expect(page.locator('.application-summary')).toContainText('31')
        await expect(page.locator('.application-summary')).toContainText('12')

        // Go back to delete enumerator answers
        await applicantQuestions.editQuestionFromReviewPage(
          'Household members',
          /* northStarEnabled= */ true,
        )
        await waitForPageJsLoad(page)

        await applicantQuestions.deleteEnumeratorEntity('Bugs')
        // Submit the answers by clicking next, and then go to review page.
        await applicantQuestions.clickContinue()

        // Make sure that the removed enumerator is not present in the review page
        await expect(page.locator('.application-summary')).toContainText(
          'Porky Pig',
        )
        await expect(page.locator('.application-summary')).not.toContainText(
          'Bugs Bunny',
        )
        await expect(page.locator('.application-summary')).not.toContainText(
          'Cartoon Character',
        )
        await expect(page.locator('.application-summary')).not.toContainText(
          '100',
        )

        // Go back and add an enumerator answer.
        await applicantQuestions.editQuestionFromReviewPage(
          'Household members',
          /* northStarEnabled= */ true,
        )
        await waitForPageJsLoad(page)
        await applicantQuestions.addEnumeratorAnswer('Tweety')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerNameQuestion('Tweety', 'Bird')
        await applicantQuestions.clickContinue()
        await applicantQuestions.clickReview(/* northStarEnabled= */ true)

        // Review page should contain Daffy Duck and newly added Tweety Bird.
        await expect(page.locator('.application-summary')).toContainText(
          'Porky Pig',
        )
        await expect(page.locator('.application-summary')).toContainText(
          'Tweety Bird',
        )
        await expect(page.locator('.application-summary')).toContainText(
          'Daffy Duck',
        )
        await expect(page.locator('.application-summary')).toContainText(
          'Banker',
        )
        await expect(page.locator('.application-summary')).toContainText(
          'Painter',
        )
        await expect(page.locator('.application-summary')).toContainText('31')
        await expect(page.locator('.application-summary')).toContainText('12')
        // // Review page should not contain deleted enumerator info for Bugs Bunny.
        await expect(page.locator('.application-summary')).not.toContainText(
          'Bugs Bunny',
        )
        await expect(page.locator('.application-summary')).not.toContainText(
          'Cartoon Character',
        )
        await expect(page.locator('.application-summary')).not.toContainText(
          '100',
        )

        await logout(page)
      })
    },
  )

  async function setupEnumeratorQuestion(
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
    shouldValidateScreenshot: boolean,
  ) {
    await loginAsAdmin(page)

    await test.step('Add questions to program', async () => {
      await adminQuestions.addNameQuestion({
        questionName: 'enumerator-ete-name',
      })
      await adminQuestions.addEnumeratorQuestion({
        questionName: 'enumerator-ete-householdmembers',
        description: 'desc',
        questionText: 'Household members',
        helpText: 'list household members',
        maxNum: 4,
      })
      await adminQuestions.addNameQuestion({
        questionName: 'enumerator-ete-repeated-name',
        description: 'desc',
        questionText: 'Name for $this',
        helpText: 'full name for $this',
        enumeratorName: 'enumerator-ete-householdmembers',
      })
      await adminQuestions.addEnumeratorQuestion({
        questionName: 'enumerator-ete-repeated-jobs',
        description: 'desc',
        questionText: 'Jobs for $this',
        helpText: "$this's jobs",
        enumeratorName: 'enumerator-ete-householdmembers',
      })
      await adminQuestions.addNumberQuestion({
        questionName: 'enumerator-ete-repeated-jobs-income',
        description: 'desc',
        questionText: "Income for $this.parent's job at $this",
        helpText: 'Monthly income at $this',
        enumeratorName: 'enumerator-ete-repeated-jobs',
      })
    })

    await test.step('Create program', async () => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(
        programName,
        'ete enumerator program description',
      )
    })

    await test.step('Verify non-repeated questions are available in question bank', async () => {
      await expect(page.locator('#question-bank-nonuniversal')).toContainText(
        'enumerator-ete-name',
      )
      await expect(page.locator('#question-bank-nonuniversal')).toContainText(
        'enumerator-ete-householdmembers',
      )
    })

    await test.step('Add an enumerator question. All options should go away.', async () => {
      await adminPrograms.addQuestionFromQuestionBank(
        'enumerator-ete-householdmembers',
      )
      await expect(page.locator('#question-bank-nonuniversal')).toHaveText('')
    })

    await test.step(
      'Remove the enumerator question and add a non-enumerator question, and the ' +
        'enumerator option should not be in the bank.',
      async () => {
        // Remove the enumerator question and add a non-enumerator question, and the enumerator option should not be in the bank.
        await page.click(
          '.cf-program-question:has-text("enumerator-ete-householdmembers") >> .cf-remove-question-button',
        )
        await adminPrograms.addQuestionFromQuestionBank('enumerator-ete-name')
        await expect(
          page.locator('#question-bank-nonuniversal'),
        ).not.toContainText('enumerator-ete-householdmembers')
      },
    )

    await test.step(
      'Create a new block with the first enumerator question, and then create a repeated block. ' +
        'The repeated questions should be the only options.',
      async () => {
        await page.click('#add-block-button')
        await adminPrograms.addQuestionFromQuestionBank(
          'enumerator-ete-householdmembers',
        )
        await page.click('#create-repeated-block-button')
        await expect(page.locator('#question-bank-nonuniversal')).toContainText(
          'enumerator-ete-repeated-name',
        )
        await expect(page.locator('#question-bank-nonuniversal')).toContainText(
          'enumerator-ete-repeated-jobs',
        )
      },
    )

    await test.step('Go back to the enumerator block, and with a repeated block, it cannot be deleted now. The enumerator question cannot be removed, either.', async () => {
      await page.click('p:text("Screen 2")')
      await expect(page.locator('#block-delete-modal-button')).toHaveAttribute(
        'disabled',
      )
      await expect(
        page.locator(
          '.cf-program-question:has-text("enumerator-ete-householdmembers") >> .cf-remove-question-button',
        ),
      ).toBeDisabled()
    })

    await test.step('Create the rest of the program.', async () => {
      // Create the rest of the program.
      // Add repeated name question
      await page.click('p:text("Screen 3")')
      await adminPrograms.addQuestionFromQuestionBank(
        'enumerator-ete-repeated-name',
      )

      // Create another repeated block and add the nested enumerator question
      await page.click('p:text("Screen 2")')
      await page.click('#create-repeated-block-button')
      await adminPrograms.addQuestionFromQuestionBank(
        'enumerator-ete-repeated-jobs',
      )

      // Create a nested repeated block and add the nested text question
      await page.click('#create-repeated-block-button')
    })

    await test.step('Maybe validate screenshot of fully created program', async () => {
      if (shouldValidateScreenshot) {
        await validateScreenshot(page, 'programindentation')
      }
      await adminPrograms.addQuestionFromQuestionBank(
        'enumerator-ete-repeated-jobs-income',
      )
    })

    await test.step('Publish!', async () => {
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })
  }
})
