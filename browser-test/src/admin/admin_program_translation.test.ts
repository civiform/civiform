import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  loginAsTestUser,
  selectApplicantLanguage,
  selectApplicantLanguageNorthstar,
  validateScreenshot,
  validateToastMessage,
  disableFeatureFlag,
} from '../support'
import {ProgramType, ProgramVisibility} from '../support/admin_programs'
import {FormField} from '../support/admin_translations'

test.describe('Admin can manage program translations', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(page, 'north_star_applicant_ui')
  })

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
      await adminPrograms.addProgram(
        programName,
        'description',
        'short description',
        'https://www.example.com',
        ProgramVisibility.PUBLIC,
        'admin description',
        ProgramType.PRE_SCREENER,
      )
    })

    await test.step('Open translations page and verify fields', async () => {
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      await adminTranslations.selectLanguage('Spanish')

      await adminTranslations.expectFormFieldVisible(FormField.PROGRAM_NAME)
      await adminTranslations.expectFormFieldVisible(
        FormField.PROGRAM_DESCRIPTION,
      )
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
        FormField.APPLICATION_STEP_ONE_TITLE,
      )
      await adminTranslations.expectFormFieldHidden(
        FormField.APPLICATION_STEP_ONE_DESCRIPTION,
      )
    })

    await test.step('Update translations', async () => {
      await adminTranslations.editProgramTranslations({
        name: 'Spanish name',
        description: 'Spanish description',
        shortDescription: 'Spanish short description',
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
        expectProgramDescription: 'Spanish description',
        expectProgramShortDescription: 'Spanish short description',
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
      description: 'Spanish description',
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
      description: 'Spanish description',
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
      description: 'Spanish description',
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
        'Screen 1 is eligible if "eligibility-question-q" text is equal to "eligible"',
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
        description: 'Spanish description',
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
      await selectApplicantLanguage(page, 'Español')
      await applicantQuestions.applyProgram('Spanish name')
      await applicantQuestions.answerTextQuestion('ineligible')
      await page.click('text="Guardar y continuar"')
      await validateScreenshot(
        page,
        'ineligible-view-with-translated-eligibility-msg',
      )
    })
  })

  test.describe('North Star translations tests', {tag: ['@northstar']}, () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('Add translations for block name and description', async ({
      page,
      adminPrograms,
      adminQuestions,
      adminTranslations,
      applicantQuestions,
      applicantProgramOverview,
    }) => {
      const programName = 'Program with blocks'

      await test.step('Create program with block', async () => {
        await loginAsAdmin(page)
        await adminQuestions.addTextQuestion({questionName: 'text-question'})
        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockUsingSpec(programName, {
          name: 'Screen 1',
          description: 'first screen',
          questions: [{name: 'text-question'}],
        })
      })

      await test.step('Update translations', async () => {
        await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
        await adminTranslations.selectLanguage('Spanish')
        await adminTranslations.editProgramTranslations({
          name: 'Spanish name',
          description: 'Spanish description',
          blockName: 'Spanish block name - bloque uno',
          blockDescription: 'Spanish block description',
          statuses: [],
        })
      })

      await test.step('Verify translations in translations page', async () => {
        await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
        await adminTranslations.selectLanguage('Spanish')
        await adminTranslations.expectBlockTranslations(
          'Spanish block name - bloque uno',
          'Spanish block description',
        )
      })

      await test.step('Publish and verify in the applicant experience', async () => {
        await adminPrograms.publishProgram(programName)
        await logout(page)
        await selectApplicantLanguageNorthstar(page, 'es-US')
        await applicantQuestions.clickApplyProgramButton('Spanish name')
        await applicantProgramOverview.startApplicationFromTranslatedProgramOverviewPage(
          'Descripción general del programa', // translated page title
          'Inscribirse en el programa Spanish name', // translated page header
          'Comenzar una solicitud', // translated button text
        )

        await expect(
          page.getByText('Spanish block name - bloque uno'),
        ).toBeVisible()
      })
    })

    test('Pre-screener form translations - north star', async ({
      page,
      adminPrograms,
      adminTranslations,
    }) => {
      const programName = 'Pre-screener program'

      await test.step('Add a pre-screener program', async () => {
        await loginAsAdmin(page)
        await adminPrograms.addPreScreenerNS(
          programName,
          'short description',
          ProgramVisibility.PUBLIC,
        )
      })

      await test.step('Open translations page and verify fields', async () => {
        await adminPrograms.gotoDraftProgramManageTranslationsPage(
          'Pre-screener program',
        )
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
          northStar: true,
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
          northStar: true,
        })
      })
    })

    test('External program translations - north star', async ({
      page,
      adminPrograms,
      adminTranslations,
    }) => {
      await enableFeatureFlag(page, 'external_program_cards_enabled')

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
})
