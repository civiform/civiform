import {test, expect} from '../support/civiform_fixtures'
import {
  AdminPredicates,
  AdminPrograms,
  ApplicantQuestions,
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  waitForPageJsLoad,
} from '../support'
import {AdminQuestions, QuestionDisplayMode} from '../support/admin_questions'
import {Page} from '@playwright/test'
import {QuestionSpec} from '../support/admin_programs'
import {BridgeDiscoveryPage} from '../page/admin/api_bridge/bridge_discovery_page'
import {MOCK_WEB_SERVICES_URL} from '../support/config'
import {ProgramBridgeConfigurationPage} from '../page/admin/programs/program_bridge_configuration_page'

test.describe('hidden questions end to end', () => {
  const programName = 'ProgramA'

  test.beforeEach(
    async ({page, seeding, adminPrograms, adminQuestions, adminPredicates}) => {
      const adminActor = new AdminActor(
        programName,
        page,
        adminPrograms,
        adminQuestions,
        adminPredicates,
      )

      await enableFeatureFlag(page, 'API_BRIDGE_ENABLED')
      await enableFeatureFlag(page, 'ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS')

      await seeding.seedQuestions()

      await adminActor.login()
      await adminActor.addYesNoQuestion(QuestionName.Screen4YesNoQuestion)

      await adminActor.configureNewProgram([
        {
          blockName: BlockName.Screen1,
          questionNames: [
            QuestionName.Screen1AddressQuestion,
            QuestionName.Screen1CheckboxQuestion,
            QuestionName.Screen1CurrencyQuestion,
          ],
        },
        {
          blockName: BlockName.Screen2,
          questionNames: [
            QuestionName.Screen2DateQuestion,
            QuestionName.Screen2DropdownQuestion,
            QuestionName.Screen2EmailQuestion,
          ],
        },
        {
          blockName: BlockName.Screen3,
          questionNames: [
            QuestionName.Screen3IdQuestion,
            QuestionName.Screen3NameQuestion,
            QuestionName.Screen3NumberQuestion,
          ],
        },
        {
          blockName: BlockName.Screen4,
          questionNames: [
            QuestionName.Screen4PhoneQuestion,
            QuestionName.Screen4RadioButtonQuestion,
            QuestionName.Screen4YesNoQuestion,
            QuestionName.Screen4TextQuestion,
          ],
        },
      ])
    },
  )

  test('single block with mixed question visibility', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
    adminPredicates,
  }) => {
    const adminActor = new AdminActor(
      programName,
      page,
      adminPrograms,
      adminQuestions,
      adminPredicates,
    )
    const applicantActor = new ApplicantActor(
      programName,
      page,
      applicantQuestions,
    )

    await adminActor.updateQuestionsToHidden([
      QuestionName.Screen1CheckboxQuestion,
      QuestionName.Screen1CurrencyQuestion,
    ])

    await test.step('admin publish and logout', async () => {
      await adminActor.publishAllDrafts()
      await adminActor.logout()
    })

    await test.step('applicant start application', async () => {
      await applicantActor.login()
      await applicantActor.startApplication()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen1}`, async () => {
      await applicantActor.answerAddressQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen2}`, async () => {
      await applicantActor.answerMemorableDateQuestion()
      await applicantActor.answerDropdownQuestion()
      await applicantActor.answerEmailQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen3}`, async () => {
      await applicantActor.answerIdQuestion()
      await applicantActor.answerNameQuestion()
      await applicantActor.answerNumberQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen4}`, async () => {
      await applicantActor.answerPhoneQuestion()
      await applicantActor.answerRadioButtonQuestion()
      await applicantActor.answerYesNoQuestion()
      await applicantActor.answerTextQuestion()
      await applicantActor.saveAndNext()
    })

    await applicantActor.submitApplication()
  })

  test('all questions hidden on single block', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
    adminPredicates,
  }) => {
    const adminActor = new AdminActor(
      programName,
      page,
      adminPrograms,
      adminQuestions,
      adminPredicates,
    )
    const applicantActor = new ApplicantActor(
      programName,
      page,
      applicantQuestions,
    )

    await adminActor.updateQuestionsToHidden([
      QuestionName.Screen2DateQuestion,
      QuestionName.Screen2DropdownQuestion,
      QuestionName.Screen2EmailQuestion,
    ])

    await test.step('admin publish and logout', async () => {
      await adminActor.publishAllDrafts()
      await adminActor.logout()
    })

    await test.step('applicant start application', async () => {
      await applicantActor.login()
      await applicantActor.startApplication()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen1}`, async () => {
      await applicantActor.answerAddressQuestion()
      await applicantActor.answerCheckboxQuestion()
      await applicantActor.answerCurrencyQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen3}`, async () => {
      await applicantActor.answerIdQuestion()
      await applicantActor.answerNameQuestion()
      await applicantActor.answerNumberQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen4}`, async () => {
      await applicantActor.answerPhoneQuestion()
      await applicantActor.answerRadioButtonQuestion()
      await applicantActor.answerYesNoQuestion()
      await applicantActor.answerTextQuestion()
      await applicantActor.saveAndNext()
    })

    await applicantActor.submitApplication()
  })

  test('all questions hidden on two blocks', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
    adminPredicates,
  }) => {
    const adminActor = new AdminActor(
      programName,
      page,
      adminPrograms,
      adminQuestions,
      adminPredicates,
    )
    const applicantActor = new ApplicantActor(
      programName,
      page,
      applicantQuestions,
    )

    await adminActor.updateQuestionsToHidden([
      QuestionName.Screen2DateQuestion,
      QuestionName.Screen2DropdownQuestion,
      QuestionName.Screen2EmailQuestion,
      QuestionName.Screen3IdQuestion,
      QuestionName.Screen3NameQuestion,
      QuestionName.Screen3NumberQuestion,
    ])

    await test.step('admin publish and logout', async () => {
      await adminActor.publishAllDrafts()
      await adminActor.logout()
    })

    await test.step('applicant start application', async () => {
      await applicantActor.login()
      await applicantActor.startApplication()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen1}`, async () => {
      await applicantActor.answerAddressQuestion()
      await applicantActor.answerCheckboxQuestion()
      await applicantActor.answerCurrencyQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen4}`, async () => {
      await applicantActor.answerPhoneQuestion()
      await applicantActor.answerRadioButtonQuestion()
      await applicantActor.answerYesNoQuestion()
      await applicantActor.answerTextQuestion()
      await applicantActor.saveAndNext()
    })

    await applicantActor.submitApplication()
  })

  test('blocks with mixed question visibility and one block with all questions hidden', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
    adminPredicates,
  }) => {
    const adminActor = new AdminActor(
      programName,
      page,
      adminPrograms,
      adminQuestions,
      adminPredicates,
    )
    const applicantActor = new ApplicantActor(
      programName,
      page,
      applicantQuestions,
    )

    await adminActor.updateQuestionsToHidden([
      QuestionName.Screen1CheckboxQuestion,
      QuestionName.Screen2DateQuestion,
      QuestionName.Screen2DropdownQuestion,
      QuestionName.Screen2EmailQuestion,
      QuestionName.Screen3IdQuestion,
      QuestionName.Screen3NameQuestion,
    ])

    await test.step('admin publish and logout', async () => {
      await adminActor.publishAllDrafts()
      await adminActor.logout()
    })

    await test.step('applicant start application', async () => {
      await applicantActor.login()
      await applicantActor.startApplication()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen1}`, async () => {
      await applicantActor.answerAddressQuestion()
      await applicantActor.answerCurrencyQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen3}`, async () => {
      await applicantActor.answerNumberQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen4}`, async () => {
      await applicantActor.answerPhoneQuestion()
      await applicantActor.answerRadioButtonQuestion()
      await applicantActor.answerYesNoQuestion()
      await applicantActor.answerTextQuestion()
      await applicantActor.saveAndNext()
    })

    await applicantActor.submitApplication()
  })

  test('blocks with mixed question visibility using the api bridge', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
    adminPredicates,
  }) => {
    test.skip(!isLocalDevEnvironment(), 'Requires mock-web-services')

    const adminActor = new AdminActor(
      programName,
      page,
      adminPrograms,
      adminQuestions,
      adminPredicates,
    )
    const applicantActor = new ApplicantActor(
      programName,
      page,
      applicantQuestions,
    )

    await adminActor.addApiBridge()

    await adminActor.editProgram()
    await adminActor.configureApiBridgeForProgram()

    await adminActor.updateQuestionsToHidden([
      QuestionName.Screen1CheckboxQuestion,
      QuestionName.Screen2DateQuestion,
      QuestionName.Screen2DropdownQuestion,
      QuestionName.Screen2EmailQuestion,
      QuestionName.Screen3IdQuestion,
      QuestionName.Screen3NameQuestion,
      QuestionName.Screen4YesNoQuestion,
    ])

    await test.step('admin publish and logout', async () => {
      await adminActor.publishAllDrafts()
      await adminActor.logout()
    })

    await test.step('applicant start application', async () => {
      await applicantActor.login()
      await applicantActor.startApplication()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen1}`, async () => {
      await applicantActor.answerAddressQuestion()
      await applicantActor.answerCurrencyQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen3}`, async () => {
      await applicantActor.answerNumberQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen4}`, async () => {
      await applicantActor.answerPhoneQuestion()
      await applicantActor.answerRadioButtonQuestion()
      await applicantActor.answerTextQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step('applicant submits application', async () => {
      await applicantActor.submitApplication()
      await applicantActor.logout()
    })

    await test.step('admin log in', async () => {
      await adminActor.login()
    })

    await test.step('admin application details include hidden questions', async () => {
      await adminActor.adminApplicationDetailsIncludeHiddenQuestion()
    })
  })

  test('single block with single hidden question used in eligibility check', async ({
    page,
    adminPrograms,
    adminQuestions,
    applicantQuestions,
    adminPredicates,
  }) => {
    test.skip(!isLocalDevEnvironment(), 'Requires mock-web-services')

    const adminActor = new AdminActor(
      programName,
      page,
      adminPrograms,
      adminQuestions,
      adminPredicates,
    )
    const applicantActor = new ApplicantActor(
      programName,
      page,
      applicantQuestions,
    )

    await adminActor.addApiBridge()

    await adminActor.editProgram()
    await adminActor.configureApiBridgeForProgram()

    await adminActor.updateQuestionsToHidden([
      QuestionName.Screen4YesNoQuestion,
    ])

    await adminActor.configureQuestionEligibility(
      BlockName.Screen4,
      QuestionName.Screen4TextQuestion,
      'text',
    )

    await test.step('admin publish and logout', async () => {
      await adminActor.publishAllDrafts()
      await adminActor.logout()
    })

    await test.step('applicant start application', async () => {
      await applicantActor.login()
      await applicantActor.startApplication()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen1}`, async () => {
      await applicantActor.answerAddressQuestion()
      await applicantActor.answerCheckboxQuestion()
      await applicantActor.answerCurrencyQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen2}`, async () => {
      await applicantActor.answerMemorableDateQuestion()
      await applicantActor.answerDropdownQuestion()
      await applicantActor.answerEmailQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen3}`, async () => {
      await applicantActor.answerIdQuestion()
      await applicantActor.answerNameQuestion()
      await applicantActor.answerNumberQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step(`applicant apply to program - ${BlockName.Screen4}`, async () => {
      await applicantActor.answerPhoneQuestion()
      await applicantActor.answerRadioButtonQuestion()
      await applicantActor.answerTextQuestion()
      await applicantActor.saveAndNext()
    })

    await test.step('applicant submits application', async () => {
      await applicantActor.submitApplication()
    })
  })
})

class AdminActor {
  private programName: string
  private page: Page
  private adminPrograms: AdminPrograms
  private adminQuestions: AdminQuestions
  private adminPredicates: AdminPredicates

  constructor(
    programName: string,
    page: Page,
    adminProgram: AdminPrograms,
    adminQuestions: AdminQuestions,
    adminPredicates: AdminPredicates,
  ) {
    this.programName = programName
    this.page = page
    this.adminPrograms = adminProgram
    this.adminQuestions = adminQuestions
    this.adminPredicates = adminPredicates
  }

  async login() {
    await loginAsAdmin(this.page)
  }

  async logout() {
    await logout(this.page)
  }

  async addProgram() {
    await test.step(`add program ${this.programName}`, async () => {
      await this.adminPrograms.addProgram(this.programName)
    })
  }

  async editProgram() {
    await test.step(`go to program edit page for ${this.programName}`, async () => {
      await this.adminPrograms.gotoEditDraftProgramPage(this.programName)
    })
  }

  async configureNewProgram(
    blocks: {blockName: BlockName; questionNames: QuestionName[]}[],
  ) {
    if (blocks.length === 0) {
      throw new Error('No blocks provided')
    }

    await this.addProgram()

    const firstBlock = blocks.shift()!
    await this.editBlock(firstBlock.blockName, firstBlock.questionNames)

    for (const block of blocks) {
      await this.addBlock(block.blockName, block.questionNames)
    }
  }

  async editBlock(blockName: BlockName, questionNames: QuestionName[]) {
    await test.step(`edit block ${blockName}`, async () => {
      await this.adminPrograms.editProgramBlock(this.programName, blockName)
      await this.adminPrograms.addQuestionsToProgramBlock({
        name: blockName,
        questions: questionNames.map((q) => <QuestionSpec>{name: q}),
      })
    })
  }

  async addBlock(blockName: BlockName, questionNames: QuestionName[]) {
    await test.step(`add block ${blockName}`, async () => {
      await this.adminPrograms.addProgramBlock(this.programName, blockName)
      await this.adminPrograms.addQuestionsToProgramBlock({
        name: blockName,
        questions: questionNames.map((q) => <QuestionSpec>{name: q}),
      })
    })
  }

  async publishAllDrafts() {
    await this.adminPrograms.publishAllDrafts()
  }

  async addYesNoQuestion(questionName: QuestionName) {
    await test.step(`add yesno question for ${questionName}`, async () => {
      await this.adminQuestions.addYesNoQuestion({questionName: questionName})
    })
  }

  async updateQuestionsToHidden(questionNames: QuestionName[]) {
    for (const questionName of questionNames) {
      await this.updateQuestionToHidden(questionName)
    }
  }

  private async updateQuestionToHidden(questionName: QuestionName) {
    await test.step(`Set ${questionName} to hidden`, async () => {
      await this.adminQuestions.gotoQuestionEditPage(questionName)
      await this.adminQuestions.selectDisplayMode(QuestionDisplayMode.HIDDEN)
      await this.adminQuestions.clickSubmitButtonAndNavigate('Update')
    })
  }

  async configureQuestionEligibility(
    blockName: BlockName,
    questionName: QuestionName,
    eligibilityValue: string,
  ) {
    await test.step(`Navigate to edit block eligibility page for block ${blockName}`, async () => {
      await this.adminPrograms.goToEditBlockEligibilityPredicatePage(
        this.programName,
        blockName,
      )
    })

    await test.step(`Add eligibility predicate to block ${blockName}`, async () => {
      await this.adminPredicates.addPredicates({
        questionName: questionName,
        scalar: 'text',
        operator: 'is equal to',
        value: eligibilityValue,
      })
    })

    await this.editProgram()
  }

  async addApiBridge() {
    await test.step('Add api bridge', async () => {
      const hostUrl = `${MOCK_WEB_SERVICES_URL}/api-bridge`
      const urlPath = '/bridge/success'

      const bridgeDiscoveryPage = new BridgeDiscoveryPage(this.page)
      await bridgeDiscoveryPage.goto()
      await bridgeDiscoveryPage.fillUrl(hostUrl)
      await bridgeDiscoveryPage.clickSearchButton()
      await bridgeDiscoveryPage.clickAddButton(urlPath)
    })
  }

  async configureApiBridgeForProgram() {
    await test.step('Configure api bridge', async () => {
      await this.adminPrograms.clickEditBridgeDefinitionButton()

      const programBridgeConfiguration = new ProgramBridgeConfigurationPage(
        this.page,
      )
      await programBridgeConfiguration.changeBridgeAdminName('bridge-success')

      await programBridgeConfiguration.setInputQuestion(
        'ZIP Code',
        QuestionName.Screen1AddressQuestion.toString().replaceAll(' ', '_'),
      )
      await programBridgeConfiguration.setInputScalar('ZIP Code', 'ZIP')
      await programBridgeConfiguration.setInputQuestion(
        'Account Number',
        QuestionName.Screen3NumberQuestion.replaceAll(' ', '_'),
      )

      await programBridgeConfiguration.setOutputQuestion(
        'Is Valid',
        QuestionName.Screen4YesNoQuestion.replaceAll(' ', '_'),
      )
      await programBridgeConfiguration.setOutputQuestion(
        'Account Number',
        QuestionName.Screen3NumberQuestion.replaceAll(' ', '_'),
      )

      await programBridgeConfiguration.save()
      await waitForPageJsLoad(this.page)
    })
  }

  async adminApplicationDetailsIncludeHiddenQuestion() {
    await this.adminPrograms.viewApplications(this.programName)
    await this.adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    await expect(
      this.page.getByText(QuestionName.Screen4YesNoQuestion),
    ).toBeVisible()
  }
}

class ApplicantActor {
  private programName: string
  private page: Page
  private applicantQuestions: ApplicantQuestions

  constructor(
    programName: string,
    page: Page,
    applicantQuestions: ApplicantQuestions,
  ) {
    this.programName = programName
    this.page = page
    this.applicantQuestions = applicantQuestions
  }

  async login() {
    await loginAsTestUser(this.page)
  }

  async logout() {
    await logout(this.page)
  }

  async startApplication() {
    await test.step(`Apply to program ${this.programName}`, async () => {
      await this.applicantQuestions.applyProgram(
        this.programName,
        /* northStarEnabled= */ true,
      )
    })
  }

  async answerAddressQuestion() {
    await this.applicantQuestions.answerAddressQuestion(
      'Address In Area',
      '',
      'Seattle',
      'WA',
      '98109',
    )
  }

  async answerCheckboxQuestion() {
    await this.applicantQuestions.answerCheckboxQuestion(['Toaster'])
  }

  async answerCurrencyQuestion() {
    await this.applicantQuestions.answerCurrencyQuestion('10')
  }

  async answerMemorableDateQuestion() {
    await this.applicantQuestions.answerMemorableDateQuestion(
      '2025',
      '06 - June',
      '1',
    )
  }

  async answerDropdownQuestion() {
    await this.applicantQuestions.answerDropdownQuestion('Chocolate')
  }

  async answerEmailQuestion() {
    await this.applicantQuestions.answerEmailQuestion(
      'user@localhost.localdomain',
    )
  }

  async answerIdQuestion() {
    await this.applicantQuestions.answerIdQuestion('12345')
  }

  async answerNameQuestion() {
    await this.applicantQuestions.answerNameQuestion('firstname', 'lastname')
  }

  async answerNumberQuestion() {
    await this.applicantQuestions.answerNumberQuestion('1234')
  }

  async answerPhoneQuestion() {
    await this.applicantQuestions.answerPhoneQuestion('253-555-5555')
  }

  async answerRadioButtonQuestion() {
    await this.applicantQuestions.answerRadioButtonQuestion('Spring')
  }

  async answerYesNoQuestion() {
    await this.applicantQuestions.answerYesNoQuestion('Yes')
  }

  async answerTextQuestion() {
    await this.applicantQuestions.answerTextQuestion('text', 1)
  }

  async saveAndNext() {
    await this.applicantQuestions.clickContinue()
  }

  async submitApplication() {
    await test.step('submit application', async () => {
      await this.applicantQuestions.clickSubmitApplication()
    })
  }
}

enum BlockName {
  Screen1 = 'Screen 1',
  Screen2 = 'Screen 2',
  Screen3 = 'Screen 3',
  Screen4 = 'Screen 4',
}

enum QuestionName {
  // Screen 1
  Screen1AddressQuestion = 'Sample Address Question',
  Screen1CheckboxQuestion = 'Sample Checkbox Question',
  Screen1CurrencyQuestion = 'Sample Currency Question',
  // Screen 2
  Screen2DateQuestion = 'Sample Date Question',
  Screen2DropdownQuestion = 'Sample Dropdown Question',
  Screen2EmailQuestion = 'Sample Email Question',
  // Screen 3
  Screen3IdQuestion = 'Sample ID Question',
  Screen3NameQuestion = 'Sample Name Question',
  Screen3NumberQuestion = 'Sample Number Question',
  // Screen 4
  Screen4PhoneQuestion = 'Sample Phone Question',
  Screen4RadioButtonQuestion = 'Sample Radio Button Question',
  Screen4YesNoQuestion = 'Sample Yes No Question',
  Screen4TextQuestion = 'Sample Text Question',
}
