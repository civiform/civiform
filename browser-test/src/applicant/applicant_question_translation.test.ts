import {test, expect} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  logout,
  selectApplicantLanguage,
  validateScreenshot,
  validateToastMessage,
} from '../support'

test.describe(
  'Admin can manage translations',
  () => {
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
        'Spanish question text',
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

      // Log in as an applicant and view the translated question
      await selectApplicantLanguage(page, 'Español')
      await applicantQuestions.validateHeader('es-US')

      // Expect program details link to contain 'Detalles del programa' with link to 'http://seattle.gov'
      expect(
        await page.innerText(
          '.cf-application-card a[href="http://seattle.gov"]',
        ),
      ).toContain('Detalles del programa')

      await applicantQuestions.applyProgram(programName)

      expect(await page.innerText('.cf-applicant-question-text')).toContain(
        'Spanish question text',
      )
      expect(
        await page.innerText('.cf-applicant-question-help-text'),
      ).toContain('Spanish help text')
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
      await selectApplicantLanguage(page, 'Español')
      await applicantQuestions.applyProgram(programName)

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
      await selectApplicantLanguage(page, 'Español')
      await applicantQuestions.applyProgram(programName)

      expect(await page.innerText('main form')).toContain(
        'miembro de la familia',
      )
    })

    test('Applicant sees toast message warning translation is not complete', async ({
      page,
      adminQuestions,
      adminPrograms,
      applicantQuestions,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Toast program'
      await adminPrograms.addProgram(programName)

      await adminQuestions.addNameQuestion({questionName: 'name-english'})
      await adminPrograms.editProgramBlock(programName, 'not translated', [
        'name-english',
      ])

      await adminPrograms.publishProgram(programName)
      await logout(page)

      // Set applicant preferred language to Spanish
      // DO NOT LOG IN AS TEST USER. We want a fresh guest so we can guarantee
      // the language has not yet been set.
      await selectApplicantLanguage(page, 'Español')
      await applicantQuestions.applyProgram(programName)

      // Check that a toast appears warning the program is not fully translated
      await validateToastMessage(
        page,
        'Lo sentimos, este programa no está traducido por completo a tu idioma preferido.',
      )

      await validateScreenshot(page, 'applicant-toast-error')
    })
  },
)
