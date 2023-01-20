import {
  createTestContext,
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
