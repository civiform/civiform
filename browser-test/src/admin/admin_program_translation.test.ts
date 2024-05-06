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
  {tag: ['@uses-fixtures']},
  () => {
    test('creates a program without statuses and adds translation', async ({
      page,
      adminPrograms,
      adminTranslations,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Program to be translated no statuses'
      await adminPrograms.addProgram(programName)

      // Go to manage translations page.
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)

      // Add translations for Spanish and publish
      await adminTranslations.selectLanguage('Spanish')
      await validateScreenshot(page, 'program-translation')
      await adminTranslations.expectProgramTranslation({
        expectProgramName: '',
        expectProgramDescription: '',
      })
      await adminTranslations.expectNoProgramStatusTranslations()
      await adminTranslations.expectNoProgramImageDescription()
      const publicName = 'Spanish name'
      const publicDescription = 'Spanish description'
      await adminTranslations.editProgramTranslations({
        name: publicName,
        description: publicDescription,
        statuses: [],
      })
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectProgramTranslation({
        expectProgramName: publicName,
        expectProgramDescription: publicDescription,
      })
      await adminTranslations.expectNoProgramStatusTranslations()
      await adminTranslations.expectNoProgramImageDescription()
      await adminPrograms.publishProgram(programName)

      // View the applicant program page in Spanish and check that the translations are present
      await logout(page)
      await selectApplicantLanguage(page, 'Español')
      const cardText = await page.innerText(
        '.cf-application-card:has-text("' + publicName + '")',
      )
      expect(cardText).toContain('Spanish name')
      expect(cardText).toContain('Spanish description')
    })

    test('creates a program with statuses and adds translations for program statuses', async ({
      page,
      adminPrograms,
      adminProgramStatuses,
      adminTranslations,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Program to be translated with statuses'
      await adminPrograms.addProgram(programName)

      // Add two statuses, one with a configured email and another without
      const statusWithEmailName = 'status-with-email'
      const statusWithNoEmailName = 'status-with-no-email'
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus(statusWithEmailName, {
        emailBody: 'An email',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus(statusWithNoEmailName)
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)

      // Add only program translations for Spanish. Empty status translations should be accepted.
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectProgramTranslation({
        expectProgramName: '',
        expectProgramDescription: '',
      })
      await adminTranslations.expectProgramStatusTranslationWithEmail({
        configuredStatusText: statusWithEmailName,
        expectStatusText: '',
        expectStatusEmail: '',
      })
      await adminTranslations.expectProgramStatusTranslationWithNoEmail({
        configuredStatusText: statusWithNoEmailName,
        expectStatusText: '',
      })
      const publicName = 'Spanish name'
      const publicDescription = 'Spanish description'
      await adminTranslations.editProgramTranslations({
        name: publicName,
        description: publicDescription,
        statuses: [],
      })
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectProgramTranslation({
        expectProgramName: publicName,
        expectProgramDescription: publicDescription,
      })
      await adminTranslations.expectProgramStatusTranslationWithEmail({
        configuredStatusText: statusWithEmailName,
        expectStatusText: '',
        expectStatusEmail: '',
      })
      await adminTranslations.expectProgramStatusTranslationWithNoEmail({
        configuredStatusText: statusWithNoEmailName,
        expectStatusText: '',
      })

      // Now add a partial translation for one status and a full translation for the other.
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.editProgramTranslations({
        name: publicName,
        description: publicDescription,
        statuses: [
          {
            configuredStatusText: statusWithEmailName,
            statusText: '',
            statusEmail: `${statusWithEmailName}-email-spanish`,
          },
          {
            configuredStatusText: statusWithNoEmailName,
            statusText: `${statusWithNoEmailName}-spanish`,
          },
        ],
      })
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectProgramTranslation({
        expectProgramName: publicName,
        expectProgramDescription: publicDescription,
      })
      await adminTranslations.expectProgramStatusTranslationWithEmail({
        configuredStatusText: statusWithEmailName,
        expectStatusText: '',
        expectStatusEmail: `${statusWithEmailName}-email-spanish`,
      })
      await adminTranslations.expectProgramStatusTranslationWithNoEmail({
        configuredStatusText: statusWithNoEmailName,
        expectStatusText: `${statusWithNoEmailName}-spanish`,
      })
    })

    test('creates a program with summary image description and adds translations', async ({
      page,
      adminPrograms,
      adminProgramImage,
      adminTranslations,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Program with summary image description'
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectProgramImageDescriptionTranslation('')

      // Add a Spanish translation
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.editProgramTranslations({
        name: 'Spanish name',
        description: 'Spanish description',
        statuses: [],
      })
      await adminTranslations.editProgramImageDescription(
        'Spanish image description',
      )

      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectProgramImageDescriptionTranslation(
        'Spanish image description',
      )
      await validateScreenshot(
        page,
        'program-translation-with-image-description',
      )

      // Verify other translations are still not filled in
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Tagalog')
      await adminTranslations.expectProgramImageDescriptionTranslation('')
    })

    test('editing summary image description does not clobber translations', async ({
      page,
      adminPrograms,
      adminProgramImage,
      adminTranslations,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Program with summary image description'
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.editProgramTranslations({
        name: 'Spanish name',
        description: 'Spanish description',
        statuses: [],
      })
      await adminTranslations.editProgramImageDescription(
        'Spanish image description',
      )

      // Update the original description
      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageDescriptionAndSubmit(
        'New image description',
      )

      // Verify the Spanish translations are still there
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectProgramImageDescriptionTranslation(
        'Spanish image description',
      )
    })

    test('deleting summary image description deletes all translations', async ({
      page,
      adminPrograms,
      adminProgramImage,
      adminTranslations,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Program with summary image description'
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageDescriptionAndSubmit(
        'Fake image description',
      )

      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.editProgramTranslations({
        name: 'Spanish name',
        description: 'Spanish description',
        statuses: [],
      })
      await adminTranslations.editProgramImageDescription(
        'Spanish image description',
      )

      // Remove the original description
      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageDescriptionAndSubmit('')

      // Verify there's no longer an option to translate the description.
      // (The image description translation field will only appear in the UI if
      // an original description exists.)
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectNoProgramImageDescription()
    })

    test('creates a question and adds translations', async ({
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
      await validateScreenshot(page, 'question-translation')
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

    test('create a multi-option question and add translations for options', async ({
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
      await validateScreenshot(page, 'multi-option-question-translation')
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

    test('create an enumerator question and add translations for entity type', async ({
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
        'family member',
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

      expect(await page.innerText('main form')).toContain('family member')
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
      await adminQuestions.changeQuestionHelpText(
        questionName,
        'a new help text',
      )

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
