import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  loginAsTestUser,
  validateScreenshot,
  testUserDisplayName,
  loginAsProgramAdmin,
} from '../support'
import {
  ProgramCategories,
  ProgramHeaderButton,
  ProgramVisibility,
} from '../support/admin_programs'

test.describe('admin program view page', () => {
  test('view active program shows read only view', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Active Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoViewActiveProgramPage(programName)
    await validateScreenshot(page, 'program-read-only-view')
  })

  test('view program details shows program categories', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    const programName = 'Active Program'

    await seeding.seedProgramsAndCategories()

    await test.step('login as admin', async () => {
      await page.goto('/')
      await loginAsAdmin(page)
    })

    await test.step('create and publish new program', async () => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.publishAllDrafts()
    })

    await test.step('expect categories to be none on details page', async () => {
      await adminPrograms.gotoViewActiveProgramPage(programName)
      await expect(page.getByText('Categories: None')).toBeVisible()
    })

    await test.step('add two categories', async () => {
      await adminPrograms.selectProgramCategories(
        programName,
        [ProgramCategories.INTERNET, ProgramCategories.EDUCATION],
        /* isActive= */ true,
      )
    })

    await test.step('expect to see the two categories on details page', async () => {
      await expect(
        page.getByText('Categories: Education, Internet'),
      ).toBeVisible()
    })
  })

  test('view draft program', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programName = 'Draft Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoEditDraftProgramPage(programName)

    await validateScreenshot(page, 'program-draft-view')
  })

  test('view program with universal questions', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Program with universal questions'
    await adminQuestions.addTextQuestion({
      questionName: 'nonuniversal-text',
      universal: false,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'universal-text',
      universal: true,
    })
    await adminQuestions.addAddressQuestion({
      questionName: 'universal-address',
      universal: true,
    })

    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      'universal-text',
      'nonuniversal-text',
      'universal-address',
    ])
    await adminPrograms.publishAllDrafts()

    await adminPrograms.gotoViewActiveProgramPage(programName)
    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'universal-text',
      true,
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'nonuniversal-text',
      false,
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'universal-address',
      true,
    )
    await validateScreenshot(page, 'program-view-universal-questions')
  })

  test('view program, view multiple blocks, then start editing with extra long screen name and description', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Apc program'
    await adminQuestions.addAddressQuestion({questionName: 'address-q'})

    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName, 'screen 2 description', [])

    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name:
        'Screen 2 ooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo',
      description:
        'dummy description oooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo',
      questions: [
        {
          name: 'address-q',
        },
      ],
    })

    await adminPrograms.publishAllDrafts()

    await adminPrograms.gotoViewActiveProgramPage(programName)

    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.gotoToBlockInReadOnlyProgram('2')
    await adminPrograms.expectReadOnlyProgramBlock('2')

    await adminPrograms.expectQuestionCardWithLabel(
      'address-q',
      'required question',
    )
    await validateScreenshot(
      page,
      'view-program-block-2-long-screen-name-and-description',
    )

    await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
    await adminPrograms.expectProgramBlockEditPage(programName)

    await validateScreenshot(
      page,
      'view-program-start-editing-extra-long-screen-name-and-description',
    )
  })

  test('view program, view multiple blocks, then start editing', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Apc program'
    await adminQuestions.addAddressQuestion({questionName: 'address-q'})
    await adminQuestions.addDateQuestion({questionName: 'date-q'})
    await adminQuestions.addEmailQuestion({questionName: 'email-q'})

    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName, 'screen 2 description', [])
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      'address-q',
      'date-q',
      'email-q',
    ])
    await adminPrograms.publishAllDrafts()

    await adminPrograms.gotoViewActiveProgramPage(programName)

    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.gotoToBlockInReadOnlyProgram('2')
    await adminPrograms.expectReadOnlyProgramBlock('2')

    await adminPrograms.expectQuestionCardWithLabel(
      'address-q',
      'required question',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      'address-q',
      'address correction: disabled',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      'date-q',
      'required question',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      'email-q',
      'required question',
    )

    await validateScreenshot(page, 'view-program-block-2')

    await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  test('view external program', async ({page, adminPrograms}) => {
    await enableFeatureFlag(page, 'external_program_cards_enabled')

    await loginAsAdmin(page)

    const programName = 'External Program'
    await adminPrograms.addExternalProgram(
      programName,
      /* shortDescription= */ 'short program description',
      /* externalLink= */ 'https://usa.gov',
      /* visibility= */ ProgramVisibility.PUBLIC,
    )

    // On draft mode, external programs should not have preview and download
    // header buttons or the block panel.
    await adminPrograms.gotoEditDraftProgramPage(programName)
    await adminPrograms.expectProgramHeaderButtonHidden(
      ProgramHeaderButton.PREVIEW_AS_APPLICANT,
    )
    await adminPrograms.expectProgramHeaderButtonHidden(
      ProgramHeaderButton.DOWNLOAD_PDF_PREVIEW,
    )
    await adminPrograms.expectBlockPanelHidden()

    // On active mode, external programs should not have preview and download
    // header buttons or the block panel
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoViewActiveProgramPage(programName)
    await adminPrograms.expectProgramHeaderButtonHidden(
      ProgramHeaderButton.PREVIEW_AS_APPLICANT,
    )
    await adminPrograms.expectProgramHeaderButtonHidden(
      ProgramHeaderButton.DOWNLOAD_PDF_PREVIEW,
    )
    await adminPrograms.expectBlockPanelHidden()
    await logout(page)
  })

  test('admin views application with long text answer', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
  }) => {
    const ProgramWithLongText = 'longTextProgram'
    const longParagraph =
      'Fur seals and sea lions make up the family Otariidae. Along with the Phocidae and Odobenidae, ottariids are pinnipeds descending from a common ancestor most closely related to modern bears (as hinted by the subfamily Arctocephalinae, meaning "bear-headed"). The name pinniped refers to mammals with front and rear flippers. Otariids arose about 15-17 million years ago in the Miocene, and were originally land mammals that rapidly diversified and adapted to a marine environment, giving rise to the semiaquatic marine mammals that thrive today. Fur seals and sea lions are closely related and commonly known together as the "eared seals". Until recently, fur seals were all grouped under a single subfamily of Pinnipedia, called the Arctocephalinae, to contrast them with Otariinae – the sea lions – based on the most prominent common feature, namely the coat of dense underfur intermixed with guard hairs. Recent genetic evidence, however, suggests Callorhinus is more closely related to some sea lion species, and the fur seal/sea lion subfamily distinction has been eliminated from many taxonomies. Nonetheless, all fur seals have certain features in common: the fur, generally smaller sizes, farther and longer foraging trips, smaller and more abundant prey items, and greater sexual dimorphism. For these reasons, the distinction remains useful. Fur seals comprise two genera: Callorhinus, and Arctocephalus. Callorhinus is represented by just one species in the Northern Hemisphere, the northern fur seal (Callorhinus ursinus), and Arctocephalus is represented by eight species in the Southern Hemisphere. The southern fur seals comprising the genus Arctocephalus include Antarctic fur seals, Galapagos fur seals, Juan Fernandez fur seals, New Zealand fur seals, brown fur seals, South American fur seals, and subantarctic fur seals.'

    await test.step('create a new program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(ProgramWithLongText)

      await adminQuestions.addTextQuestion({
        questionName: 'long-text',
        questionText: 'long text question',
      })
      await adminQuestions.addAddressQuestion({questionName: 'address-1'})
      await adminQuestions.addDateQuestion({questionName: 'date-2'})
      await adminQuestions.addEmailQuestion({questionName: 'email-3'})

      await adminPrograms.editProgramBlockUsingSpec(ProgramWithLongText, {
        name: 'First Block',
        description: 'First block',
        questions: [{name: 'long-text', isOptional: false}],
      })

      await adminPrograms.editProgramBlockUsingSpec(ProgramWithLongText, {
        name: 'Second Block',
        description: 'Second block',
        questions: [
          {name: 'address-1', isOptional: false},
          {name: 'date-2', isOptional: false},
          {name: 'email-3', isOptional: false},
        ],
      })
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('Applicant applies and answers with long text', async () => {
      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(ProgramWithLongText)
      await applicantQuestions.answerTextQuestion(longParagraph)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
      )
      await applicantQuestions.answerMemorableDateQuestion(
        '2025',
        '05 - May',
        '2',
      )
      await applicantQuestions.answerEmailQuestion('test@example.com')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage()
      await logout(page)
    })

    await test.step('Admin views applications and verifies answer', async () => {
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(ProgramWithLongText)
      await adminPrograms.viewApplicationForApplicant(testUserDisplayName())

      await validateScreenshot(page, 'admin-view-application-long-text-answer')
    })
  })
})
