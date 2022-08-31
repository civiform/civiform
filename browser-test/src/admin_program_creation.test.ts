import {
  AdminPrograms,
  AdminQuestions,
  createBrowserContext,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'
import {Page} from 'playwright'

describe('program creation', () => {
  const ctx = createBrowserContext()

  it('create program with enumerator and repeated questions', async () => {
    const {page} = ctx
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

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

    const programName = 'apc program'
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
    await page.click('button:text("apc-name")')
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
    await page.click('button:text("apc-enumerator")')
    expect(await page.innerText('id=question-bank-questions')).toBe('')

    // Create a repeated block. The repeated question should be the only option.
    await page.click('#create-repeated-block-button')
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'apc-repeated',
    )
  })

  it('change questions order within block', async () => {
    const {page} = ctx
    page.setDefaultTimeout(4000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    const color = 'favorite-color'
    const movie = 'favorite-movie'
    const song = 'favorite-song'
    for (const question of [movie, color, song]) {
      await adminQuestions.addTextQuestion({questionName: question})
    }

    const programName = 'apc program 2'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'apc program description')

    for (const question of [movie, color, song]) {
      await page.click(`button:text("${question}")`)
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

    // Questions in the question bank use animation. And it causes flakiness
    // as buttons have very brief animation upon initial rendering and it can
    // capturef by screenshot. So delay taking screenshot.
    await page.waitForTimeout(2000)
    await validateScreenshot(page, 'program-creation')
  })

  it('create question from question bank', async () => {
    const {page} = ctx

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)
    const programName = 'apc program 3'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToManageQuestionsPage(programName)
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

    await adminQuestions.expectDraftQuestionExist(questionName, questionText)
    // Ensure the question can be added from the question bank.
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      questionName,
    ])
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
