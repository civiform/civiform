import {test, expect} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'

test.describe('Admin can manage question translations', () => {
  test('creates a question and adds translations', async ({
    page,
    adminQuestions,
    adminTranslations,
  }) => {
    await loginAsAdmin(page)

    // Add a new question to be translated
    const questionName = 'name-translated'
    await adminQuestions.addNameQuestion({questionName})

    // Go to the question translation page and add a translation for Spanish
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')

    await validateScreenshot(page, 'question-translation')
  })

  test('create a multi-option question and add translations for options', async ({
    page,
    adminQuestions,
    adminTranslations,
  }) => {
    await loginAsAdmin(page)

    // Add a new question to be translated
    const questionName = 'multi-option-translated'
    await adminQuestions.addRadioButtonQuestion({
      questionName,
      options: [
        {adminName: 'one_admin', text: 'one'},
        {adminName: 'two_admin', text: 'two'},
        {adminName: 'three_admin', text: 'three'},
      ],
    })

    // Go to the question translation page and add a translation for Spanish
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')

    await validateScreenshot(page, 'multi-option-question-translation')
  })

  test('updating a question does not clobber translations', async ({
    page,
    adminQuestions,
    adminTranslations,
  }) => {
    await loginAsAdmin(page)

    // Add a new question.
    const questionName = 'translate-no-clobber'
    await adminQuestions.addNumberQuestion({questionName})

    // Add a translation for a non-English language.
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')
    await adminTranslations.editQuestionTranslations(
      'something different',
      'help text different',
    )

    // Edit the question again and update the question.
    await adminQuestions.updateQuestion(questionName)

    // View the question translations and check that the Spanish translations are still there.
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')
    expect(await page.inputValue('text=Question text')).toContain(
      'something different',
    )
  })

  test('deleting help text in question edit view deletes all help text translations', async ({
    page,
    adminQuestions,
    adminTranslations,
  }) => {
    await loginAsAdmin(page)

    // Add a new question with help text
    const questionName = 'translate-help-text-deletion'
    await adminQuestions.addNumberQuestion({questionName})

    // Add a translation for a non-English language.
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')
    await adminTranslations.editQuestionTranslations(
      'something different',
      'help text different',
    )

    // Edit the question and delete the help text.
    await adminQuestions.changeQuestionHelpText(questionName, '')

    // Edit the question and add help text back
    await adminQuestions.changeQuestionHelpText(questionName, 'a new help text')

    // View the question translations and check that the Spanish translations for question help text are gone.
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')
    expect(await page.inputValue('text=Question text')).toContain(
      'something different',
    )

    // Fix me! ESLint: playwright/prefer-web-first-assertions
    // Directly switching to the best practice method fails
    // because of a locator stict mode violation. That is it
    // returns multiple elements.
    //
    // Recommended prefer-web-first-assertions fix:
    //   await expect(page.locator('text=Question help text')).toHaveText('')
    expect(await page.inputValue('text=Question help text')).toEqual('')
  })
})
