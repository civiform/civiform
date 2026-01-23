import {test, expect} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'
import {
  Eligibility,
  FormField,
  ProgramType,
  ProgramVisibility,
} from '../support/admin_programs'
import {dismissModal, waitForAnyModalLocator} from '../support/wait'
import {Page} from '@playwright/test'

test.describe('program creation', () => {
  // TODO(#10363): Remove test once external program cards feature is rolled out
  test('create program page', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await disableFeatureFlag(page, 'external_program_cards_enabled')
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName, {
      description: 'description',
      selectedTI: 'selectedTI',
      confirmationMessage: 'confirmationMessage',
      eligibility: Eligibility.IS_GATING,
      submitNewProgram: false,
    })
    await adminPrograms.expectProgramDetailsSaveAndContinueButton()
    await validateScreenshot(page, 'program-creation-page')

    // When the program submission goes through,
    // verify we're redirected to the program image upload page.
    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()
  })

  test('create program page with external program cards feature', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName, {
      description: 'description',
      selectedTI: 'selectedTI',
      confirmationMessage: 'confirmationMessage',
      eligibility: Eligibility.IS_GATING,
      submitNewProgram: false,
    })
    await adminPrograms.expectProgramDetailsSaveAndContinueButton()
    await validateScreenshot(
      page,
      'program-creation-page-with-external-programs-feature',
    )

    // When the program submission goes through, verify we're redirected to the
    // program image upload page.
    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()
  })

  // TODO(#10363): Remove test once external program cards feature is rolled out
  test('create program with disabled visibility', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await disableFeatureFlag(page, 'external_program_cards_enabled')
    await loginAsAdmin(page)

    await adminPrograms.addProgram('program name', {
      description: 'description',
      visibility: ProgramVisibility.DISABLED,
      selectedTI: 'selectedTI',
      confirmationMessage: 'confirmationMessage',
      eligibility: Eligibility.IS_GATING,
      submitNewProgram: false,
    })
    await adminPrograms.expectProgramDetailsSaveAndContinueButton()
    expect(await page.innerText('id=program-details-form')).toContain(
      'Disabled',
    )
    await validateScreenshot(page, 'program-creation-page-disabled-visibility')

    // When the program submission goes through,
    // verify we're redirected to the program image upload page.
    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()
  })

  test('create program with disabled visibility and external programs feature enabled', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')
    await loginAsAdmin(page)

    await adminPrograms.addProgram('program name', {
      description: 'description',
      visibility: ProgramVisibility.DISABLED,
      selectedTI: 'selectedTI',
      confirmationMessage: 'confirmationMessage',
      eligibility: Eligibility.IS_GATING,
      submitNewProgram: false,
    })
    await adminPrograms.expectProgramDetailsSaveAndContinueButton()
    expect(await page.innerText('id=program-details-form')).toContain(
      'Disabled',
    )
    await validateScreenshot(
      page,
      'program-creation-page-disabled-visibility-with-external-programs-feature',
    )

    // When the program submission goes through,
    // verify we're redirected to the program image upload page.
    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()
  })

  test('renders saved values for application steps', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    const programName = 'program name'
    const titleOne = 'Title one'
    const descriptionOne = 'Description one'
    const titleTwo = 'Title two'
    const descriptionTwo = 'Description two'
    const titleThree = 'Title three'
    const descriptionThree = 'Description three'

    await test.step('add program with multiple application steps', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName, {
        description: 'description',
        selectedTI: 'selectedTI',
        confirmationMessage: 'confirmationMessage',
        eligibility: Eligibility.IS_GATING,
        applicationSteps: [
          {title: titleOne, description: descriptionOne},
          {title: titleTwo, description: descriptionTwo},
          {title: titleThree, description: descriptionThree},
        ],
      })
      await adminProgramImage.expectProgramImagePage()
    })

    await test.step('navigate back to program edit page and confirm application step values show up', async () => {
      await adminProgramImage.clickBackButton()
      await adminPrograms.expectProgramEditPage(programName)
      await expect(
        page.getByRole('textbox', {name: 'Step 1 title'}),
      ).toHaveValue(titleOne)
      await expect(
        page.getByRole('textbox', {name: 'Step 1 description'}),
      ).toHaveValue(descriptionOne)
      await expect(
        page.getByRole('textbox', {name: 'Step 2 title'}),
      ).toHaveValue(titleTwo)
      await expect(
        page.getByRole('textbox', {name: 'Step 2 description'}),
      ).toHaveValue(descriptionTwo)
      await expect(
        page.getByRole('textbox', {name: 'Step 3 title'}),
      ).toHaveValue(titleThree)
      await expect(
        page.getByRole('textbox', {name: 'Step 3 description'}),
      ).toHaveValue(descriptionThree)
    })
  })

  test('blank application steps are ignored', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    const programName = 'program name'
    const titleOne = 'Title one'
    const descriptionOne = 'Description one'
    const titleThree = 'Title three'
    const descriptionThree = 'Description three'

    await test.step('add program with blank application step', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName, {
        description: 'description',
        selectedTI: 'selectedTI',
        confirmationMessage: 'confirmationMessage',
        eligibility: Eligibility.IS_GATING,
        applicationSteps: [
          {title: titleOne, description: descriptionOne},
          {title: '', description: ''}, // step 2 is blank
          {title: titleThree, description: descriptionThree},
        ],
      })
      await adminProgramImage.expectProgramImagePage()
    })

    await test.step('navigate back to program edit page and confirm previously blank step is ignored', async () => {
      await adminProgramImage.clickBackButton()
      await adminPrograms.expectProgramEditPage(programName)

      await expect(
        page.getByRole('textbox', {name: 'Step 1 title'}),
      ).toHaveValue(titleOne)
      await expect(
        page.getByRole('textbox', {name: 'Step 1 description'}),
      ).toHaveValue(descriptionOne)
      // values that were entered for step three now show up under step 2
      await expect(
        page.getByRole('textbox', {name: 'Step 2 title'}),
      ).toHaveValue(titleThree)
      await expect(
        page.getByRole('textbox', {name: 'Step 2 description'}),
      ).toHaveValue(descriptionThree)
      // step three is blank
      await expect(
        page.getByRole('textbox', {name: 'Step 3 title'}),
      ).toHaveValue('')
      await expect(
        page.getByRole('textbox', {name: 'Step 3 description'}),
      ).toHaveValue('')
    })
  })

  test('create program then go back prevents URL edits', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName, {
      description: 'description',
      selectedTI: 'selectedTI',
      confirmationMessage: 'confirmationMessage',
      eligibility: Eligibility.IS_GATING,
      submitNewProgram: false,
    })

    await test.step('On program creation, admin can fill in the program slug.', async () => {
      expect(await page.locator('#program-slug').count()).toEqual(1)

      await adminPrograms.submitProgramDetailsEdits()
      await adminProgramImage.expectProgramImagePage()
    })

    await test.step('On program edit, admin cannot edit the program slug (which is used for the URL)', async () => {
      await adminProgramImage.clickBackButton()
      await adminPrograms.expectProgramEditPage(programName)
      await expect(page.locator('#program-slug')).toBeHidden()
    })
  })

  test('create program then go back can still go forward', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminProgramImage.expectProgramImagePage()

    // WHEN the admin goes back to the program details page
    await adminProgramImage.clickBackButton()

    // THEN they should be able to still go forward to the program images page again.
    await adminPrograms.expectProgramEditPage(programName)
    await adminPrograms.expectProgramDetailsSaveAndContinueButton()

    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()
  })

  test('program confirmation preview shows empty lines and markdown formatting', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)

    await expect(
      page.locator('#program-confirmation-message-preview'),
    ).toBeVisible()
    await validateScreenshot(
      page,
      'program-description-page-with-external-programs-feature',
    )
  })

  // TODO(#10363): Remove test once external program cards feature is rolled out
  test('program details page screenshot', async ({page, adminPrograms}) => {
    await disableFeatureFlag(page, 'external_program_cards_enabled')

    await loginAsAdmin(page)
    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)
    await validateScreenshot(page, 'program-description-page')
  })

  test('program details page screenshot with external programs feature enabled', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')

    await loginAsAdmin(page)
    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)
    await validateScreenshot(
      page,
      'program-description-page-with-external-programs-feature',
    )
  })

  test('program details page redirects to block page', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Program Name'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)
    await adminPrograms.submitProgramDetailsEdits()

    await adminPrograms.expectProgramBlockEditPage()
  })

  test('shows correct formatting during question creation', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    await adminQuestions.createStaticQuestion({
      questionName: 'static-question',
      questionText:
        'This is an example of some static text with formatting\n' +
        '* List Item 1\n' +
        '* List Item 2\n' +
        '\n' +
        '[This is a link](https://www.example.com)\n',
    })

    const previewLocator = page.locator('#sample-question')
    await expect(previewLocator).toContainText('This is an example')

    await validateScreenshot(
      previewLocator,
      'program-creation-static-question-with-formatting',
    )
  })

  test('preserves blank lines in question preview', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    await adminQuestions.createStaticQuestion({
      questionName: 'static-question',
      questionText:
        'Here is the first line\n' +
        '\n' +
        'Here is some more text after a blank line\n' +
        '\n' +
        '\n' +
        'Here is more text after more blank lines',
    })

    const previewLocator = page.locator('#sample-question')
    await expect(previewLocator).toContainText('Here is the first line')

    await validateScreenshot(
      previewLocator,
      'program-creation-static-question-with-blank-lines',
    )
  })

  test('create program and search for questions', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    await adminQuestions.addAddressQuestion({
      questionName: 'address-w-admin-note',
      description: 'this is a note',
    })

    await adminQuestions.addAddressQuestion({
      questionName: 'address-universal-w-admin-note',
      description: 'universal note',
      universal: true,
    })

    const programName = 'search-program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(
      programName,
      'search program description',
    )

    await adminPrograms.openQuestionBank()

    expect(await page.innerText('id=question-bank-nonuniversal')).toContain(
      'address-w-admin-note',
    )
    expect(await page.innerText('id=question-bank-nonuniversal')).toContain(
      'this is a note',
    )
    expect(await page.innerText('id=question-bank-universal')).toContain(
      'address-universal-w-admin-note',
    )
    expect(await page.innerText('id=question-bank-universal')).toContain(
      'universal note',
    )

    await validateScreenshot(
      page.locator('.cf-question-bank-panel'),
      'open-question-search',
      {fullPage: false},
    )
  })

  test('create program with enumerator and repeated questions', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    await adminQuestions.addAddressQuestion({questionName: 'apc-address'})
    await adminQuestions.addNameQuestion({questionName: 'apc-name'})
    await adminQuestions.addTextQuestion({questionName: 'apc-text'})
    await adminQuestions.addEnumeratorQuestion({
      questionName: 'apc-enumerator',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'apc-repeated',
      description: 'description',
      questionText: '$this text',
      helpText: '$this helptext',
      enumeratorName: 'apc-enumerator',
    })

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'apc program description')

    // All non-repeated questions should be available in the question bank
    expect(await page.innerText('id=question-bank-nonuniversal')).toContain(
      'apc-address',
    )
    expect(await page.innerText('id=question-bank-nonuniversal')).toContain(
      'apc-name',
    )
    expect(await page.innerText('id=question-bank-nonuniversal')).toContain(
      'apc-text',
    )
    expect(await page.innerText('id=question-bank-nonuniversal')).toContain(
      'apc-enumerator',
    )
    expect(await page.innerText('id=question-bank-nonuniversal')).not.toContain(
      'apc-repeated',
    )

    // Add a non-enumerator question and the enumerator option should go away
    await adminPrograms.addQuestionFromQuestionBank('apc-name')
    expect(await page.innerText('id=question-bank-nonuniversal')).not.toContain(
      'apc-enumerator',
    )
    expect(await page.innerText('id=question-bank-nonuniversal')).not.toContain(
      'apc-repeated',
    )

    // Remove the non-enumerator question and add a enumerator question. All options should go away.
    await page.click(
      '.cf-program-question:has-text("apc-name") >> .cf-remove-question-button',
    )
    await adminPrograms.addQuestionFromQuestionBank('apc-enumerator')

    await expect(page.locator('id=question-bank-nonuniversal')).toHaveText('')

    // Create a repeated block. The repeated question should be the only option.
    await page.click('#create-repeated-block-button')
    expect(await page.innerText('id=question-bank-nonuniversal')).toContain(
      'apc-repeated',
    )
  })

  test('create program with address and address correction feature enabled', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    await adminQuestions.addAddressQuestion({questionName: 'ace-address'})
    await adminQuestions.addNameQuestion({questionName: 'ace-name'})

    const programName = 'Ace program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'ace program description')

    await adminPrograms.addQuestionFromQuestionBank('ace-address')
    await adminPrograms.addQuestionFromQuestionBank('ace-name')

    await validateScreenshot(
      page.locator(
        adminPrograms.questionCardSelectorInProgramView('ace-address'),
      ),
      'program-detail-page-with-address-correction-false',
    )

    const addressCorrectionInput = adminPrograms.getAddressCorrectionToggle()

    await expect(addressCorrectionInput).toHaveValue('false')

    await adminPrograms.clickAddressCorrectionToggle()

    await expect(addressCorrectionInput).toHaveValue('true')

    await validateScreenshot(
      page.locator(
        adminPrograms.questionCardSelectorInProgramView('ace-address'),
      ),
      'program-detail-page-with-address-correction-true',
    )

    // ensure that non address question does not contain address correction button
    expect(
      await page.innerText(
        adminPrograms.questionCardSelectorInProgramView('ace-name'),
      ),
    ).not.toContain('Address correction')
  })

  test('create program with multiple address questions, address correction feature enabled, and can only enable correction on one address', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    const helpText =
      'This screen already contains a question with address correction enabled'

    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    await adminQuestions.addAddressQuestion({questionName: 'ace-address-one'})
    await adminQuestions.addAddressQuestion({questionName: 'ace-address-two'})
    await adminQuestions.addNameQuestion({questionName: 'ace-name'})

    const programName = 'Ace program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'ace program description')

    await adminPrograms.addQuestionFromQuestionBank('ace-address-one')
    await adminPrograms.addQuestionFromQuestionBank('ace-address-two')
    await adminPrograms.addQuestionFromQuestionBank('ace-name')

    await validateScreenshot(
      page.locator('#questions-section'),
      'program-detail-page-with-multiple-address-correction-false',
    )

    const addressCorrectionInput1 =
      adminPrograms.getAddressCorrectionToggleByName('ace-address-one')
    const addressCorrectionInput2 =
      adminPrograms.getAddressCorrectionToggleByName('ace-address-two')
    const addressCorrectionHelpText1 =
      adminPrograms.getAddressCorrectionHelpTextByName('ace-address-one')
    const addressCorrectionHelpText2 =
      adminPrograms.getAddressCorrectionHelpTextByName('ace-address-two')

    await expect(addressCorrectionInput1).toHaveValue('false')
    await expect(addressCorrectionInput2).toHaveValue('false')

    expect(await addressCorrectionHelpText1.innerText()).not.toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).not.toContain(helpText)

    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-one')

    await expect(addressCorrectionInput1).toHaveValue('true')
    await expect(addressCorrectionInput2).toHaveValue('false')
    expect(await addressCorrectionHelpText1.innerText()).not.toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).toContain(helpText)

    await validateScreenshot(
      page.locator('#questions-section'),
      'program-detail-page-with-first-address-correction-true',
    )

    // Trying to toggle the other one should not do anything
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-two')

    await expect(addressCorrectionInput1).toHaveValue('true')
    await expect(addressCorrectionInput2).toHaveValue('false')
    expect(await addressCorrectionHelpText1.innerText()).not.toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).toContain(helpText)

    await validateScreenshot(
      page.locator('#questions-section'),
      'program-detail-page-with-first-address-correction-true-second-false',
    )

    // Once we untoggle the first one, we should be able to toggle the second one
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-one')
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-two')

    await expect(addressCorrectionInput1).toHaveValue('false')
    await expect(addressCorrectionInput2).toHaveValue('true')
    expect(await addressCorrectionHelpText1.innerText()).toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).not.toContain(helpText)

    await validateScreenshot(
      page.locator('#questions-section'),
      'program-detail-page-with-second-address-correction-true',
    )

    // ensure that non address question does not contain address correction button
    expect(
      await page.innerText(
        adminPrograms.questionCardSelectorInProgramView('ace-name'),
      ),
    ).not.toContain('Address correction')
  })

  test('create program with address and address correction feature disabled', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    await disableFeatureFlag(page, 'esri_address_correction_enabled')

    await adminQuestions.addAddressQuestion({questionName: 'acd-address'})

    const programName = 'Acd program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'acd program description')

    await adminPrograms.addQuestionFromQuestionBank('acd-address')

    const addressCorrectionInput = adminPrograms.getAddressCorrectionToggle()

    await expect(addressCorrectionInput).toHaveValue('false')

    await adminPrograms.clickAddressCorrectionToggle()
    // should be the same as before with button submit disabled
    await expect(addressCorrectionInput).toHaveValue('false')
  })

  test('create program with markdown questions', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: 'a',
      questionText: '*italics*',
      helpText: '*italic help text*',
      markdown: true,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'b',
      questionText: '**bold**',
      helpText: '**bold help text**',
      markdown: true,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'c',
      questionText: '[link](example.com)',
      helpText: '[linked help text](example.com)',
      markdown: true,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'd',
      questionText: '[questionBank](example.com)',
      helpText: '[questionBank help text](e.com)',
      markdown: true,
    })

    const programName = 'Acd program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'acd program description')

    await adminPrograms.addQuestionFromQuestionBank('a')
    await adminPrograms.addQuestionFromQuestionBank('b')
    await adminPrograms.addQuestionFromQuestionBank('c')

    await validateScreenshot(page, 'program-detail-markdown')

    await adminPrograms.gotoEditDraftProgramPage(programName)
    await adminPrograms.openQuestionBank()
    await validateScreenshot(
      page.locator('.cf-question-bank-panel'),
      'question-bank-markdown',
      {fullPage: false},
    )
  })

  test('change questions order within block', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const color = 'favorite-color'
    const movie = 'favorite-movie'
    const song = 'favorite-song'
    for (const question of [movie, color, song]) {
      await adminQuestions.addTextQuestion({questionName: question})
    }

    const programName = 'Apc program 2'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'apc program description')

    await validateScreenshot(page, 'program-creation-question-bank-initial')

    for (const question of [movie, color, song]) {
      await adminPrograms.addQuestionFromQuestionBank(question)
    }
    // verify original order
    await expectQuestionsOrderWithinBlock(page, [movie, color, song])

    // move movie question down
    await page.click(
      adminPrograms.selectWithinQuestionWithinBlock(
        movie,
        '[aria-label="move down"]',
      ),
    )
    await expectQuestionsOrderWithinBlock(page, [color, movie, song])

    // move song question up
    await page.click(
      adminPrograms.selectWithinQuestionWithinBlock(
        song,
        '[aria-label="move up"]',
      ),
    )
    await expectQuestionsOrderWithinBlock(page, [color, song, movie])

    await validateScreenshot(page, 'program-creation')
  })

  test('create question from question bank', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Apc program 3'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoEditDraftProgramPage(programName)
    await adminPrograms.openQuestionBank()
    await validateScreenshot(
      page.locator('.cf-question-bank-panel'),
      'question-bank-empty',
      {fullPage: false},
    )
    await page.click('#create-question-button')
    await page.click('#create-text-question')
    await waitForPageJsLoad(page)

    const questionName = 'new-from-question-bank'
    const questionText = 'Question text'
    await adminQuestions.fillInQuestionBasics({
      questionName: questionName,
      description: '',
      questionText: questionText,
      helpText: 'Question help text',
    })
    await adminQuestions.clickSubmitButtonAndNavigate('Create')
    await adminPrograms.expectSuccessToast(`question ${questionName} created`)
    await adminPrograms.expectProgramBlockEditPage(programName)

    await page.click('#create-question-button')
    await page.click('#create-text-question')
    await waitForPageJsLoad(page)

    const universalQuestionName = 'new-universal-from-question-bank'
    const universalQuestionText = 'Universal question text'
    await adminQuestions.fillInQuestionBasics({
      questionName: universalQuestionName,
      description: '',
      questionText: universalQuestionText,
      helpText: 'Universal question help text',
      universal: true,
    })
    await adminQuestions.clickSubmitButtonAndNavigate('Create')
    await adminPrograms.expectSuccessToast(
      `question ${universalQuestionName} created`,
    )
    await adminPrograms.expectProgramBlockEditPage(programName)
    await validateScreenshot(
      page.locator('.cf-question-bank-panel'),
      'question-bank-with-created-question',
      {fullPage: false},
    )

    await adminQuestions.expectDraftQuestionExist(questionName, questionText)
    await adminQuestions.expectDraftQuestionExist(
      universalQuestionName,
      universalQuestionText,
    )
    // Ensure the question can be added from the question bank.
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      questionName,
    ])
    await adminPrograms.editProgramBlock(programName, 'new dummy description', [
      universalQuestionName,
    ])
  })

  test('edit question from program screen', async ({
    page,
    adminPrograms,
    adminQuestions,
    adminProgramImage,
  }) => {
    await test.step('setup program with question', async () => {
      await loginAsAdmin(page)
      await adminQuestions.addTextQuestion({questionName: 'text-question'})
      await adminPrograms.addProgram('Test program')
      await adminProgramImage.clickContinueButton()
      await adminPrograms.addQuestionFromQuestionBank('text-question')
    })

    await test.step('click edit question link', async () => {
      await adminPrograms.editQuestion('text-question')
      await adminQuestions.expectQuestionEditPage('text-question')
    })
  })

  test('all questions shown on question bank before filtering, then filters based on different attributes correctly', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: 'q-f',
      questionText: 'first question',
      description: 'qf-description-text',
      helpText: 'qf-help-text',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'q-s',
      questionText: 'second question',
      description: 'qs-description-text',
      helpText: 'qs-help-text',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'q-f-u',
      questionText: 'universal first question',
      description: 'qf-description-text-universal',
      helpText: 'qf-help-text-universal',
      universal: true,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'q-s-u',
      questionText: 'universal second question',
      description: 'qs-description-test-universal',
      helpText: 'qs-help-text-universal',
      universal: true,
    })

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName)
    await adminPrograms.openQuestionBank()

    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['second question\n', 'first question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal second question\n', 'universal first question\n'])

    // Filter questions based on text
    await page.locator('#question-bank-filter').fill('fi')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['first question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal first question\n'])

    await page.locator('#question-bank-filter').fill('se')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['second question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal second question\n'])

    await page.locator('#question-bank-filter').fill('')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['second question\n', 'first question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal second question\n', 'universal first question\n'])

    // Filter questions based on name
    await page.locator('#question-bank-filter').fill('q-f')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['first question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal first question\n'])

    await page.locator('#question-bank-filter').fill('q-s')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['second question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal second question\n'])

    // Filter questions based on help text
    await page.locator('#question-bank-filter').fill('qf-help')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['first question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal first question\n'])

    await page.locator('#question-bank-filter').fill('qs-help')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['second question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal second question\n'])

    // Filter questions based on description
    await page.locator('#question-bank-filter').fill('qf-desc')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['first question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal first question\n'])

    await page.locator('#question-bank-filter').fill('qs-desc')
    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual(['second question\n'])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual(['universal second question\n'])

    // All question UIs will have an "Add" button, so ensure filtering to "Add" doesn't just show
    // every question
    await page.locator('#question-bank-filter').fill('add')

    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual([])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual([])

    // All question UIs will have an "Admin ID" field, so ensure filtering to "Admin ID"
    // doesn't just show every question.
    await page.locator('#question-bank-filter').fill('admin id')

    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual([])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual([])

    // Many question UIs will have an "Admin Note" field (description), so ensure filtering to "Admin Note"
    // doesn't just show all questions with an admin note.
    await page.locator('#question-bank-filter').fill('admin note')

    expect(
      await adminPrograms.questionBankNames(/* universal= */ false),
    ).toEqual([])
    expect(
      await adminPrograms.questionBankNames(/* universal= */ true),
    ).toEqual([])
  })

  /**
   * There was a bug where if you deleted the first block
   * and then go to edit the program, it would error
   * because it would go to the block with ID == 1
   */
  test('delete first block and edit', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programName = 'Test program 5'
    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName)
    await adminPrograms.removeProgramBlock(programName, 'Screen 1')
    // removing the first screen of a draft resulted in an error when a user would go to to edit the draft
    await adminPrograms.gotoEditDraftProgramPage(programName)
    await adminPrograms.publishProgram(programName)
    // removing the first screen of a program without drafts, caused an error when a user went to edit the program
    await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
  })

  test('delete last block and edit', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programName = 'Test program 6'
    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName)
    await adminPrograms.removeProgramBlock(programName, 'Screen 2')
    await adminPrograms.gotoEditDraftProgramPage(programName)
  })

  test('correctly renders delete screen confirmation modal', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Test program 7'
    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName)
    await adminPrograms.launchRemoveProgramBlockModal(programName, 'Screen 1')
    await validateScreenshot(
      page.locator('#block-delete-modal'),
      'delete-screen-confirmation-modal',
      {fullPage: false},
    )
  })

  test('eligibility is gating selected by default', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)

    await expect(adminPrograms.getEligibilityIsGatingInput()).toBeChecked()
    await expect(
      adminPrograms.getEligibilityIsNotGatingInput(),
    ).not.toBeChecked()
  })

  test('can select eligibility is not gating', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)

    await adminPrograms.chooseEligibility(Eligibility.IS_NOT_GATING)

    await expect(adminPrograms.getEligibilityIsGatingInput()).not.toBeChecked()
    await expect(adminPrograms.getEligibilityIsNotGatingInput()).toBeChecked()

    await adminPrograms.submitProgramDetailsEdits()
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  // TODO(#10363): Remove test once external program cards feature is rolled out
  test('create pre-screener form', async ({page, adminPrograms, seeding}) => {
    await disableFeatureFlag(page, 'external_program_cards_enabled')

    await test.step('seed categories', async () => {
      await seeding.seedProgramsAndCategories()
      await page.goto('/')
    })

    await loginAsAdmin(page)
    const programName = 'Apc program'

    await test.step('create new program that is not an pre-screener form', async () => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramDescriptionPage(programName)
    })

    await test.step('add category to program', async () => {
      await page.getByText('Education').check()
    })

    const preScreenerFormInput = adminPrograms.getPreScreenerFormToggle()

    await test.step('expect pre-screener toggle not to be checked', async () => {
      await expect(preScreenerFormInput).not.toBeChecked()
    })

    await test.step('click pre-screener toggle and expect it to be checked', async () => {
      await adminPrograms.clickPreScreenerFormToggle()
      await expect(preScreenerFormInput).toBeChecked()
    })

    await test.step('expect non-applicable fields to be unchecked and disabled', async () => {
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_CATEGORIES)
      await adminPrograms.expectFormFieldDisabled(FormField.APPLICATION_STEPS)
    })

    await test.step('save program', async () => {
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.expectProgramBlockEditPage(programName)
    })

    await test.step('edit program and confirm fields are still disabled', async () => {
      await adminPrograms.goToProgramDescriptionPage(programName)
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_CATEGORIES)
      await adminPrograms.expectFormFieldDisabled(FormField.APPLICATION_STEPS)
    })

    await test.step('click pre-screener toggle and confirm fields are re-enabled', async () => {
      await adminPrograms.clickPreScreenerFormToggle()
      await adminPrograms.expectFormFieldEnabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldEnabled(FormField.PROGRAM_CATEGORIES)
      await adminPrograms.expectFormFieldEnabled(FormField.APPLICATION_STEPS)
    })
  })

  // TODO(#10363): Remove test once external program cards feature is rolled out
  test('correctly renders pre-screener form change confirmation modal', async ({
    page,
    adminPrograms,
  }) => {
    await disableFeatureFlag(page, 'external_program_cards_enabled')

    await loginAsAdmin(page)

    const preScreenerFormProgramName = 'Benefits finder'
    await adminPrograms.addPreScreener(
      preScreenerFormProgramName,
      'short program description',
      ProgramVisibility.PUBLIC,
    )

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)

    await adminPrograms.goToProgramDescriptionPage(programName)
    await adminPrograms.clickPreScreenerFormToggle()
    await page.click('#program-update-button')

    let modal = await waitForAnyModalLocator(page)
    await expect(modal).toContainText('Confirm pre-screener change?')

    await validateScreenshot(
      page.locator('#confirm-pre-screener-change'),
      'confirm-pre-screener-change-modal',
      {fullPage: false},
    )

    // Modal gets re-rendered if needed.
    await dismissModal(page)
    await page.click('#program-update-button')
    modal = await waitForAnyModalLocator(page)
    await expect(modal).toContainText('Confirm pre-screener change?')

    await page.click('#confirm-pre-screener-change-button')
    await waitForPageJsLoad(page)
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  test('correctly renders pre-screener form change confirmation modal with external programs feature enabled', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')

    await loginAsAdmin(page)

    const preScreenerFormProgramName = 'Benefits finder'
    await adminPrograms.addPreScreener(
      preScreenerFormProgramName,
      'short program description',
      ProgramVisibility.PUBLIC,
    )

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)

    await adminPrograms.goToProgramDescriptionPage(programName)
    await adminPrograms.selectProgramType(ProgramType.PRE_SCREENER)
    await page.click('#program-update-button')

    let modal = await waitForAnyModalLocator(page)
    await expect(modal).toContainText('Confirm pre-screener change?')

    await validateScreenshot(
      page.locator('#confirm-pre-screener-change'),
      'confirm-pre-screener-change-modal',
      {fullPage: false},
    )

    // Modal gets re-rendered if needed.
    await dismissModal(page)
    await page.click('#program-update-button')
    modal = await waitForAnyModalLocator(page)
    await expect(modal).toContainText('Confirm pre-screener change?')

    await page.click('#confirm-pre-screener-change-button')
    await waitForPageJsLoad(page)
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  test('regular program has eligibility conditions', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    await adminPrograms.addProgram('cif', {
      description: 'desc',
    })

    await adminPrograms.gotoEditDraftProgramPage('cif')
    expect(await page.innerText('main')).toContain('Eligibility')
  })

  test('pre-screener form does not have eligibility conditions', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    await adminPrograms.addPreScreener(
      'cif',
      'short program description',
      ProgramVisibility.PUBLIC,
    )

    await adminPrograms.gotoEditDraftProgramPage('cif')
    expect(await page.innerText('main')).not.toContain('Eligibility')
  })

  test('create program with universal questions', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    await adminQuestions.addAddressQuestion({
      questionName: 'universal-address',
      universal: true,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'nonuniversal-text',
      universal: false,
    })
    await adminQuestions.addPhoneQuestion({
      questionName: 'universal-phone',
      universal: true,
    })

    const programName = 'Program with universal questions'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(
      programName,
      'universal program description',
    )
    await adminPrograms.addQuestionFromQuestionBank('universal-address')
    await adminPrograms.addQuestionFromQuestionBank('nonuniversal-text')
    await adminPrograms.addQuestionFromQuestionBank('universal-phone')

    await validateScreenshot(
      page,
      'program-block-edit-with-universal-questions',
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'universal-address',
      true,
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'nonuniversal-text',
      false,
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'universal-phone',
      true,
    )
  })

  // TODO(#10363): Remove test once external program cards feature is rolled out
  test('create and update program with categories', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await disableFeatureFlag(page, 'external_program_cards_enabled')

    await seeding.seedProgramsAndCategories()
    await page.goto('/')

    const programName = 'program with categories'

    await test.step('create new program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName, {
        description: 'description',
        selectedTI: 'selectedTI',
        confirmationMessage: 'confirmationMessage',
        eligibility: Eligibility.IS_GATING,
        submitNewProgram: false,
      })
    })

    await test.step('add categories to program', async () => {
      await page.getByText('Education').check()
      await page.getByText('Healthcare').check()
    })

    await test.step('validate screenshot', async () => {
      await validateScreenshot(
        page.locator('#program-details-form'),
        'program-creation-with-categories',
      )
    })

    await expect(page.getByText('Education')).toBeChecked()
    await expect(page.getByText('Healthcare')).toBeChecked()

    await test.step('submit and publish program', async () => {
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishProgram(programName)
    })

    await test.step('go to program edit form and check that categories are still pre-selected', async () => {
      await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
      await page.getByRole('button', {name: 'Edit program details'}).click()
      await waitForPageJsLoad(page)
      await expect(page.getByText('Education')).toBeChecked()
      await expect(page.getByText('Healthcare')).toBeChecked()
    })

    await test.step('add another category', async () => {
      await page.getByText('Internet').check()
    })

    await test.step('submit and return to edit form to ensure categories are still pre-selected', async () => {
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.goToProgramDescriptionPage(programName)
      await expect(page.getByText('Education')).toBeChecked()
      await expect(page.getByText('Healthcare')).toBeChecked()
      await expect(page.getByText('Internet')).toBeChecked()
    })
  })

  test('create and update program with categories and external programs feature enabled', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')

    await seeding.seedProgramsAndCategories()
    await page.goto('/')

    const programName = 'program with categories'

    await test.step('create new program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName, {
        description: 'description',
        selectedTI: 'selectedTI',
        confirmationMessage: 'confirmationMessage',
        eligibility: Eligibility.IS_GATING,
        submitNewProgram: false,
      })
    })

    await test.step('add categories to program', async () => {
      await page.getByText('Education').check()
      await page.getByText('Healthcare').check()
    })

    await test.step('validate screenshot', async () => {
      await validateScreenshot(
        page.locator('#program-details-form'),
        'program-creation-with-categories-and-external-programs-feature',
      )
    })

    await expect(page.getByText('Education')).toBeChecked()
    await expect(page.getByText('Healthcare')).toBeChecked()

    await test.step('submit and publish program', async () => {
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishProgram(programName)
    })

    await test.step('go to program edit form and check that categories are still pre-selected', async () => {
      await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
      await page.getByRole('button', {name: 'Edit program details'}).click()
      await waitForPageJsLoad(page)
      await expect(page.getByText('Education')).toBeChecked()
      await expect(page.getByText('Healthcare')).toBeChecked()
    })

    await test.step('add another category', async () => {
      await page.getByText('Internet').check()
    })

    await test.step('submit and return to edit form to ensure categories are still pre-selected', async () => {
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.goToProgramDescriptionPage(programName)
      await expect(page.getByText('Education')).toBeChecked()
      await expect(page.getByText('Healthcare')).toBeChecked()
      await expect(page.getByText('Internet')).toBeChecked()
    })
  })

  test('create pre-screener form with external programs enabled', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')

    await test.step('seed categories', async () => {
      await seeding.seedProgramsAndCategories()
      await page.goto('/')
    })

    await loginAsAdmin(page)
    const programName = 'Apc program'

    await test.step('create new program that is not an pre-screener form', async () => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramDescriptionPage(programName)
    })

    await test.step("expect program type field to be 'default'", async () => {
      await adminPrograms.expectProgramTypeSelected(ProgramType.DEFAULT)
    })

    await test.step('expect non-applicable fields for default programs to have disabled state', async () => {
      await adminPrograms.expectFormFieldDisabled(
        FormField.PROGRAM_EXTERNAL_LINK,
      )
    })

    await test.step('add category to default program', async () => {
      await page.getByText('Education').check()
    })

    await test.step("select 'pre-screener' program type", async () => {
      await adminPrograms.selectProgramType(ProgramType.PRE_SCREENER)
      await adminPrograms.expectProgramTypeSelected(ProgramType.PRE_SCREENER)
    })

    await test.step('expect fields for pre-screeners to have the correct disabled state', async () => {
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_CATEGORIES)
      await adminPrograms.expectFormFieldDisabled(
        FormField.PROGRAM_EXTERNAL_LINK,
      )
      await adminPrograms.expectFormFieldDisabled(FormField.LONG_DESCRIPTION)
      await adminPrograms.expectFormFieldDisabled(FormField.APPLICATION_STEPS)
    })

    await test.step("select 'default' program type", async () => {
      await adminPrograms.selectProgramType(ProgramType.DEFAULT)
      await adminPrograms.expectProgramTypeSelected(ProgramType.DEFAULT)
    })

    await test.step('expect fields for default programs to have the correct enabled/disabled state', async () => {
      await adminPrograms.expectFormFieldEnabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldEnabled(FormField.PROGRAM_CATEGORIES)
      await adminPrograms.expectFormFieldEnabled(FormField.LONG_DESCRIPTION)
      await adminPrograms.expectFormFieldEnabled(FormField.APPLICATION_STEPS)
      // External link remains disabled for default programs
      await adminPrograms.expectFormFieldDisabled(
        FormField.PROGRAM_EXTERNAL_LINK,
      )
    })

    await test.step("select 'pre-screener' program type and save program", async () => {
      await adminPrograms.selectProgramType(ProgramType.PRE_SCREENER)
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.expectProgramBlockEditPage(programName)
    })

    await test.step('edit program and confirm fields are still disabled', async () => {
      await adminPrograms.goToProgramDescriptionPage(programName)
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_CATEGORIES)
      await adminPrograms.expectFormFieldDisabled(
        FormField.PROGRAM_EXTERNAL_LINK,
      )
      await adminPrograms.expectFormFieldDisabled(FormField.LONG_DESCRIPTION)
      await adminPrograms.expectFormFieldDisabled(FormField.APPLICATION_STEPS)
    })
  })

  test('create external program', async ({page, adminPrograms}) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')

    await loginAsAdmin(page)
    const programName = 'Program'

    await test.step("start the creation of a 'default' program", async () => {
      // Start creation of a program, without submission.
      await adminPrograms.addProgram(programName, {
        shortDescription: 'program short description',
        submitNewProgram: false,
      })
      await adminPrograms.expectProgramTypeSelected(ProgramType.DEFAULT)
    })

    await test.step('expect fields for default programs to have the correct enabled/disabled state', async () => {
      // We only verify the fields that are affected by program type. Tests
      // for default programs have more exhaustive coverage.
      await adminPrograms.expectFormFieldEnabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldEnabled(
        FormField.NOTIFICATION_PREFERENCES,
      )
      await adminPrograms.expectFormFieldEnabled(FormField.LONG_DESCRIPTION)
      await adminPrograms.expectFormFieldEnabled(FormField.APPLICATION_STEPS)

      await adminPrograms.expectFormFieldDisabled(
        FormField.PROGRAM_EXTERNAL_LINK,
      )
    })

    await test.step("select 'external' program type", async () => {
      await adminPrograms.selectProgramType(ProgramType.EXTERNAL)
      await adminPrograms.expectProgramTypeSelected(ProgramType.EXTERNAL)
    })

    await test.step('expect fields for external programs to have the correct enabled/disabled state', async () => {
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldDisabled(
        FormField.NOTIFICATION_PREFERENCES,
      )
      await adminPrograms.expectFormFieldDisabled(FormField.LONG_DESCRIPTION)
      await adminPrograms.expectFormFieldDisabled(FormField.APPLICATION_STEPS)
      await adminPrograms.expectFormFieldDisabled(
        FormField.CONFIRMATION_MESSAGE,
      )

      await adminPrograms.expectFormFieldEnabled(
        FormField.PROGRAM_EXTERNAL_LINK,
        ProgramType.EXTERNAL,
      )

      // Changing the program type is allowed during program creation.
      // Therefore, all the program type options should be enabled.
      await adminPrograms.expectProgramTypeEnabled(ProgramType.DEFAULT)
      await adminPrograms.expectProgramTypeEnabled(ProgramType.EXTERNAL)
      await adminPrograms.expectProgramTypeEnabled(ProgramType.PRE_SCREENER)
    })

    await test.step("change program type back to 'default'", async () => {
      await adminPrograms.selectProgramType(ProgramType.DEFAULT)
      await adminPrograms.expectProgramTypeSelected(ProgramType.DEFAULT)
    })

    await test.step('expect fields for default programs to have the correct enabled/disabled state', async () => {
      await adminPrograms.expectFormFieldEnabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldEnabled(
        FormField.NOTIFICATION_PREFERENCES,
      )
      await adminPrograms.expectFormFieldEnabled(FormField.LONG_DESCRIPTION)
      await adminPrograms.expectFormFieldEnabled(FormField.APPLICATION_STEPS)
      await adminPrograms.expectFormFieldEnabled(FormField.CONFIRMATION_MESSAGE)

      await adminPrograms.expectFormFieldDisabled(
        FormField.PROGRAM_EXTERNAL_LINK,
      )
    })

    await test.step('save external program', async () => {
      await adminPrograms.selectProgramType(ProgramType.EXTERNAL)
      await adminPrograms.expectProgramTypeSelected(ProgramType.EXTERNAL)
      await adminPrograms.submitProgramDetailsEdits()
    })

    await test.step('edit external program', async () => {
      await adminPrograms.goToProgramDescriptionPage(programName)
    })

    await test.step('confirm fields for default programs to have the correct enabled/disabled state', async () => {
      await adminPrograms.expectFormFieldDisabled(FormField.PROGRAM_ELIGIBILITY)
      await adminPrograms.expectFormFieldDisabled(
        FormField.NOTIFICATION_PREFERENCES,
      )
      await adminPrograms.expectFormFieldDisabled(FormField.LONG_DESCRIPTION)
      await adminPrograms.expectFormFieldDisabled(FormField.APPLICATION_STEPS)
      await adminPrograms.expectFormFieldDisabled(
        FormField.CONFIRMATION_MESSAGE,
      )

      await adminPrograms.expectFormFieldEnabled(
        FormField.PROGRAM_EXTERNAL_LINK,
        ProgramType.EXTERNAL,
      )

      // Changing the program type of an external program is disallowed
      // after program creation. Therefore, only external program option
      // should be enabled.
      await adminPrograms.expectProgramTypeDisabled(ProgramType.DEFAULT)
      await adminPrograms.expectProgramTypeEnabled(ProgramType.EXTERNAL)
      await adminPrograms.expectProgramTypeDisabled(ProgramType.PRE_SCREENER)
    })
  })

  test('default or pre-screener program cannot be changed to be an external program after creation', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')

    await loginAsAdmin(page)
    const programName = 'External Program'

    await test.step("add a 'default' program", async () => {
      await adminPrograms.addProgram(programName)
    })

    await test.step("'default' program cannot be changed to be an 'external' program", async () => {
      await adminPrograms.goToProgramDescriptionPage(programName)
      await adminPrograms.expectProgramTypeSelected(ProgramType.DEFAULT)

      await adminPrograms.expectProgramTypeEnabled(ProgramType.DEFAULT)
      await adminPrograms.expectProgramTypeDisabled(ProgramType.EXTERNAL)
      await adminPrograms.expectProgramTypeEnabled(ProgramType.PRE_SCREENER)
    })

    await test.step("'pre-screener' program cannot be changed to be an 'external' program", async () => {
      await adminPrograms.selectProgramType(ProgramType.PRE_SCREENER)
      await adminPrograms.expectProgramTypeSelected(ProgramType.PRE_SCREENER)

      await adminPrograms.expectProgramTypeEnabled(ProgramType.DEFAULT)
      await adminPrograms.expectProgramTypeDisabled(ProgramType.EXTERNAL)
      await adminPrograms.expectProgramTypeEnabled(ProgramType.PRE_SCREENER)
    })
  })

  test('when editing a default program, "Manage questions" link is visible', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    await adminPrograms.addProgram('Default Program')
    await adminPrograms.goToProgramDescriptionPage('Default Program')

    await expect(
      page.getByRole('link', {name: 'Manage questions →'}),
    ).toBeVisible()
  })

  test('when editing an external program, "Manage questions" link is hidden', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')
    await loginAsAdmin(page)

    await adminPrograms.addExternalProgram(
      'External Program',
      'short description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
    )
    await adminPrograms.goToProgramDescriptionPage('External Program')

    await expect(
      page.getByRole('link', {name: 'Manage questions →'}),
    ).toBeHidden()
  })
})

async function expectQuestionsOrderWithinBlock(
  page: Page,
  expectedQuestions: string[],
) {
  const actualQuestions = await page
    .locator('.cf-program-question')
    .allTextContents()
  expect(actualQuestions.length).toEqual(expectedQuestions.length)
  for (let i = 0; i < actualQuestions.length; i++) {
    expect(actualQuestions[i]).toContain(expectedQuestions[i])
  }
}
