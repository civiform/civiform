import {
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {Page} from 'playwright'

describe('program creation', () => {
  const ctx = createTestContext()

  it('program details page screenshot', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)
    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramDescriptionPage(programName)
    await validateScreenshot(page, 'program-description-page')
  })

  it('shows correct formatting during question creation', async () => {
    const {page, adminQuestions} = ctx

    await loginAsAdmin(page)

    await adminQuestions.createStaticQuestion({
      questionName: 'static-question',
      questionText:
        '### This is an example of some static text with formatting',
    })

    await validateScreenshot(
      page,
      'program-creation-static-question-with-formatting',
    )
  })

  it('create program with enumerator and repeated questions', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

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
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'apc-address',
    )
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'apc-name',
    )
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'apc-text',
    )
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'apc-enumerator',
    )
    expect(await page.innerText('id=question-bank-questions')).not.toContain(
      'apc-repeated',
    )

    // Add a non-enumerator question and the enumerator option should go away
    await adminPrograms.addQuestionFromQuestionBank('apc-name')
    expect(await page.innerText('id=question-bank-questions')).not.toContain(
      'apc-enumerator',
    )
    expect(await page.innerText('id=question-bank-questions')).not.toContain(
      'apc-repeated',
    )

    // Remove the non-enumerator question and add a enumerator question. All options should go away.
    await page.click(
      '.cf-program-question:has-text("apc-name") >> .cf-remove-question-button',
    )
    await adminPrograms.addQuestionFromQuestionBank('apc-enumerator')
    expect(await page.innerText('id=question-bank-questions')).toBe('')

    // Create a repeated block. The repeated question should be the only option.
    await page.click('#create-repeated-block-button')
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'apc-repeated',
    )
  })

  it('create program with address and address correction feature enabled', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

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

    // the input value shows what it will be set to when clicked
    expect(await addressCorrectionInput.inputValue()).toBe('true')

    await adminPrograms.clickAddressCorrectionToggle()

    expect(await addressCorrectionInput.inputValue()).toBe('false')

    await validateScreenshot(
      page,
      'program-detail-page-with-address-correction-true',
    )

    // ensure that non address question does not contain address correction button
    expect(
      await page.innerText(
        adminPrograms.questionCardSelectorInProgramEditor('ace-name'),
      ),
    ).not.toContain('Address correction')
  })

  it('create program with multiple address questions, address correction feature enabled, and can only enable correction on one address', async () => {
    const {page, adminQuestions, adminPrograms} = ctx
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

    // the input value shows what it will be set to when clicked, so the
    // opposite of its current value
    expect(await addressCorrectionInput1.inputValue()).toBe('true')
    expect(await addressCorrectionInput2.inputValue()).toBe('true')

    expect(await addressCorrectionHelpText1.innerText()).not.toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).not.toContain(helpText)

    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-one')

    expect(await addressCorrectionInput1.inputValue()).toBe('false')
    expect(await addressCorrectionInput2.inputValue()).toBe('true')
    expect(await addressCorrectionHelpText1.innerText()).not.toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).toContain(helpText)

    await validateScreenshot(
      page,
      'program-detail-page-with-first-address-correction-true',
    )

    // Trying to toggle the other one should not do anything
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-two')

    expect(await addressCorrectionInput1.inputValue()).toBe('false')
    expect(await addressCorrectionInput2.inputValue()).toBe('true')
    expect(await addressCorrectionHelpText1.innerText()).not.toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).toContain(helpText)

    await validateScreenshot(
      page,
      'program-detail-page-with-first-address-correction-true',
    )

    // Once we untoggle the first one, we should be able to toggle the second one
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-one')
    await adminPrograms.clickAddressCorrectionToggleByName('ace-address-two')

    expect(await addressCorrectionInput1.inputValue()).toBe('true')
    expect(await addressCorrectionInput2.inputValue()).toBe('false')
    expect(await addressCorrectionHelpText1.innerText()).toContain(helpText)
    expect(await addressCorrectionHelpText2.innerText()).not.toContain(helpText)

    await validateScreenshot(
      page,
      'program-detail-page-with-second-address-correction-true',
    )

    // ensure that non address question does not contain address correction button
    expect(
      await page.innerText(
        adminPrograms.questionCardSelectorInProgramEditor('ace-name'),
      ),
    ).not.toContain('Address correction')
  })

  it('create program with address and address correction feature disabled', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

    await loginAsAdmin(page)

    await adminQuestions.addAddressQuestion({questionName: 'acd-address'})

    const programName = 'Acd program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'acd program description')

    await adminPrograms.addQuestionFromQuestionBank('acd-address')

    const addressCorrectionInput = adminPrograms.getAddressCorrectionToggle()

    // the input value shows what it will be set to when clicked
    expect(await addressCorrectionInput.inputValue()).toBe('true')

    await adminPrograms.clickAddressCorrectionToggle()
    // should be the same as before with button submit disabled
    expect(await addressCorrectionInput.inputValue()).toBe('true')
  })

  it('change questions order within block', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

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

  it('create question from question bank', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

    await loginAsAdmin(page)
    const programName = 'Apc program 3'
    await adminPrograms.addProgram(programName)
    await adminPrograms.openQuestionBank()
    await validateScreenshot(page, 'question-bank-empty')
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
    await validateScreenshot(page, 'question-bank-with-created-question')

    await adminPrograms.expectSuccessToast(`question ${questionName} created`)
    await adminPrograms.expectProgramBlockEditPage(programName)

    await adminQuestions.expectDraftQuestionExist(questionName, questionText)
    // Ensure the question can be added from the question bank.
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      questionName,
    ])
  })

  it('filter questions in question bank', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: 'q-f',
      questionText: 'first question',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'q-s',
      questionText: 'second question',
    })

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName)
    await adminPrograms.openQuestionBank()
    expect(await adminPrograms.questionBankNames()).toEqual([
      'second question',
      'first question',
    ])
    await page.locator('#question-bank-filter').fill('fi')
    expect(await adminPrograms.questionBankNames()).toEqual(['first question'])
    await page.locator('#question-bank-filter').fill('se')
    expect(await adminPrograms.questionBankNames()).toEqual(['second question'])
    await page.locator('#question-bank-filter').fill('')
    expect(await adminPrograms.questionBankNames()).toEqual([
      'second question',
      'first question',
    ])
  })

  /**
   * There was a bug where if you deleted the first block
   * and then go to edit the program, it would error
   * because it would go to the block with ID == 1
   */
  it('delete first block and edit', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)

    const programName = 'Test program 5'
    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName)
    await adminPrograms.removeProgramBlock(programName, 'Screen 1')
    await adminPrograms.goToManageQuestionsPage(programName)
  })

  it('delete last block and edit', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)

    const programName = 'Test program 6'
    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName)
    await adminPrograms.removeProgramBlock(programName, 'Screen 2')
    await adminPrograms.goToManageQuestionsPage(programName)
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
})
