import {test, expect} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'
import {Eligibility, ProgramVisibility} from '../support/admin_programs'
import {dismissModal, waitForAnyModal} from '../support/wait'
import {Page} from 'playwright'

test.describe('program creation', () => {
  test('create program page', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(
      programName,
      'description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ false,
      'selectedTI',
      'confirmationMessage',
      Eligibility.IS_GATING,
      /* submitNewProgram= */ false,
    )
    await adminPrograms.expectProgramDetailsSaveAndContinueButton()
    await validateScreenshot(page, 'program-creation-page')

    // When the program submission goes through,
    // verify we're redirected to the program image upload page.
    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()
  })

  test('create program with disabled visibility condition feature enabled', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await enableFeatureFlag(page, 'disabled_visibility_condition_enabled')
    await loginAsAdmin(page)

    await adminPrograms.addProgram(
      'program name',
      'description',
      'https://usa.gov',
      ProgramVisibility.DISABLED,
      'admin description',
      /* isCommonIntake= */ false,
      'selectedTI',
      'confirmationMessage',
      Eligibility.IS_GATING,
      /* submitNewProgram= */ false,
    )
    await adminPrograms.expectProgramDetailsSaveAndContinueButton()
    expect(await page.innerText('id=program-details-form')).toContain(
      'Disabled',
    )
    await validateScreenshot(
      page,
      'program-creation-page-disabled-visibility-enabled',
    )

    // When the program submission goes through,
    // verify we're redirected to the program image upload page.
    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()
  })

  test('create program with disabled visibility condition feature disabled', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await disableFeatureFlag(page, 'disabled_visibility_condition_enabled')
    await loginAsAdmin(page)

    await adminPrograms.addProgram(
      'program name',
      'description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ false,
      'selectedTI',
      'confirmationMessage',
      Eligibility.IS_GATING,
      /* submitNewProgram= */ false,
    )
    await adminPrograms.expectProgramDetailsSaveAndContinueButton()
    expect(await page.innerText('id=program-details-form')).not.toContain(
      'Disabled',
    )
    await validateScreenshot(
      page,
      'program-creation-page-disabled-visibility-disabled',
    )

    // When the program submission goes through,
    // verify we're redirected to the program image upload page.
    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()
  })

  test('create program then go back prevents URL edits', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(
      programName,
      'description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ false,
      'selectedTI',
      'confirmationMessage',
      Eligibility.IS_GATING,
      /* submitNewProgram= */ false,
    )

    // On initial program creation, expect an admin can fill in the program name.
    expect(await page.locator('#program-name-input').count()).toEqual(1)

    await adminPrograms.submitProgramDetailsEdits()
    await adminProgramImage.expectProgramImagePage()

    // WHEN the admin goes back to the program details page
    await adminProgramImage.clickBackButton()

    // THEN they should not be able to modify the program name (used for the URL).
    await adminPrograms.expectProgramEditPage(programName)
    expect(await page.locator('#program-name-input').count()).toEqual(0)
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

  test('program details page screenshot', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)
    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)
    await validateScreenshot(page, 'program-description-page')
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

    await page.waitForTimeout(100) // ms
    const previewLocator = page.locator('#sample-question')
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

    await page.waitForTimeout(100) // ms
    const previewLocator = page.locator('#sample-question')
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
      page,
      'open-question-search',
      /* fullPage= */ false,
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
      page,
      'program-detail-page-with-address-correction-false',
    )

    const addressCorrectionInput = adminPrograms.getAddressCorrectionToggle()

    await expect(addressCorrectionInput).toHaveValue('false')

    await adminPrograms.clickAddressCorrectionToggle()

    await expect(addressCorrectionInput).toHaveValue('true')

    await validateScreenshot(
      page,
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
      page,
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
      page,
      'program-detail-page-with-first-address-correction-true',
    )

    // Trying to toggle the other one should not do anything
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-two')

    await expect(addressCorrectionInput1).toHaveValue('true')
    await expect(addressCorrectionInput2).toHaveValue('false')
    expect(await addressCorrectionHelpText1.innerText()).not.toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).toContain(helpText)

    await validateScreenshot(
      page,
      'program-detail-page-with-first-address-correction-true',
    )

    // Once we untoggle the first one, we should be able to toggle the second one
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-one')
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-two')

    await expect(addressCorrectionInput1).toHaveValue('false')
    await expect(addressCorrectionInput2).toHaveValue('true')
    expect(await addressCorrectionHelpText1.innerText()).toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).not.toContain(helpText)

    await validateScreenshot(
      page,
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
    await validateScreenshot(page, 'question-bank-empty', /* fullPage= */ false)
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
      page,
      'question-bank-with-created-question',
      /* fullPage= */ false,
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

    await adminPrograms.launchDeleteScreenModal()
    await validateScreenshot(
      page,
      'delete-screen-confirmation-modal',
      /* fullPage= */ false,
    )
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

  test('create common intake form with intake form feature enabled', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'intake_form_enabled')

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)

    await validateScreenshot(
      page,
      'program-description-page-with-intake-form-false',
    )
    const commonIntakeFormInput = adminPrograms.getCommonIntakeFormToggle()
    await expect(commonIntakeFormInput).not.toBeChecked()

    await adminPrograms.clickCommonIntakeFormToggle()
    await validateScreenshot(
      page,
      'program-description-page-with-intake-form-true',
    )
    await expect(commonIntakeFormInput).toBeChecked()
    await adminPrograms.submitProgramDetailsEdits()
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  test('correctly renders common intake form change confirmation modal', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'intake_form_enabled')
    await loginAsAdmin(page)

    const commonIntakeFormProgramName = 'Benefits finder'
    await adminPrograms.addProgram(
      commonIntakeFormProgramName,
      'program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ true,
    )

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)

    await adminPrograms.goToProgramDescriptionPage(programName)
    await adminPrograms.clickCommonIntakeFormToggle()
    await page.fill('#program-external-link-input', 'badlink')
    await page.click('#program-update-button')

    // Error messages get displayed before the confirmation modal.
    const toastMessages = await page.innerText('#toast-container')
    expect(toastMessages).toContain('program link')

    await page.fill('#program-external-link-input', 'https://example.com')
    await page.click('#program-update-button')

    let modal = await waitForAnyModal(page)
    expect(await modal.innerText()).toContain(`Confirm pre-screener change?`)
    await validateScreenshot(
      page,
      'confirm-common-intake-change-modal',
      /* fullPage= */ false,
    )

    // Modal gets re-rendered if needed.
    await dismissModal(page)
    await page.click('#program-update-button')
    modal = await waitForAnyModal(page)
    expect(await modal.innerText()).toContain(`Confirm pre-screener change?`)

    await page.click('#confirm-common-intake-change-button')
    await waitForPageJsLoad(page)
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  test('regular program has eligibility conditions', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsAdmin(page)

    await adminPrograms.addProgram(
      'cif',
      'desc',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ false,
    )

    await adminPrograms.gotoEditDraftProgramPage('cif')
    expect(await page.innerText('main')).toContain('Eligibility')
  })

  test('common intake form does not have eligibility conditions', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsAdmin(page)

    await adminPrograms.addProgram(
      'cif',
      'desc',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ true,
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
})
