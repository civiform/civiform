import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {Locator, Page} from '@playwright/test'
import {waitForHtmxReady} from './support/wait'

test.describe('End to end enumerator test with enumerators feature flag on', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'enumerator_improvements_enabled')
  })

  test.describe('Admin', () => {
    test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram('Enumerator test program')

      await test.step('Create questions', async () => {
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
        await adminQuestions.addNumberQuestion({
          questionName: 'income-non-repeated-question',
          description: 'desc',
          questionText: 'Your monthly income',
          helpText: 'Monthly income',
        })
      })

      await test.step('Go to the program block edit page', async () => {
        await adminPrograms.gotoEditDraftProgramPage('Enumerator test program')
      })
    })

    test('can add an enumerator block and a repeated block to a program at once from the program block edit page', async ({
      page,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      let initialBlockCount: number
      const screenLinks = page.getByRole('link', {name: /Screen /})
      let enumeratorBlockLink: Locator

      await test.step('Record how many blocks are present in the block order panel', async () => {
        initialBlockCount = await screenLinks.count()
      })

      await test.step('Click "Add screen" button and check that the dropdown appears', async () => {
        await page.getByRole('button', {name: 'Add screen'}).first().click()
        const addScreenDropdown = page
          .getByRole('list')
          .filter({has: page.getByRole('button', {name: 'Add repeated set'})})
        await expect(addScreenDropdown).toBeVisible()
      })

      await test.step('Click "Add repeated set" button', async () => {
        await page.getByRole('button', {name: 'Add repeated set'}).click()
      })

      await test.step('Validate that two new blocks appear in the block order panel', async () => {
        await expect(screenLinks).toHaveCount(initialBlockCount + 2)
      })

      await test.step('Validate that the current block is the newly-created repeated block', async () => {
        await expectCurrentBlockTitle(
          /* isRepeatedBlock= */ true,
          blockPanel,
          /* expectedScreenNumber= */ 3,
          /* repeatedFrom= */ 2,
        )
      })

      await test.step('Validate that the enumerator block says repeated set', async () => {
        enumeratorBlockLink = page
          .getByRole('link', {name: /Screen /})
          .nth(initialBlockCount) // zero-indexed

        await expect(
          enumeratorBlockLink.getByText('Repeated set'),
        ).toBeVisible()
      })

      await test.step('Click on the enumerator block in the block order panel', async () => {
        await enumeratorBlockLink.click()
      })

      await test.step('Validate that "Repeated set creation method" radio buttons are visible', async () => {
        await expect(
          blockPanel.getByRole('group', {name: 'Repeated set creation method'}),
        ).toBeVisible()
        await expect(
          blockPanel.getByRole('radio', {name: 'Create new'}),
        ).toBeVisible()
        await expect(
          blockPanel.getByRole('radio', {name: 'Choose existing'}),
        ).toBeVisible()
      })

      await test.step('Validate the "Create new repeated set" text is visible', async () => {
        await expect(
          blockPanel.getByText('Create new repeated set'),
        ).toBeVisible()
      })

      await test.step('Validate that "Create repeated set" button is visible', async () => {
        await expect(
          blockPanel.getByRole('button', {name: 'Create repeated set'}),
        ).toBeVisible()
      })

      await test.step('Validate that questions section is not visible', async () => {
        await expect(blockPanel.locator('#questions-section')).toBeHidden()
      })

      await test.step('Validate that "Add question" button is not visible', async () => {
        await expect(
          blockPanel.getByRole('button', {name: 'Add question'}),
        ).toBeHidden()
      })

      await test.step('Take a screenshot of the block panel', async () => {
        await validateScreenshot(blockPanel, 'enumerator-block-panel', {
          fullPage: false,
        })
      })
    })

    test('can create a new enumerator question from the Program Block Edit page and add that question to the block', async ({
      page,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')

      await test.step('Add a new repeated set and select the parent block', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
      })

      await fillOutEnumeratorQuestionFormCorrectly(page)

      const enumeratorQuestionCard = blockPanel
        .getByTestId('question-div')
        .getByText('List the names of your pets.')

      await test.step('Validate that focus is sent to the repeated set question section heading', async () => {
        await expect(
          blockPanel.getByText('Repeated set question'),
        ).toBeFocused()
      })

      await test.step('Validate that the new question card is now visible on the enumerator block', async () => {
        await expect(enumeratorQuestionCard).toBeVisible()
      })

      await test.step('Navigate to another block, return and make sure the enumerator question is still visible', async () => {
        await navigateToRepeatedScreen(page, 3, 2)
        await page.getByRole('link', {name: 'Screen 2'}).click()
        await expectCurrentBlockTitle(
          /* isRepeatedBlock= */ false,
          blockPanel,
          /* expectedScreenNumber= */ 2,
        )
        await expect(enumeratorQuestionCard).toBeVisible()
      })

      await test.step('Check that the new question is now on the question list page', async () => {
        await page.getByRole('link', {name: 'Questions'}).click()
        await expect(
          page.getByRole('heading', {name: 'All questions'}),
        ).toBeVisible()
        await expect(page.getByText('Admin ID: pets enumerator')).toBeVisible()
      })
    })

    test('can add an existing enumerator question to an enumerator block', async ({
      page,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      const questionBankSidebar = page.getByRole('form', {
        name: 'Add a question',
      })

      await test.step('Add a new repeated set and select the parent block', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
      })

      await test.step('Click the "Choose existing" radio button', async () => {
        // Unfortunately, we have to click the label to select the radio button, because
        // USWDS places the radio button itself outside the viewport.
        await blockPanel.getByTestId('choose-existing-radio-label').click()
      })

      await test.step('Click the "Add question" button', async () => {
        await blockPanel.getByRole('button', {name: 'Add question'}).click()
      })

      await test.step('Validate that the question bank sidebar only shows enumerator questions', async () => {
        await expect(
          questionBankSidebar.getByText('enumerator-ete-householdmembers'),
        ).toBeVisible()
        await expect(
          questionBankSidebar.getByText('enumerator-ete-repeated-name'),
        ).toBeHidden()
      })

      await test.step('Validate that the question bank sidebar does not show the "Create new question" button', async () => {
        await expect(
          questionBankSidebar.getByText(
            "Not finding a question you're looking for in this list?",
          ),
        ).toBeHidden()
        await expect(
          questionBankSidebar.getByRole('button', {
            name: 'Create new question',
          }),
        ).toBeHidden()
      })

      await test.step('Add a question to the block and validate question card is visible', async () => {
        await questionBankSidebar.getByRole('button', {name: 'Add'}).click()

        const enumeratorQuestionCard = blockPanel.getByTestId(
          'question-admin-name-enumerator-ete-householdmembers',
        )

        await expect(enumeratorQuestionCard).toBeVisible()

        await expect(questionBankSidebar).toBeHidden()
      })

      await test.step('Validate that focus is sent to the repeated set question section heading', async () => {
        await expect(
          blockPanel.getByText('Repeated set question'),
        ).toBeFocused()
      })
    })

    test('auto-fills and preserves editable repeated set suggestions', async ({
      page,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      const listedEntityInput = blockPanel.getByRole('textbox', {
        name: 'Listed entity',
      })
      const adminIdInput = blockPanel.getByRole('textbox', {
        name: 'Repeated set admin ID',
      })
      const questionTextInput = blockPanel.getByRole('textbox', {
        name: 'Question text',
      })

      await test.step('Add a new repeated set and select its block', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
      })

      await test.step('Auto-fill admin id and question text from listed entity', async () => {
        // Adding extra spaces to test normalization of the listed entity input.
        await listedEntityInput.fill('household     member')

        await expect(adminIdInput).toHaveValue('household member repeated set')
        await expect(questionTextInput).toHaveValue(
          'Please add each household member.',
        )
      })

      await test.step('Preserve manual edits for suggested fields', async () => {
        await adminIdInput.fill('custom repeated set id')

        await questionTextInput.fill('Custom repeated set prompt')

        await listedEntityInput.fill('income source')

        await expect(adminIdInput).toHaveValue('custom repeated set id')
        await expect(questionTextInput).toHaveValue(
          'Custom repeated set prompt',
        )
      })

      await test.step('Resume auto-fill after clearing manual field', async () => {
        await questionTextInput.fill('')

        await listedEntityInput.fill('household item')

        await expect(questionTextInput).toHaveValue(
          'Please add each household item.',
        )
      })
    })

    test('error validation prevents user from submitting an invalid enumerator question form', async ({
      page,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')

      await test.step('Add a new repeated set and select the parent block', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
      })

      await test.step('Submit the new enumerator question form without filling out all the required fields', async () => {
        await blockPanel
          .getByRole('textbox', {name: 'Listed entity'})
          .fill('Pets')
        await blockPanel.getByRole('textbox', {name: 'Question text'}).fill('')
        await blockPanel
          .getByRole('textbox', {name: 'Repeated set admin ID'})
          .fill('')

        await blockPanel
          .getByRole('button', {name: 'Create repeated set'})
          .click()
        await waitForHtmxReady(page)
      })

      await test.step('Validate that the form is still visible and previous input is retained', async () => {
        await expect(
          blockPanel.locator('#new-enumerator-question-form'),
        ).toBeVisible()
        await expect(
          blockPanel.getByRole('textbox', {name: 'Listed entity'}),
        ).toHaveValue('Pets')
      })

      await test.step('Validate that an error alert is shown with the correct error messages', async () => {
        const errorAlert = blockPanel.getByRole('alert').filter({
          hasText:
            'Error: Question text cannot be blank. Administrative identifier cannot be blank.',
        })
        await expect(errorAlert).toBeVisible()
      })

      await fillOutEnumeratorQuestionFormCorrectly(page)

      await test.step('Validate that the new question card is now visible on the enumerator block', async () => {
        const enumeratorQuestionCard = blockPanel
          .getByTestId('question-div')
          .getByText('List the names of your pets.')
        await expect(enumeratorQuestionCard).toBeVisible()
      })

      await test.step('Add a new repeated set', async () => {
        await addRepeatedSetBlock(page)
      })

      await test.step('Select the repeated set block from the block order panel', async () => {
        await page.getByRole('link', {name: 'Screen 4'}).click()
      })

      await test.step('Submit the new enumerator question form with a duplicate admin ID', async () => {
        await fillOutEnumeratorQuestionFormCorrectly(page, {
          adminId: 'pets enumerator',
        })
      })

      await test.step('Verify that an error alert is shown with the duplicate admin ID message', async () => {
        const errorAlert = blockPanel.getByRole('alert').filter({
          hasText:
            "Administrative identifier 'pets enumerator' generates " +
            "JSON path 'pets_enumerator' which would conflict with " +
            "the existing question with admin ID 'pets enumerator'.",
        })
        await expect(errorAlert).toBeVisible()
      })

      await test.step('Verify that focus is on the first input field of the enumerator form', async () => {
        await expect(
          blockPanel.getByRole('textbox', {name: 'Listed entity'}),
        ).toBeFocused()
      })
    })

    test('can use the "Add repeated screen" button to add repeated screens', async ({
      page,
      adminPrograms,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      const addRepeatedScreenButton = blockPanel.getByRole('button', {
        name: 'Add repeated screen',
      })

      await test.step('Add a new repeated set', async () => {
        await addRepeatedSetBlock(page)
      })

      await test.step('Verify that the "Add repeated screen" button is not present on the repeated screen', async () => {
        await expect(addRepeatedScreenButton).toBeHidden()
      })

      await test.step('Select the repeated set block from the block order panel', async () => {
        await page.getByRole('link', {name: 'Screen 2'}).click()
      })

      await test.step('Verify that the "Add repeated screen" button is not present on the enumerator screen', async () => {
        await expect(addRepeatedScreenButton).toBeHidden()
      })

      await fillOutEnumeratorQuestionFormCorrectly(page)

      await test.step('Verify that the "Add repeated screen" button is now present and click the button', async () => {
        await addRepeatedScreenButton.click()
      })

      await test.step('Go to the program block edit page', async () => {
        await adminPrograms.gotoEditDraftProgramPage('Enumerator test program')
      })

      await test.step('Verify that we can add a repeated screen from another repeated screen', async () => {
        await navigateToRepeatedScreen(page, 4, 2)
        await addRepeatedScreenButton.click()
        await expectCurrentBlockTitle(
          /* isRepeatedBlock= */ true,
          blockPanel,
          /* expectedScreenNumber= */ 5,
          /* repeatedFrom= */ 2,
        )
      })
    })

    test('"Add nested repeated set" button appears only on parent enumerator and direct repeated screens', async ({
      page,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      const addNestedRepeatedSetButton = blockPanel.getByRole('button', {
        name: 'Add nested repeated set',
      })

      await test.step('Add a new repeated set and verify nested button is hidden before parent enumerator question is saved', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
        await expect(addNestedRepeatedSetButton).toBeHidden()
      })

      await test.step('Save parent enumerator question and verify nested button appears on parent enumerator', async () => {
        await fillOutEnumeratorQuestionFormCorrectly(page)
        await expect(addNestedRepeatedSetButton).toBeVisible()
      })

      await test.step('Add a repeated screen and verify nested button appears on direct repeated screen', async () => {
        await blockPanel
          .getByRole('button', {name: 'Add repeated screen'})
          .click()
        await navigateToRepeatedScreen(page, 4, 2)
        await expect(addNestedRepeatedSetButton).toBeVisible()
      })

      await test.step('Create nested repeated set and verify nested button is hidden on nested blocks', async () => {
        await addNestedRepeatedSetButton.click()

        await navigateToRepeatedScreen(page, 5, 2)
        await expect(addNestedRepeatedSetButton).toBeHidden()

        await fillOutEnumeratorQuestionFormCorrectly(page, {
          listedEntity: 'Jobs',
          questionText: 'List jobs for $this',
          adminId: 'jobs enumerator',
        })

        await navigateToRepeatedScreen(page, 6, 5, {
          childLabel: '[child label]',
        })
        await expect(addNestedRepeatedSetButton).toBeHidden()
      })
    })

    test('enumerator question cannot be removed while a repeated screen exists, and enumerator block cannot be deleted once that repeated screen has a question', async ({
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      const deleteScreenButton = blockPanel.getByRole('button', {
        name: 'Delete screen',
      })
      const enumeratorQuestionCard = blockPanel.getByTestId(
        'question-admin-name-pets enumerator',
      )
      const removeQuestionButton = enumeratorQuestionCard.getByRole('button', {
        name: 'Delete',
      })

      await test.step('Add a new repeated set and save the enumerator question on the parent block', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
        await fillOutEnumeratorQuestionFormCorrectly(page)
      })

      await test.step('Before the repeated screen has a question: delete-screen is enabled but remove-question is disabled', async () => {
        await expect(deleteScreenButton).toBeEnabled()
        await expect(removeQuestionButton).toBeDisabled()
      })

      await test.step('Create a repeated text question for the pets enumerator and add it to the repeated screen', async () => {
        await adminQuestions.addTextQuestion({
          questionName: 'pets-repeated-name',
          questionText: 'Name for $this',
          enumeratorName: 'pets enumerator',
        })
        await adminPrograms.gotoEditDraftProgramPage('Enumerator test program')
        await navigateToRepeatedScreen(page, 3, 2)
        await adminPrograms.addQuestionFromQuestionBank('pets-repeated-name')
      })

      await test.step('Navigate back to the parent enumerator screen', async () => {
        await page.getByRole('link', {name: 'Screen 2'}).click()
      })

      await test.step('After the repeated screen has a question: both delete-screen and remove-question are disabled', async () => {
        await expect(deleteScreenButton).toBeDisabled()
        await expect(removeQuestionButton).toBeDisabled()
      })
    })

    test('repeated screen add-question button is disabled until enumerator question is saved', async ({
      page,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      const questionsSection = blockPanel.locator('#questions-section')
      const addQuestionButton = blockPanel.getByRole('button', {
        name: 'Add question',
      })
      const repeatedSetAlert = blockPanel.getByRole('alert').filter({
        hasText:
          'A repeated set question must first be added before repeated questions can be. Please navigate to the parent screen to add a repeated set question.',
      })

      await test.step('Add a new repeated set and verify repeated screen is selected', async () => {
        await addRepeatedSetBlock(page)
        await expectCurrentBlockTitle(
          /* isRepeatedBlock= */ true,
          blockPanel,
          /* expectedScreenNumber= */ 3,
          /* repeatedFrom= */ 2,
        )
      })

      await test.step('Verify repeated questions section content before enumerator question is saved', async () => {
        await expect(
          questionsSection.getByText('Repeated questions', {exact: true}),
        ).toBeVisible()
        await expect(
          questionsSection.getByText(
            'Add the questions you would like to be asked about each object or individual listed by the applicant.',
          ),
        ).toBeVisible()

        await validateScreenshot(
          questionsSection,
          'repeated-questions-section-before-save',
          {
            fullPage: false,
          },
        )
      })

      await test.step('Verify add-question is disabled and alert is visible before enumerator question is saved', async () => {
        await expect(addQuestionButton).toBeDisabled()
        await expect(repeatedSetAlert).toBeVisible()
      })

      await test.step('Save an enumerator question on the parent repeated set screen', async () => {
        await page.getByRole('link', {name: 'Screen 2'}).click()
        await fillOutEnumeratorQuestionFormCorrectly(page)
      })

      await test.step('Return to repeated screen and verify Add question is enabled and alert is hidden', async () => {
        await navigateToRepeatedScreen(page, 3, 2)
        await expect(addQuestionButton).toBeEnabled()
        await expect(repeatedSetAlert).toBeHidden()
      })
    })

    test('can add repeated questions to repeated screens', async ({
      page,
      adminPrograms,
      adminQuestions,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      const addRepeatedScreenButton = blockPanel.getByRole('button', {
        name: 'Add repeated screen',
      })

      await test.step('Add a new repeated set and select the parent block', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
      })

      await fillOutEnumeratorQuestionFormCorrectly(page)

      await test.step('Add a repeated question associated with this enumerator', async () => {
        await adminQuestions.addTextQuestion({
          questionName: 'enumerator-pets-repeated-colors',
          description: 'desc',
          questionText: "What is $this's favorite color?",
          helpText: 'Favorite color for $this',
          enumeratorName: 'pets enumerator',
        })

        await adminPrograms.gotoEditDraftProgramPage('Enumerator test program')
      })

      await test.step('Verify that the "Add repeated screen" button is now present and click the button', async () => {
        await expect(addRepeatedScreenButton).toBeVisible()
        await addRepeatedScreenButton.click()
      })

      await test.step('Click on the new repeated screen in the block order panel', async () => {
        await navigateToRepeatedScreen(page, 4, 2)
      })

      await test.step('Verify that the question bank has all non-repeated questions', async () => {
        await page.getByRole('button', {name: 'Add question'}).click()
        await expect(
          page.getByText('Admin ID: income-non-repeated-question'),
        ).toBeVisible()
      })

      await test.step('Verify that the repeated question associated with this enumerator is in the previously-used section', async () => {
        const previouslyUsedSection = page.locator(
          '#question-bank-previously-used',
        )

        await expect(
          page.getByRole('heading', {
            name: 'Previously used for this repeated set',
          }),
        ).toBeVisible()
        await expect(
          page.getByText(
            'Questions that are associated with a different repeated set are not available to be added.',
          ),
        ).toBeVisible()
        await expect(
          previouslyUsedSection.getByText(
            'Admin ID: enumerator-pets-repeated-colors',
          ),
        ).toBeVisible()
        await validateScreenshot(
          previouslyUsedSection,
          'question-bank-previously-used-section',
          {
            fullPage: false,
          },
        )
        await expect(
          page
            .locator('#question-bank-nonuniversal')
            .getByText('Admin ID: enumerator-pets-repeated-colors'),
        ).toBeHidden()
      })

      await test.step('Verify that the question bank does not have repeated questions that are associated with other enumerators', async () => {
        await expect(
          page.getByText('Admin ID: enumerator-ete-repeated-name'),
        ).toBeHidden()
        await adminPrograms.closeQuestionBank()
      })

      await test.step('Add the previously-used repeated question and verify the previously-used section no longer appears', async () => {
        await adminPrograms.addQuestionFromQuestionBank(
          'enumerator-pets-repeated-colors',
        )

        await page.getByRole('button', {name: 'Add question'}).click()
        await expect(
          page.locator('#question-bank-previously-used'),
        ).toBeHidden()
        await page.getByRole('button', {name: 'Close'}).click()
      })

      await test.step('Verify that creating a repeated question pre-selects the enumerator question.', async () => {
        await test.step('Add a new text question to the screen', async () => {
          await page.getByRole('button', {name: 'Add question'}).click()
          await page.getByRole('button', {name: 'Create new question'}).click()
          await page.getByRole('link', {name: 'Text', exact: true}).click()
        })

        await expect(page.getByLabel('Question enumerator')).toHaveAttribute(
          'readonly',
          'readonly',
        )
        await expect(
          page.getByLabel('Question enumerator').locator('option[selected]'),
        ).toHaveText('pets enumerator')
      })

      await test.step('Verify that adding a non-repeated question creates a copy that is associated with the enumerator', async () => {
        await test.step('Go to the block edit page', async () => {
          await adminPrograms.gotoEditDraftProgramPage(
            'Enumerator test program',
          )
          await navigateToRepeatedScreen(page, 4, 2)
        })

        await test.step('Add a non-repeated question to the repeated screen', async () => {
          await adminPrograms.addQuestionFromQuestionBank(
            'income-non-repeated-question',
          )
        })

        await test.step('Verify that a copy of the question is added to the screen', async () => {
          await navigateToRepeatedScreen(page, 4, 2)
          await expect(
            page.getByText('Admin ID: income-non-repeated-question -_- a'),
          ).toBeVisible()
        })
      })
    })

    test('Enumerator block name edit retains prefix', async ({
      page,
      adminPrograms,
    }) => {
      await test.step('Go to the program block edit page', async () => {
        await adminPrograms.gotoEditDraftProgramPage('Enumerator test program')
      })

      await test.step('Add a new repeated set', async () => {
        await addRepeatedSetBlock(page)
        await waitForPageJsLoad(page)
      })

      await navigateToRepeatedScreen(page, 3, 2)

      const modalPrefix = page.getByTestId('name-prefix')

      await test.step('check for correct enumerator description and uneditable prefix in screen editing modal', async () => {
        await page
          .getByRole('button', {name: 'Edit screen name and description'})
          .click()
        const modalDescription = page.getByTestId(
          'repeated-set-prefix-description',
        )
        await expect(modalDescription).toBeVisible()
        await expect(modalPrefix).toBeVisible()
      })

      await test.step('edit screen name, exit, and ensure prefix is still the same', async () => {
        await expect(modalPrefix).toHaveText('[parent label] -')

        await page.getByTestId('block-name-input').fill('name')

        await page.getByTestId('save-button').click()

        await page
          .getByRole('button', {name: 'Edit screen name and description'})
          .click()

        const currentModalPrefix = page.getByTestId('name-prefix')
        await expect(currentModalPrefix).toHaveText('[parent label] -')
      })
    })

    test('Radio button swaps repeated set creation method', async ({
      page,
      adminPrograms,
    }) => {
      await test.step('Go to the program block edit page', async () => {
        await adminPrograms.gotoEditDraftProgramPage('Enumerator test program')
      })

      await test.step('Add a new repeated set and select the parent block', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
      })

      await test.step('Check that Create New is preselected and create new partial view is visible', async () => {
        const createNewButton = page.getByLabel('Create new')

        const newEnumeratorQuestionFormButton = page.getByRole('button', {
          name: 'Create repeated set',
        })
        await expect(createNewButton).toBeChecked()
        await expect(newEnumeratorQuestionFormButton).toBeVisible()
      })

      await test.step('swap to choose existing and check existing partial view is visible', async () => {
        const chooseExistingButton = page.getByRole('radio', {
          name: 'Choose existing',
        })

        await chooseExistingButton.scrollIntoViewIfNeeded()
        await expect(chooseExistingButton).toBeVisible()

        // Uswds styling makes the label the clickable portion, trying to check the input will not work.
        const chooseExistingLabel = page.getByTestId(
          'choose-existing-radio-label',
        )
        await chooseExistingLabel.check()

        const addQuestionButton = page.getByRole('button', {
          name: 'Add question',
        })
        await expect(chooseExistingButton).toBeChecked()
        await expect(addQuestionButton).toBeVisible()
      })
    })

    test('disables enumerator dropdown when creating question from non-repeating screen', async ({
      page,
    }) => {
      await test.step('Click on the first (non-repeating) screen', async () => {
        await page.getByRole('link', {name: 'Screen 1'}).click()
      })

      await test.step('Add a new text question to the screen', async () => {
        await page.getByRole('button', {name: 'Add a question'}).click()
        await page.getByRole('button', {name: 'Create new question'}).click()
        await page.getByRole('link', {name: 'Text', exact: true}).click()
      })

      await test.step('Verify that the "Question enumerator" dropdown is read only', async () => {
        await expect(page.getByLabel('Question enumerator')).toHaveAttribute(
          'readonly',
          'readonly',
        )
      })
    })

    test('creating a new version of an active enumerator cascades linked repeated questions to draft', async ({
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      const blockPanel = page.getByTestId('block-panel-edit')
      const questionBankSidebar = page.getByRole('form', {
        name: 'Add a question',
      })

      await test.step('Add the existing enumerator question to a new repeated set', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
        await blockPanel.getByTestId('choose-existing-radio-label').click()
        await blockPanel.getByRole('button', {name: 'Add question'}).click()
        await questionBankSidebar.getByRole('button', {name: 'Add'}).click()
      })

      await test.step('Add the existing repeated name question to the repeated screen', async () => {
        await navigateToRepeatedScreen(page, 3, 2)
        await adminPrograms.addQuestionFromQuestionBank(
          'enumerator-ete-repeated-name',
        )
      })

      await test.step('Publish the program so both questions become active', async () => {
        await adminPrograms.publishProgram('Enumerator test program')
        await adminQuestions.expectActiveQuestionExist(
          'enumerator-ete-householdmembers',
        )
        await adminQuestions.expectActiveQuestionExist(
          'enumerator-ete-repeated-name',
        )
      })

      await test.step('Create a new version of the enumerator question', async () => {
        await adminQuestions.createNewVersion('enumerator-ete-householdmembers')
      })

      await test.step('Linked repeated question cascades to draft', async () => {
        await adminQuestions.expectDraftQuestionExist(
          'enumerator-ete-repeated-name',
        )
      })

      await test.step('Unlinked question remains active', async () => {
        await adminQuestions.expectActiveQuestionExist(
          'income-non-repeated-question',
        )
      })
    })
  })

  test.describe('Applicant', () => {
    const programName = 'Enumerator test program'
    const repeatedQuestionName = 'enumerator-ete-repeated-name'
    const nestedRepeatedQuestionName = 'enumerator-ete-repeated-jobs-income'

    test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoEditDraftProgramPage(programName)

      await test.step('Add a new repeated set and select the parent block', async () => {
        await addRepeatedSetBlock(page, {selectParent: true})
      })

      await fillOutEnumeratorQuestionFormCorrectly(page, {maxEntities: 4})

      await adminQuestions.addNameQuestion({
        questionName: repeatedQuestionName,
        description: 'desc',
        questionText: 'Name for $this',
        helpText: 'full name for $this',
        enumeratorName: 'pets enumerator',
      })

      await adminPrograms.gotoEditDraftProgramPage(programName)

      await navigateToRepeatedScreen(page, 3, 2)

      await test.step('Add repeated name question to the repeated screen', async () => {
        await adminPrograms.addQuestionFromQuestionBank(repeatedQuestionName)
      })

      await test.step('Create nested repeated set from repeated screen', async () => {
        const blockPanel = page.getByTestId('block-panel-edit')
        await blockPanel
          .getByRole('button', {name: 'Add nested repeated set'})
          .click()

        await navigateToRepeatedScreen(page, 4, 2)
        await fillOutEnumeratorQuestionFormCorrectly(page, {
          listedEntity: 'Jobs',
          questionText: 'List jobs for $this',
          adminId: 'jobs enumerator',
        })
      })

      await test.step('Add nested repeated number question (income) under jobs', async () => {
        await adminQuestions.addNumberQuestion({
          questionName: nestedRepeatedQuestionName,
          description: 'desc',
          questionText: "Income for $this.parent's job at $this",
          helpText: 'Monthly income at $this',
          enumeratorName: 'jobs enumerator',
        })

        await adminPrograms.gotoEditDraftProgramPage(programName)
        await navigateToRepeatedScreen(page, 5, 4, {
          childLabel: '[child label]',
        })
        await adminPrograms.addQuestionFromQuestionBank(
          nestedRepeatedQuestionName,
        )
      })

      await test.step('Publish the program', async () => {
        await adminPrograms.publishProgram(programName)
      })

      await logout(page)
    })

    test('sees repeated entity and nested repeated entity names in the screen name', async ({
      applicantQuestions,
      page,
    }) => {
      await test.step('Apply to the program', async () => {
        await applicantQuestions.applyProgram(programName)
      })

      await test.step('Enter a repeated entity name on the enumerator screen', async () => {
        await addRepeatedEntity(page, 'Pets', 'Bugs')
      })

      await test.step('Answer repeated and nested repeated questions', async () => {
        await applicantQuestions.clickContinue()

        await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
        await applicantQuestions.clickContinue()

        await addRepeatedEntity(page, 'Jobs', 'Mechanic')
        await applicantQuestions.clickContinue()

        await applicantQuestions.answerNumberQuestion('100')
        await page.getByRole('button', {name: 'Review and submit'}).click()
      })

      await test.step('Go to review screen and check repeated entity names in the screen names', async () => {
        await expect(
          page.getByText(`Bugs - Screen 3 (repeated from 2)`),
        ).toBeVisible()
        await expect(
          page.getByText(`Bugs - Mechanic - Screen 5 (repeated from 4)`),
        ).toBeVisible()
      })
    })

    test('still supports $this placeholder in repeated question text', async ({
      applicantQuestions,
      page,
    }) => {
      await test.step('Apply to the program and add a repeated entity', async () => {
        await applicantQuestions.applyProgram(programName)
        await addRepeatedEntity(page, 'Pets', 'Bugs')
      })

      await test.step('Continue to repeated question and verify $this is replaced', async () => {
        await applicantQuestions.clickContinue()
        await applicantQuestions.validateQuestionIsOnPage('Name for Bugs')
      })
    })

    test('does not throw errors when repeated question text omits $this', async ({
      adminPrograms,
      adminQuestions,
      applicantQuestions,
      page,
    }) => {
      await test.step('Update repeated question text to omit $this and publish', async () => {
        await loginAsAdmin(page)
        await adminQuestions.gotoQuestionEditPage(repeatedQuestionName)
        await page.getByRole('textbox', {name: 'Question text'}).fill('Name')
        await adminQuestions.clickSubmitButtonAndNavigate('Update')
        await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()

        await adminPrograms.gotoEditDraftProgramPage(programName)
        await adminPrograms.publishProgram(programName)
        await logout(page)
      })

      await test.step('Apply and verify repeated question renders without $this', async () => {
        await applicantQuestions.applyProgram(programName)
        await addRepeatedEntity(page, 'Pets', 'Bugs')

        await applicantQuestions.clickContinue()
        await applicantQuestions.validateQuestionIsOnPage('Name')
      })
    })

    test('applicant enumerator add/remove flow re-indexes correctly and has no accessibility violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await test.step('Adding three pets keeps clone IDs unique (no a11y violations)', async () => {
        // Each add clones a hidden DOM element; the clones should have unique
        // IDs to avoid accessibility violations.
        await addRepeatedEntity(page, 'Pets', 'Bugs')
        await addRepeatedEntity(page, 'Pets', 'Daffy')
        await addRepeatedEntity(page, 'Pets', 'Goofy')
        await validateAccessibility(page)
      })

      await test.step('Each entry retains its filled value', async () => {
        await expect(entityNameInput(page, 'Pets', 1)).toHaveValue('Bugs')
        await expect(entityNameInput(page, 'Pets', 2)).toHaveValue('Daffy')
        await expect(entityNameInput(page, 'Pets', 3)).toHaveValue('Goofy')
      })

      await test.step('Removing the middle entry re-indexes the remaining entries and stays accessible', async () => {
        await applicantQuestions.deleteEnumeratorEntityByIndex(1)
        await expect(entityNameInput(page, 'Pets', 1)).toHaveValue('Bugs')
        await expect(entityNameInput(page, 'Pets', 2)).toHaveValue('Goofy')
        await validateAccessibility(page)
      })
    })

    test('applicant can fill in lots of blocks, and then go back and delete some repeated entities', async ({
      page,
      applicantQuestions,
    }) => {
      const applicationSummary = page.getByRole('list', {
        name: 'Program application summary',
      })
      const errors = page.locator('.cf-applicant-question-errors:visible')

      await applicantQuestions.applyProgram(programName)

      await test.step('Add two pets', async () => {
        await addRepeatedEntity(page, 'Pets', 'Bugs')
        await addRepeatedEntity(page, 'Pets', 'Daffy')
        await applicantQuestions.clickContinue()
      })

      await test.step("Answer Bugs's name", async () => {
        await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
        await applicantQuestions.clickContinue()
      })

      await test.step('Add one job for Bugs', async () => {
        await addRepeatedEntity(page, 'Jobs', 'Cartoon Character')
        await applicantQuestions.clickContinue()
      })

      await test.step("Answer income for Bugs's Cartoon Character job", async () => {
        await applicantQuestions.answerNumberQuestion('100')
        await applicantQuestions.clickContinue()
      })

      await test.step("Answer Daffy's name", async () => {
        await applicantQuestions.answerNameQuestion('Daffy', 'Duck')
        await applicantQuestions.clickContinue()
      })

      await test.step('Adding a blank job entry triggers the blank-line error', async () => {
        await addRepeatedEntity(page, 'Jobs', '')
        await applicantQuestions.clickContinue()
        await expect(errors).toContainText(
          'Error: Please enter a value for each line.',
        )
      })

      await test.step('Replacing the blank with two duplicate Banker entries triggers the duplicate error', async () => {
        await applicantQuestions.deleteEnumeratorEntity('')
        await addRepeatedEntity(page, 'Jobs', 'Banker')
        await addRepeatedEntity(page, 'Jobs', 'Banker')
        await applicantQuestions.clickContinue()
        await expect(errors).toContainText(
          'Error: Please enter a unique value for each line.',
        )
      })

      await test.step('Remove one duplicate Banker and add Painter', async () => {
        await applicantQuestions.deleteEnumeratorEntityByIndex(1)
        await addRepeatedEntity(page, 'Jobs', 'Painter')
        await applicantQuestions.clickContinue()
      })

      await test.step("Answer incomes for Daffy's two jobs", async () => {
        await applicantQuestions.answerNumberQuestion('31')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerNumberQuestion('12')
        await applicantQuestions.clickContinue()
      })

      await test.step('Review page contains all entered values', async () => {
        await expect(applicationSummary).toContainText('Bugs Bunny')
        await expect(applicationSummary).toContainText('Cartoon Character')
        await expect(applicationSummary).toContainText('100')
        await expect(applicationSummary).toContainText('Daffy Duck')
        await expect(applicationSummary).toContainText('Banker')
        await expect(applicationSummary).toContainText('Painter')
        await expect(applicationSummary).toContainText('31')
        await expect(applicationSummary).toContainText('12')
      })

      await test.step('Edit the Pets enumerator from review and delete Bugs', async () => {
        await applicantQuestions.editQuestionFromReviewPage(
          'List the names of your pets',
        )
        await waitForPageJsLoad(page)
        await applicantQuestions.deleteEnumeratorEntity('Bugs')
        await applicantQuestions.clickContinue()
      })

      await test.step('Bugs and its descendants are gone from the review page', async () => {
        await expect(applicationSummary).not.toContainText('Bugs Bunny')
        await expect(applicationSummary).not.toContainText('Cartoon Character')
        await expect(applicationSummary).not.toContainText('100')
      })

      await test.step('Edit the Pets enumerator from review and add Tweety', async () => {
        await applicantQuestions.editQuestionFromReviewPage(
          'List the names of your pets',
        )
        await waitForPageJsLoad(page)
        await addRepeatedEntity(page, 'Pets', 'Tweety')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerNameQuestion('Tweety', 'Bird')
        await applicantQuestions.clickContinue()
        await applicantQuestions.clickReview()
      })

      await test.step("Review contains Tweety Bird and Daffy's data, not Bugs's", async () => {
        await expect(applicationSummary).toContainText('Tweety Bird')
        await expect(applicationSummary).toContainText('Daffy Duck')
        await expect(applicationSummary).toContainText('Banker')
        await expect(applicationSummary).toContainText('Painter')
        await expect(applicationSummary).toContainText('31')
        await expect(applicationSummary).toContainText('12')
        await expect(applicationSummary).not.toContainText('Bugs Bunny')
        await expect(applicationSummary).not.toContainText('Cartoon Character')
        await expect(applicationSummary).not.toContainText('100')
      })

      await logout(page)
    })

    test('applicant repeated entity add button is enabled/disabled correctly', async ({
      page,
      applicantQuestions,
    }) => {
      const addPetsButton = page.getByRole('button', {name: 'Add Pets'})
      const errors = page.locator('.cf-applicant-question-errors')

      await applicantQuestions.applyProgram(programName)

      await test.step('Add button is disabled when the maximum number of entities is entered', async () => {
        await addRepeatedEntity(page, 'Pets', 'Bugs')
        await addRepeatedEntity(page, 'Pets', 'Daffy')
        await addRepeatedEntity(page, 'Pets', 'Donald')
        await addRepeatedEntity(page, 'Pets', 'Tweety')
        await expect(addPetsButton).toBeDisabled()
      })

      await test.step('Add button is still disabled after navigating away and back', async () => {
        await applicantQuestions.clickContinue()
        await applicantQuestions.clickBack()
        await expect(addPetsButton).toBeDisabled()
      })

      await test.step('Add button is enabled when below the maximum', async () => {
        await applicantQuestions.deleteEnumeratorEntity('Tweety')
        await expect(addPetsButton).toBeEnabled()
      })

      await test.step('Add button is disabled if an entity is blank', async () => {
        await addRepeatedEntity(page, 'Pets', '')
        await expect(addPetsButton).toBeDisabled()
      })

      await test.step('Add button is re-enabled when the blank entity is removed', async () => {
        await applicantQuestions.deleteEnumeratorEntity('')
        await expect(addPetsButton).toBeEnabled()
      })

      await test.step('Add button is still enabled after navigating away and back', async () => {
        await applicantQuestions.clickContinue()
        await applicantQuestions.clickBack()
        await expect(addPetsButton).toBeEnabled()
      })

      await test.step('Add button is disabled when an existing entity is blanked out', async () => {
        await applicantQuestions.editEnumeratorAnswer('Bugs', '')
        await expect(addPetsButton).toBeDisabled()
      })

      await test.step('Add button is still disabled after trying to save with an empty entity', async () => {
        await applicantQuestions.clickContinue()
        await expect(errors).toBeVisible()
        await expect(addPetsButton).toBeDisabled()
      })
    })

    test('applicant cannot enter more than 50 entities for enumerators without a max count', async ({
      page,
      applicantQuestions,
    }) => {
      const addJobsButton = page.getByRole('button', {name: 'Add Jobs'})

      await applicantQuestions.applyProgram(programName)

      await test.step('Add two pets and continue to the repeated screens', async () => {
        await addRepeatedEntity(page, 'Pets', 'Bugs')
        await addRepeatedEntity(page, 'Pets', 'Daffy')
        await applicantQuestions.clickContinue()
      })

      await test.step("Answer Bugs's name", async () => {
        await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
        await applicantQuestions.clickContinue()
      })

      await test.step('Filling the Jobs enumerator with 50 entries disables the Add button', async () => {
        for (let i = 1; i <= 50; i++) {
          await addRepeatedEntity(page, 'Jobs', 'Cartoon Character')
        }
        await expect(addJobsButton).toBeDisabled()
      })

      await logout(page)
    })

    test('applicant can navigate to previous blocks', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await test.step('Add two pets and continue', async () => {
        await addRepeatedEntity(page, 'Pets', 'Bugs')
        await addRepeatedEntity(page, 'Pets', 'Daffy')
        await applicantQuestions.clickContinue()
      })

      await test.step("Answer Bugs's name and continue", async () => {
        await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
        await applicantQuestions.clickContinue()
      })

      await test.step('Add a job for Bugs and continue', async () => {
        await addRepeatedEntity(page, 'Jobs', 'Cartoon Character')
        await applicantQuestions.clickContinue()
      })

      await test.step("Answer income for Bugs's job and continue", async () => {
        await applicantQuestions.answerNumberQuestion('100')
        await applicantQuestions.clickContinue()
      })

      await test.step('Click back: income question retains its value', async () => {
        await applicantQuestions.clickBack()
        await applicantQuestions.checkNumberQuestionValue('100')
      })

      await test.step('Click back: Jobs enumerator retains its entry', async () => {
        await applicantQuestions.clickBack()
        await expect(entityNameInput(page, 'Jobs', 1)).toHaveValue(
          'Cartoon Character',
        )
      })

      await test.step("Click back: Bugs's name question retains its value", async () => {
        await applicantQuestions.clickBack()
        await applicantQuestions.checkNameQuestionValue('Bugs', 'Bunny')
      })

      await test.step('Click back: Pets enumerator retains its entries', async () => {
        await applicantQuestions.clickBack()
        await expect(entityNameInput(page, 'Pets', 1)).toHaveValue('Bugs')
        await expect(entityNameInput(page, 'Pets', 2)).toHaveValue('Daffy')
      })

      await logout(page)
    })
  })

  // Returns the textbox locator for the applicant-facing enumerator entity
  // input at the given 1-based index, keyed off the entity-type label (e.g.
  // "Pets name #1").
  function entityNameInput(page: Page, entityType: string, index: number) {
    return page.getByRole('textbox', {name: `${entityType} name #${index}`})
  }

  // Flag-on equivalent of applicantQuestions.addEnumeratorAnswer. Clicks the
  // "Add {entityType}" button, then fills the freshly-cloned input via
  // entityNameInput (auto-computing its 1-based index from the count of
  // currently visible entity inputs).
  async function addRepeatedEntity(
    page: Page,
    entityType: string,
    entityName: string,
  ) {
    // Substring name match excludes the hidden template (whose label is just
    // "{entityType} name", with no "#N" suffix).
    const visibleInputs = page.getByRole('textbox', {
      name: `${entityType} name #`,
    })
    const newIndex = (await visibleInputs.count()) + 1
    await page.getByRole('button', {name: `Add ${entityType}`}).click()
    await entityNameInput(page, entityType, newIndex).fill(entityName)
  }

  // Adds a new repeated-set block via the program block edit page. Clicks
  // "Add screen", then "Add repeated set" — which creates a parent enumerator
  // block (Screen 2) plus a repeated child block (Screen 3) and leaves focus
  // on the repeated child. Pass {selectParent: true} to also click into the
  // parent (Screen 2) afterward.
  async function addRepeatedSetBlock(
    page: Page,
    options?: {selectParent?: boolean},
  ) {
    await page.getByRole('button', {name: 'Add screen'}).first().click()
    await page.getByRole('button', {name: 'Add repeated set'}).click()
    if (options?.selectParent) {
      await page.getByRole('link', {name: 'Screen 2'}).click()
    }
  }

  async function fillOutEnumeratorQuestionFormCorrectly(
    page: Page,
    options?: {
      listedEntity?: string
      questionText?: string
      adminId?: string
      hintText?: string
      maxEntities?: number
    },
  ) {
    const blockPanel = page.getByTestId('block-panel-edit')
    const {
      listedEntity = 'Pets',
      questionText = 'List the names of your pets.',
      adminId = 'pets enumerator',
      hintText = 'Hint',
      maxEntities,
    } = options ?? {}

    await test.step('Fill out the new enumerator question form and submit it', async () => {
      const listedEntityInput = blockPanel.getByRole('textbox', {
        name: 'Listed entity',
      })
      await expect(listedEntityInput).toHaveAttribute('aria-required', 'true')
      await listedEntityInput.fill(listedEntity)

      const questionTextInput = blockPanel.getByRole('textbox', {
        name: 'Question text',
      })
      await expect(questionTextInput).toHaveAttribute('aria-required', 'true')
      await questionTextInput.fill(questionText)

      const adminIdInput = blockPanel.getByRole('textbox', {
        name: 'Repeated set admin ID',
      })
      await expect(adminIdInput).toHaveAttribute('aria-required', 'true')
      await adminIdInput.fill(adminId)

      await blockPanel.getByRole('textbox', {name: 'Hint text'}).fill(hintText)

      if (maxEntities != null) {
        await blockPanel
          .getByRole('spinbutton', {name: 'Maximum entity count'})
          .fill(String(maxEntities))
      }

      await blockPanel
        .getByRole('button', {name: 'Create repeated set'})
        .click()
      await waitForHtmxReady(page)
    })
  }

  async function expectCurrentBlockTitle(
    isRepeatedBlock: boolean,
    blockPanel: Locator,
    expectedScreenNumber: number,
    repeatedFrom?: number,
  ) {
    if (!isRepeatedBlock) {
      await expect(
        blockPanel.getByText(`Screen ${expectedScreenNumber}`, {
          exact: true,
        }),
      ).toBeVisible()
    } else {
      await expect(
        blockPanel.getByText(
          `[parent label] - Screen ${expectedScreenNumber} (repeated from ${repeatedFrom})`,
        ),
      ).toBeVisible()
    }
  }

  async function navigateToRepeatedScreen(
    page: Page,
    screenNumber: number,
    repeatedFrom: number,
    options?: {
      parentLabel?: string
      childLabel?: string
    },
  ) {
    const {parentLabel = '[parent label]', childLabel} = options ?? {}
    const repeatedLabel = childLabel
      ? `${parentLabel} - ${childLabel}`
      : parentLabel

    await test.step('Navigate to repeated screen', async () => {
      await page
        .getByRole('link', {
          name: `${repeatedLabel} - Screen ${screenNumber} (repeated from ${repeatedFrom})`,
        })
        .click()
    })
  }
})
