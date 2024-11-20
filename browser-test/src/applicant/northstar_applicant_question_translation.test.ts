import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  selectApplicantLanguage,
  validateScreenshot,
} from '../support'

test.describe('Admin can manage translations', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('Expect single-answer question is translated for applicant', async ({
    page,
    adminPrograms,
    adminQuestions,
    adminTranslations,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)

    // Add a new question to be translated
    const questionName = 'name-translated'
    await adminQuestions.addNameQuestion({questionName})

    // Go to the question translation page and add a translation for Spanish
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')
    await adminTranslations.editQuestionTranslations(
      'ingrese\n1.nombre\n2.segundo nombre\n3.apellido',
      'Spanish help text',
    )

    // Add the question to a program and publish
    const programName = 'Spanish question program'
    await adminPrograms.addProgram(
      programName,
      'program description',
      'http://seattle.gov',
    )
    await adminPrograms.editProgramBlock(programName, 'block', [questionName])
    await adminPrograms.publishProgram(programName)
    await logout(page)

    // Go to the home page and select Spanish as the language
    await selectApplicantLanguage(page, 'Español')
    await applicantQuestions.validateHeader('es-US')

    await applicantQuestions.applyProgram(
      programName,
      /* northStarEnabled= */ true,
    )

    // TODO(#9203): When the bug is fixed, we don't need to select Español again.
    await selectApplicantLanguage(page, 'Español')
    await applicantQuestions.validateHeader('es-US')

    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      'ingrese\n1.nombre\n2.segundo nombre\n3.apellido',
    )
    expect(await page.innerText('.cf-applicant-question-help-text')).toContain(
      'Spanish help text',
    )
  })

  test('Expect single-answer question is translated with markdown for applicant', async ({
    page,
    adminPrograms,
    adminQuestions,
    adminTranslations,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)

    // Add a new question to be translated
    const questionName = 'name-translated'
    await adminQuestions.addNameQuestion({questionName})

    // Go to the question translation page and add a translation for Spanish
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')
    await adminTranslations.editQuestionTranslations(
      '# Introducir nombre  \n * Tu nombre de pila \n *  Tu segundo nombre \n * Tu apellido',
      '## It will identify you',
    )
    // Add the question to a program and publish
    const programName = 'Spanish question program with markdown'
    await adminPrograms.addProgram(
      programName,
      'program description',
      'http://seattle.gov',
    )
    await adminPrograms.editProgramBlock(programName, 'block', [questionName])
    await adminPrograms.publishProgram(programName)
    await logout(page)

    // Log in as an applicant and view the translated question
    await applicantQuestions.applyProgram(
      programName,
      /* northStarEnabled= */ true,
    )
    await selectApplicantLanguage(page, 'Español')
    await applicantQuestions.validateHeader('es-US')

    await validateScreenshot(
      page.locator('.cf-applicant-question-text'),
      'question-translation-with-markdown',
      /* fullPage= */ false,
    )
  })

  test('Expect multi-option question is translated for applicant', async ({
    page,
    adminPrograms,
    adminQuestions,
    adminTranslations,
    applicantQuestions,
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
    await adminTranslations.editQuestionTranslations('hola', 'mundo', [
      'uno',
      'dos',
      'tres',
    ])

    // Add the question to a program and publish
    const programName = 'Spanish question multi option program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'block', [questionName])
    await adminPrograms.publishProgram(programName)
    await logout(page)

    // Log in as an applicant and view the translated question
    await applicantQuestions.applyProgram(
      programName,
      /* northStarEnabled= */ true,
    )
    await selectApplicantLanguage(page, 'Español')

    expect(await page.innerText('main form')).toContain('uno')
    expect(await page.innerText('main form')).toContain('dos')
    expect(await page.innerText('main form')).toContain('tres')
  })

  test('Expect enumerator question is translated for applicant', async ({
    page,
    adminPrograms,
    adminQuestions,
    adminTranslations,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)

    // Add a new question to be translated
    const questionName = 'enumerator-translated'
    await adminQuestions.addEnumeratorQuestion({questionName})

    // Go to the question translation page and add a translation for Spanish
    await adminQuestions.goToQuestionTranslationPage(questionName)
    await adminTranslations.selectLanguage('Spanish')
    await adminTranslations.editQuestionTranslations('test', 'enumerator', [
      'miembro de la familia',
    ])

    // Add the question to a program and publish
    const programName = 'Spanish question enumerator program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'block', [questionName])
    await adminPrograms.publishProgram(programName)
    await logout(page)

    // Log in as an applicant and view the translated question
    await applicantQuestions.applyProgram(
      programName,
      /* northStarEnabled= */ true,
    )
    await selectApplicantLanguage(page, 'Español')

    expect(await page.innerText('main form')).toContain('miembro de la familia')
  })
})
