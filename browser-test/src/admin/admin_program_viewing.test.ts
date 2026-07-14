import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  loginAsTestUser,
  validateScreenshot,
  loginAsProgramAdmin,
} from '../support'
import {
  ProgramCategories,
  ProgramHeaderButton,
  ProgramVisibility,
} from '../support/admin_programs'
import {SAMPLE_PROGRAMS, SAMPLE_QUESTIONS} from '../support/seeding'

test.describe('admin program view page', () => {
  test('view active program shows read only view', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await seeding.seedProgramsAndCategories()
    await loginAsAdmin(page)

    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoViewActiveProgramPage(SAMPLE_PROGRAMS.minimal)
    await validateScreenshot(page, 'program-read-only-view')
    // After publishing, editing is not allowed and text mentions draft mode
    await expect(page.locator('#eligibility-predicate')).toContainText(
      'You can change this in the program settings if your program is in draft mode',
    )
  })

  test('view program details shows program categories', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await seeding.seedProgramsAndCategories()

    await test.step('login as admin', async () => {
      await page.goto('/')
      await loginAsAdmin(page)
    })

    await test.step('publish seeded programs', async () => {
      await adminPrograms.publishAllDrafts()
    })

    await test.step('expect categories to be none on details page', async () => {
      await adminPrograms.gotoViewActiveProgramPage(SAMPLE_PROGRAMS.minimal)
      await expect(page.getByText('Categories: None')).toBeVisible()
    })

    await test.step('add two categories', async () => {
      await adminPrograms.selectProgramCategories(
        SAMPLE_PROGRAMS.minimal,
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

  test('view draft program', async ({page, adminPrograms, seeding}) => {
    await seeding.seedProgramsAndCategories()
    await loginAsAdmin(page)

    await adminPrograms.gotoEditDraftProgramPage(SAMPLE_PROGRAMS.minimal)

    await validateScreenshot(page, 'program-draft-view')
  })

  test('view program with universal questions', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await seeding.seedQuestions()
    await loginAsAdmin(page)

    const programName = 'Program with universal questions'
    // Sample name/email questions are universal, sample text question is not.
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      SAMPLE_QUESTIONS.name,
      SAMPLE_QUESTIONS.text,
      SAMPLE_QUESTIONS.email,
    ])
    await adminPrograms.publishAllDrafts()

    await adminPrograms.gotoViewActiveProgramPage(programName)
    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      SAMPLE_QUESTIONS.name,
      true,
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      SAMPLE_QUESTIONS.text,
      false,
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      SAMPLE_QUESTIONS.email,
      true,
    )
    await validateScreenshot(page, 'program-view-universal-questions')
  })

  test('view program, view multiple blocks, then start editing with extra long screen name and description', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await seeding.seedQuestions()
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Apc program'
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
          name: SAMPLE_QUESTIONS.address,
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
      SAMPLE_QUESTIONS.address,
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
    seeding,
  }) => {
    await seeding.seedQuestions()
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName, 'screen 2 description', [])
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      SAMPLE_QUESTIONS.address,
      SAMPLE_QUESTIONS.date,
      SAMPLE_QUESTIONS.email,
    ])
    await adminPrograms.publishAllDrafts()

    await adminPrograms.gotoViewActiveProgramPage(programName)

    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.gotoToBlockInReadOnlyProgram('2')
    await adminPrograms.expectReadOnlyProgramBlock('2')

    await adminPrograms.expectQuestionCardWithLabel(
      SAMPLE_QUESTIONS.address,
      'required question',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      SAMPLE_QUESTIONS.address,
      'address correction: disabled',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      SAMPLE_QUESTIONS.date,
      'required question',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      SAMPLE_QUESTIONS.email,
      'required question',
    )

    await validateScreenshot(page, 'view-program-block-2')

    await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
    await adminPrograms.expectProgramBlockEditPage(programName)
  })

  test('view external program', async ({page, adminPrograms}) => {
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
    applicantQuestions,
    seeding,
  }) => {
    const ProgramWithLongText = 'longTextProgram'
    const longParagraph =
      'Fur seals and sea lions make up the family Otariidae. Along with the Phocidae and Odobenidae, ottariids are pinnipeds descending from a common ancestor most closely related to modern bears (as hinted by the subfamily Arctocephalinae, meaning "bear-headed"). The name pinniped refers to mammals with front and rear flippers. Otariids arose about 15-17 million years ago in the Miocene, and were originally land mammals that rapidly diversified and adapted to a marine environment, giving rise to the semiaquatic marine mammals that thrive today. Fur seals and sea lions are closely related and commonly known together as the "eared seals". Until recently, fur seals were all grouped under a single subfamily of Pinnipedia, called the Arctocephalinae, to contrast them with Otariinae – the sea lions – based on the most prominent common feature, namely the coat of dense underfur intermixed with guard hairs. Recent genetic evidence, however, suggests Callorhinus is more closely related to some sea lion species, and the fur seal/sea lion subfamily distinction has been eliminated from many taxonomies. Nonetheless, all fur seals have certain features in common: the fur, generally smaller sizes, farther and longer foraging trips, smaller and more abundant prey items, and greater sexual dimorphism. For these reasons, the distinction remains useful. Fur seals comprise two genera: Callorhinus, and Arctocephalus. Callorhinus is represented by just one species in the Northern Hemisphere, the northern fur seal (Callorhinus ursinus), and Arctocephalus is represented by eight species in the Southern Hemisphere. The southern fur seals comprising the genus Arctocephalus include Antarctic fur seals, Galapagos fur seals, Juan Fernandez fur seals, New Zealand fur seals, brown fur seals, South American fur seals, and subantarctic fur seals.'

    await test.step('create a new program', async () => {
      await seeding.seedQuestions()
      await loginAsAdmin(page)
      await adminPrograms.addProgram(ProgramWithLongText)

      await adminPrograms.editProgramBlockUsingSpec(ProgramWithLongText, {
        name: 'First Block',
        description: 'First block',
        questions: [{name: SAMPLE_QUESTIONS.text, isOptional: false}],
      })

      await adminPrograms.editProgramBlockUsingSpec(ProgramWithLongText, {
        name: 'Second Block',
        description: 'Second block',
        questions: [
          {name: SAMPLE_QUESTIONS.address, isOptional: false},
          {name: SAMPLE_QUESTIONS.date, isOptional: false},
          {name: SAMPLE_QUESTIONS.email, isOptional: false},
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
      // The sample email question carries the primary-applicant-info email
      // tag, so the answered email replaces the account email as the
      // applicant's display name.
      await adminPrograms.viewApplicationForApplicant('test@example.com')

      await validateScreenshot(page, 'admin-view-application-long-text-answer')
    })
  })
})
