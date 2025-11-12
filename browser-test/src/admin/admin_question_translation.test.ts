import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

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

  // map questions use mock-web-services, which is only available in local environments
  if (isLocalDevEnvironment()) {
    test('create a map question and add translations for settings', async ({
      page,
      adminQuestions,
      adminTranslations,
    }) => {
      const questionName = 'map-setting-translated'

      await test.step('Add a new question to be translated', async () => {
        await loginAsAdmin(page)
        await adminQuestions.addMapQuestion({
          questionName,
          filters: [{key: 'type', displayName: 'Type'}],
        })
      })

      await test.step('Go to the question translation page and add a translation for Spanish', async () => {
        await adminQuestions.goToQuestionTranslationPage(questionName)
        await adminTranslations.selectLanguage('Spanish')

        await validateScreenshot(page, 'map-question-translation')

        await page
          .getByLabel('Question text')
          .fill('texto de la pregunta del mapa')
        await page
          .getByLabel('Question help text')
          .fill('Texto de ayuda para la pregunta del mapa')
        await page.getByLabel('Filter display name').fill('Tipo')
        await page.getByText('Save Spanish updates').click()
        await waitForPageJsLoad(page)
      })

      await test.step('Verify that the translation was saved', async () => {
        await adminQuestions.gotoAdminQuestionsPage()
        await adminQuestions.updateQuestion(questionName)
        await adminQuestions.goToQuestionTranslationPage(questionName)
        await adminTranslations.selectLanguage('Spanish')

        await expect(page.getByLabel('Filter display name')).toHaveValue('Tipo')
      })
    })
  }

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
    await expect(page.getByRole('textbox', {name: 'Question text'})).toHaveText(
      'something different',
    )
    await expect(
      page.getByRole('textbox', {name: 'Question help text'}),
    ).toHaveText('')
  })

  test('admin can add translation when the question is in active mode', async ({
    page,
    adminQuestions,
    adminPrograms,
    adminTranslations,
  }) => {
    await enableFeatureFlag(page, 'translation_management_improvement_enabled')
    await loginAsAdmin(page)
    const questionName = 'name-translated'

    await test.step('create a question in active mode', async () => {
      await adminQuestions.addNameQuestion({questionName})
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllDrafts()
      await adminQuestions.expectActiveQuestionExist(questionName)
    })

    await test.step('verify that admin can add translation for the question in active mode', async () => {
      await adminQuestions.goToQuestionTranslationPage(questionName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.editQuestionTranslations(
        'something different',
        'help text different',
      )

      await adminQuestions.goToQuestionTranslationPage(questionName)
      await adminTranslations.selectLanguage('Spanish')
      await expect(
        page.getByRole('textbox', {name: 'Question text'}),
      ).toHaveText('something different')
      await expect(
        page.getByRole('textbox', {name: 'Question help text'}),
      ).toHaveText('help text different')
    })

    await test.step('verify that admin cannot add translation to the question in active mode when existing a draft', async () => {
      await adminQuestions.updateQuestion(questionName)
      await validateScreenshot(
        page.locator('.cf-admin-question-table-row'),
        'drop-down-for-active-question-hidden',
      )
    })
  })
})
