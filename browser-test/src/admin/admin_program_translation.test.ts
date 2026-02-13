import {test, expect} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  logout,
  loginAsTestUser,
  selectApplicantLanguage,
  validateScreenshot,
  validateToastMessage,
} from '../support'
import {ProgramVisibility} from '../support/admin_programs'
import {FormField} from '../support/admin_translations'

test.describe('Admin can manage program translations', () => {
  test('page layout screenshot', async ({
    page,
    adminPrograms,
    adminTranslations,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Program to be translated no statuses'
    await adminPrograms.addProgram(programName)

    await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)

    await adminTranslations.selectLanguage('Spanish')

    await validateScreenshot(page, 'program-translation')
  })

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
    await adminTranslations.expectProgramTranslation({
      expectProgramName: '',
      expectProgramDescription: '',
      expectProgramShortDescription: '',
      expectApplicationStepTitle: '',
      expectApplicationStepDescription: '',
    })
    await adminTranslations.expectNoProgramStatusTranslations()
    await adminTranslations.expectNoProgramImageDescription()
    const publicName = 'Spanish name'
    const publicDescription = 'Spanish description'
    const publicShortDesc = 'Short Spanish desc'
    const stepOneTitle = 'step one Spanish title'
    const stepOneDescription = 'step one Spanish description'
    await adminTranslations.editProgramTranslations({
      name: publicName,
      description: publicDescription,
      blockName: 'Spanish block name',
      blockDescription: 'Spanish block description',
      statuses: [],
      shortDescription: publicShortDesc,
      applicationStepTitle: stepOneTitle,
      applicationStepDescription: stepOneDescription,
    })
    await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
    await adminTranslations.selectLanguage('Spanish')
    await adminTranslations.expectProgramTranslation({
      expectProgramName: publicName,
      expectProgramDescription: publicDescription,
      expectProgramShortDescription: publicShortDesc,
      expectApplicationStepTitle: stepOneTitle,
      expectApplicationStepDescription: stepOneDescription,
    })
    await adminTranslations.expectNoProgramStatusTranslations()
    await adminTranslations.expectNoProgramImageDescription()
    await adminPrograms.publishProgram(programName)

    // View the applicant program page in Spanish and check that the translations are present
    await logout(page)
    await selectApplicantLanguage(page, 'es-US')
    const cardText = await page.innerText(
      '.cf-application-card:has-text("' + publicName + '")',
    )
    expect(cardText).toContain('Spanish name')
    expect(cardText).toContain('Short Spanish desc')
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
      expectProgramShortDescription: '',
      expectApplicationStepTitle: '',
      expectApplicationStepDescription: '',
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
    const publicShortDescription = 'Short Spanish desc'
    const stepOneTitle = 'step one Spanish title'
    const stepOneDescription = 'step one Spanish description'
    await adminTranslations.editProgramTranslations({
      name: publicName,
      description: publicDescription,
      blockName: 'Spanish block name',
      blockDescription: 'Spanish block description',
      statuses: [],
      shortDescription: publicShortDescription,
      applicationStepTitle: stepOneTitle,
      applicationStepDescription: stepOneDescription,
    })
    await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
    await adminTranslations.selectLanguage('Spanish')
    await adminTranslations.expectProgramTranslation({
      expectProgramName: publicName,
      expectProgramDescription: publicDescription,
      expectProgramShortDescription: publicShortDescription,
      expectApplicationStepTitle: stepOneTitle,
      expectApplicationStepDescription: stepOneDescription,
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
      blockName: 'Spanish block name',
      blockDescription: 'Spanish block description',
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
      shortDescription: publicShortDescription,
      applicationStepTitle: stepOneTitle,
      applicationStepDescription: stepOneDescription,
    })
    await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
    await adminTranslations.selectLanguage('Spanish')
    await adminTranslations.expectProgramTranslation({
      expectProgramName: publicName,
      expectProgramDescription: publicDescription,
      expectProgramShortDescription: publicShortDescription,
      expectApplicationStepTitle: stepOneTitle,
      expectApplicationStepDescription: stepOneDescription,
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

  test('Pre-screener form translations', async ({
    page,
    adminPrograms,
    adminTranslations,
  }) => {
    const programName = 'Pre-screener program'

    await test.step('Add a pre-screener program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addPreScreener(
        programName,
        'short description',
        ProgramVisibility.PUBLIC,
      )
    })

    await test.step('Open translations page and verify fields', async () => {
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')

      await adminTranslations.expectFormFieldVisible(FormField.PROGRAM_NAME)
      await adminTranslations.expectFormFieldVisible(
        FormField.CONFIRMATION_MESSAGE,
      )
      await adminTranslations.expectFormFieldVisible(
        FormField.SHORT_DESCRIPTION,
      )
      await adminTranslations.expectFormFieldVisible(FormField.SCREEN_NAME)
      await adminTranslations.expectFormFieldVisible(
        FormField.SCREEN_DESCRIPTION,
      )

      await adminTranslations.expectFormFieldHidden(
        FormField.PROGRAM_DESCRIPTION,
      )
      await adminTranslations.expectFormFieldHidden(
        FormField.APPLICATION_STEP_ONE_TITLE,
      )
      await adminTranslations.expectFormFieldHidden(
        FormField.APPLICATION_STEP_ONE_DESCRIPTION,
      )
    })

    await test.step('Update translations', async () => {
      await adminTranslations.editProgramTranslations({
        name: 'Spanish name',
        shortDescription: 'Spanish description',
        blockName: 'Spanish block name - bloque uno',
        blockDescription: 'Spanish block description',
        statuses: [],
        programType: 'pre-screener',
      })
    })

    await test.step('Verify translations in translations page', async () => {
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectBlockTranslations(
        'Spanish block name - bloque uno',
        'Spanish block description',
      )

      await adminTranslations.expectProgramTranslation({
        expectProgramName: 'Spanish name',
        expectProgramShortDescription: 'Spanish description',
        programType: 'pre-screener',
      })
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
      shortDescription: 'Spanish description',
      blockName: 'Spanish block name',
      blockDescription: 'Spanish block description',
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
      shortDescription: 'Spanish description',
      blockName: 'Spanish block name',
      blockDescription: 'Spanish block description',
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
      shortDescription: 'Spanish description',
      blockName: 'Spanish block name',
      blockDescription: 'Spanish block description',
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

  test('Add translation for block name, description and eligibility message', async ({
    page,
    adminPrograms,
    adminQuestions,
    adminTranslations,
    adminPredicates,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)

    const questionName = 'eligibility-question-q'
    const eligibilityMsg = 'Cutomized eligibility mesage'
    const programName = 'Program with blocks'
    const screenName = 'Screen 1'

    await test.step('Add program and a screen', async () => {
      await adminQuestions.addTextQuestion({questionName: questionName})
      await adminQuestions.addTextQuestion({
        questionName: 'eligibility-other-q',
        description: 'desc',
        questionText: 'eligibility question',
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: screenName,
        description: 'first screen',
        questions: [{name: questionName}],
      })
    })

    await test.step('Add predicate for the screen', async () => {
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        screenName,
      )
      await adminPredicates.addPredicates({
        questionName: questionName,
        scalar: 'text',
        operator: 'is equal to',
        value: 'eligible',
      })
      await adminPredicates.expectPredicateDisplayTextContains(
        'Applicant is eligible if "eligibility-question-q" text is equal to "eligible"',
      )
    })

    await test.step('Update translations', async () => {
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        screenName,
      )
      await adminPredicates.updateEligibilityMessage(eligibilityMsg)
      await validateToastMessage(page, eligibilityMsg)

      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')

      await adminTranslations.editProgramTranslations({
        name: 'Spanish name',
        shortDescription: 'Spanish description',
        blockName: 'Spanish block name - bloque uno',
        blockDescription: 'Spanish block description',
        statuses: [],
        blockEligibilityMsg: 'Spanish block eligibility message',
      })
    })

    await test.step('Verify translations in translations page', async () => {
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectBlockTranslations(
        'Spanish block name - bloque uno',
        'Spanish block description',
        'Spanish block eligibility message',
      )
    })

    await test.step('Publish and verify the translation on applicant side', async () => {
      await adminPrograms.publishProgram(programName)
      await logout(page)

      await loginAsTestUser(page)
      await selectApplicantLanguage(page, 'es-US')
      await applicantQuestions.applyProgram(
        'Spanish name',
        /* showProgramOverviewPage= */ true,
        /* translatedOverviewTitle= */ 'Descripción general del programa',
        /* translatedLinkText= */ 'Comenzar una solicitud',
      )
      await applicantQuestions.answerTextQuestion('ineligible')
      await page.click('text="Continuar"')
      await expect(page.locator('main')).toContainText(
        'Spanish block eligibility message',
      )
    })

    await test.step('Clear eligibility message', async () => {
      await selectApplicantLanguage(page, 'en-US')
      await logout(page)
      await loginAsAdmin(page)
      await adminPrograms.editProgram(programName)
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        screenName,
      )
      await adminPredicates.updateEligibilityMessage('')
      await validateToastMessage(page, 'Eligibility message removed.')

      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')
      await adminTranslations.expectBlockTranslations(
        'Spanish block name - bloque uno',
        'Spanish block description',
        '',
      )

      await adminPrograms.publishProgram(programName)
      await logout(page)
    })

    await test.step('Verify that eligibility message does not show up on the applicant side', async () => {
      await loginAsTestUser(page)
      await selectApplicantLanguage(page, 'es-US')
      await page.click('text="Editar"')

      await expect(page.locator('main')).not.toContainText(
        'Spanish block eligibility message',
      )
    })
  })

  test('External program translations', async ({
    page,
    adminPrograms,
    adminTranslations,
  }) => {
    await test.step('Add an external program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addExternalProgram(
        'External program',
        'short description',
        'https://usa.gov',
        ProgramVisibility.PUBLIC,
      )
    })

    await test.step('Open translations page and verify fields', async () => {
      await adminPrograms.gotoDraftProgramManageTranslationsPage(
        'External program',
      )
      await adminTranslations.selectLanguage('Spanish')

      await adminTranslations.expectFormFieldVisible(FormField.PROGRAM_NAME)
      await adminTranslations.expectFormFieldVisible(
        FormField.SHORT_DESCRIPTION,
      )

      await adminTranslations.expectFormFieldHidden(
        FormField.PROGRAM_DESCRIPTION,
      )
      await adminTranslations.expectFormFieldHidden(
        FormField.CONFIRMATION_MESSAGE,
      )
      await adminTranslations.expectFormFieldHidden(
        FormField.APPLICATION_STEP_ONE_TITLE,
      )
      await adminTranslations.expectFormFieldHidden(
        FormField.APPLICATION_STEP_ONE_DESCRIPTION,
      )
      await adminTranslations.expectFormFieldHidden(FormField.SCREEN_NAME)
      await adminTranslations.expectFormFieldHidden(
        FormField.SCREEN_DESCRIPTION,
      )

      await validateScreenshot(
        page.locator('#program-translation-form'),
        'external-program-translation',
      )
    })

    // Updating translation is extensively tested in previous tests
  })
})
