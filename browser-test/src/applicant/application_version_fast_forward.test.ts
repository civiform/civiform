import {test, expect} from '../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  closeWarningMessage,
  AdminPredicates,
} from '../support'
import {QuestionSpec} from '../support/admin_programs'
import {Browser, Locator, Page} from '@playwright/test'

test.describe('Application Version Fast Forward', () => {
  test.beforeEach(async ({request}) => {
    await test.step('Clear database', async () => {
      await request.post('/dev/seed/clear')
    })
  })

  test('all major steps', async ({browser}) => {
    const programName = 'program-fastforward-example'

    const civiformAdminActor = await FastForwardCiviformAdminActor.create(
      programName,
      browser,
    )
    const applicantActor = await FastForwardApplicantActor.create(
      programName,
      browser,
    )
    const programAdminActor = await FastForwardProgramAdminActor.create(
      programName,
      browser,
    )

    /*

      Program definitions

      Program: v1       Add (A, B, C, D, E)
        Block: First
          Question: A
        Block: Second
          Question: B  ---> Eligibility
        Block: Third
          Question: C  ---> Eligibility
        Block: Fourth
          Question: D
        Block: Fifth
          Question: E

      Program: v2       Remove (C, D), move (A, E), add (F, G), no-change (B)
        Block: First
          Question: G
        Block: Second
          Question: B  ---> Eligibility
        Block: Third
          Question: A
        Block: Fourth
          Question: E
        Block: Fifth
          Question: F

      Program: v3       Add (H), no-change (G, B, A, E, F)
        Block: First
          Question: G
        Block: Second
          Question: B  ---> Eligibility
        Block: Third
          Question: A
        Block: Fourth
          Question: E 
                    H ---> Eligibility
        Block: Fifth
          Question: F
    */

    /*
      High level overview

      - Civiform Admin creates program v1
      - Applicant fills out application to program v1; does not submit
      - Civiform Admin creates program v2
      - Applicant goes into the edit appliction page and stays in there
      - Civiform Admin creates program v3
      - Applicant, still on the edit page, submits application for program v1
      - Program Admin can view applications as expected for program v1


    */

    await test.step('Login actors', async () => {
      await civiformAdminActor.login()
      await applicantActor.login()
    })

    // Civiform Admin creates program v1
    const programIdV1 =
      await test.step('As civiform admin - active program v1', async () => {
        await test.step('create all questions', async () => {
          await civiformAdminActor.addQuestions([
            Question.A,
            Question.B,
            Question.C,
            Question.D,
            Question.E,
          ])
        })

        await test.step('create application', async () => {
          await civiformAdminActor.addProgram()
          await civiformAdminActor.gotoEditDraftProgramPage()

          await civiformAdminActor.addQuestionsToExistingBlocks([
            {
              block: Block.First,
              questions: [Question.A],
            },
          ])

          await civiformAdminActor.addQuestionsToNewBlocks([
            {
              block: Block.Second,
              questions: [Question.B],
              eligibilityValue: `${Question.B}-text-answer`,
            },
            {
              block: Block.Third,
              questions: [Question.C],
              eligibilityValue: `${Question.C}-text-answer`,
            },
            {
              block: Block.Fourth,
              questions: [Question.D],
            },
            {
              block: Block.Fifth,
              questions: [Question.E],
            },
          ])
        })

        return await test.step('publish application', async () => {
          const programIdV1 =
            civiformAdminActor.getProgramIdFromEditProgramUrl()
          await civiformAdminActor.publish()
          return programIdV1
        })
      })

    // Applicant fills out application for program v1; does not submit application
    await test.step('As applicant - active program v1', async () => {
      await test.step('check program list has no in-progress applications', async () => {
        await applicantActor.gotoApplicantHomePage()

        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.NotStarted),
        ).toBeAttached()
        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.InProgress),
        ).not.toBeAttached()
        await expect(
          applicantActor.getCardHeadingLocator(ApplicationStatus.NotStarted),
        ).toBeAttached()
      })

      await test.step('fill out application, but do not submit', async () => {
        await applicantActor.applyToProgram()
        expect(applicantActor.getProgramIdFromApplicationReviewUrl()).toBe(
          programIdV1,
        )

        await applicantActor.answerQuestions([
          Question.A,
          Question.B,
          Question.C,
          Question.D,
          Question.E,
        ])
      })

      await test.step('check program list has one in-progress application for program v1', async () => {
        await applicantActor.gotoApplicantHomePage()

        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.NotStarted),
        ).not.toBeAttached()
        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.InProgress),
        ).toBeAttached()

        const headingLocator = applicantActor.getCardHeadingLocator(
          ApplicationStatus.InProgress,
        )
        await expect(headingLocator).toBeAttached()

        expect(
          await applicantActor.getProgramIdFromLocator(headingLocator),
        ).toBe(programIdV1)
      })

      await test.step('check program list shows eligible tag', async () => {
        await expect(
          applicantActor.getCardEligibleTagLocator(
            ApplicationStatus.InProgress,
          ),
        ).toBeVisible()
        await expect(
          applicantActor.getCardNotEligibleTagLocator(
            ApplicationStatus.InProgress,
          ),
        ).not.toBeAttached()
      })
    })

    // Civiform Admin creates program v2
    const programIdV2 =
      await test.step('As civiform admin - active program v2', async () => {
        await test.step('create new questions', async () => {
          await civiformAdminActor.addQuestions([Question.F, Question.G])
        })

        await test.step('edit program', async () => {
          await civiformAdminActor.editProgram()
        })

        await test.step('remove questions from program', async () => {
          await civiformAdminActor.removeQuestionsFromBlock([
            {
              block: Block.First,
              questions: [Question.A],
            },
            {
              block: Block.Third,
              questions: [Question.C],
            },
            {
              block: Block.Fourth,
              questions: [Question.D],
            },
            {
              block: Block.Fifth,
              questions: [Question.E],
            },
          ])
        })

        return await test.step('add questions to program and republish program', async () => {
          await civiformAdminActor.addQuestionsToExistingBlocks([
            {
              block: Block.First,
              questions: [Question.G],
            },
            {
              block: Block.Third,
              questions: [Question.A],
            },
            {
              block: Block.Fourth,
              questions: [Question.E],
            },
            {
              block: Block.Fifth,
              questions: [Question.F],
            },
          ])

          const programIdV2 =
            civiformAdminActor.getProgramIdFromEditProgramUrl()
          await civiformAdminActor.publish()
          return programIdV2
        })
      })

    // Verify we have a new program version id
    expect(
      programIdV1,
      'Verify Program v1 and Program v2 have the different IDs.',
    ).not.toEqual(programIdV2)
    expect(
      programIdV1,
      'Verify Program v1 ID is less than Program v2 ID.',
    ).toBeLessThan(programIdV2)

    // Applicant edits existing application for program v1
    await test.step('As applicant - active program v2', async () => {
      await test.step('As applicant - check program list has one in-progress application for program v1', async () => {
        await applicantActor.gotoApplicantHomePage()

        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.NotStarted),
        ).not.toBeAttached()
        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.InProgress),
        ).toBeAttached()

        const headingLocator = applicantActor.getCardHeadingLocator(
          ApplicationStatus.InProgress,
        )
        await expect(headingLocator).toBeAttached()
        expect(
          await applicantActor.getProgramIdFromLocator(headingLocator),
        ).toBe(programIdV1)
      })

      await test.step('As applicant - load application and verify it is on program v1', async () => {
        await applicantActor.clickApplyProgramButton()
        expect(applicantActor.getProgramIdFromApplicationReviewUrl()).toBe(
          programIdV1,
        )
      })

      await test.step('As applicant - check program list has one in-progress application for program v1', async () => {
        await applicantActor.gotoApplicantHomePage()
        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.NotStarted),
        ).not.toBeAttached()
        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.InProgress),
        ).toBeAttached()

        const headingLocator = applicantActor.getCardHeadingLocator(
          ApplicationStatus.InProgress,
        )
        await expect(headingLocator).toBeAttached()
        expect(
          await applicantActor.getProgramIdFromLocator(headingLocator),
        ).toBe(programIdV1)
      })

      await test.step('As applicant - fill out application', async () => {
        await applicantActor.clickApplyProgramButton()
        await applicantActor.gotoEditQuestionPage(Question.A)
        expect(applicantActor.getProgramIdFromApplicationReviewUrl()).toBe(
          programIdV1,
        )
      })
    })

    // Civiform Admin creates program v3
    const programIdV3 =
      await test.step('As civiform admin - active program v3', async () => {
        await test.step('create new questions', async () => {
          await civiformAdminActor.addQuestions([Question.H])
        })

        await test.step('edit program', async () => {
          await civiformAdminActor.editProgram()
        })

        return await test.step('add questions to program and republish program', async () => {
          await civiformAdminActor.addQuestionsToExistingBlocks([
            {
              block: Block.Fourth,
              questions: [Question.H],
              eligibilityValue: `${Question.H}-text-answer`,
            },
          ])

          const programIdV3 =
            civiformAdminActor.getProgramIdFromEditProgramUrl()
          await civiformAdminActor.publish()
          return programIdV3
        })
      })

    // Verify we have a new program version id
    expect(
      programIdV2,
      'Verify Program v2 and Program v3 have the different IDs.',
    ).not.toEqual(programIdV3)
    expect(
      programIdV2,
      'Verify Program v2 ID is less than Program v3 ID.',
    ).toBeLessThan(programIdV3)

    // Applicant submits application for program v1; it does not change to program v3
    await test.step('As applicant - active program v3', async () => {
      await test.step('As applicant - submit application', async () => {
        await applicantActor.gotoNextPage()
        await applicantActor.submitApplication()
      })

      await test.step('As applicant - check program list has one submitted application for program v1', async () => {
        await applicantActor.gotoApplicantHomePage()
        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.InProgress),
        ).not.toBeAttached()
        await expect(
          applicantActor.getCardListLocator(ApplicationStatus.Submitted),
        ).toBeAttached()

        const headingLocator = applicantActor.getCardHeadingLocator(
          ApplicationStatus.Submitted,
        )
        await expect(headingLocator).toBeAttached()

        // Once submitted the program id on the edit button will be for the latest version
        expect(
          await applicantActor.getProgramIdFromLocator(headingLocator),
        ).toBe(programIdV3)
      })

      await test.step('check program list shows eligible tag', async () => {
        await expect(
          applicantActor.getCardEligibleTagLocator(ApplicationStatus.Submitted),
        ).toBeVisible()
        await expect(
          applicantActor.getCardNotEligibleTagLocator(
            ApplicationStatus.Submitted,
          ),
        ).not.toBeAttached()
      })
    })

    // Program Admin can view applications submitted for program v1
    await test.step('As program admin - active program v3', async () => {
      await test.step('Log in program admin', async () => {
        // Log in Program Admin here and not at the start. If logged in before the program is created by
        // a civiform admin, the program admin won't have permissions to the program.
        await programAdminActor.login()
        await programAdminActor.viewApplications()
      })

      await test.step('sees submitted application with questions from program version v1', async () => {
        const cardLocator = programAdminActor.getCardLocator()
        await expect(cardLocator).toHaveCount(1)

        const cardButton = cardLocator.getByRole('link', {name: 'View'})
        expect(
          await programAdminActor.parseProgramIdFromLocator(cardButton),
        ).toBe(programIdV1)
      })

      await test.step('does not see submitted application with questions from program version v2', async () => {
        const cardLocator = programAdminActor.getCardLocator()
        await expect(cardLocator).toHaveCount(1)

        const cardButton = cardLocator.getByRole('link', {name: 'View'})
        expect(
          await programAdminActor.parseProgramIdFromLocator(cardButton),
        ).not.toBe(programIdV2)
      })
    })
  })
})

/**
 * This class maintains the state and logic used by the Civiform Admin
 */
class FastForwardCiviformAdminActor {
  private programName: string
  private page: Page
  private adminPrograms: AdminPrograms
  private adminQuestions: AdminQuestions
  private adminPredicates: AdminPredicates

  /**
   * @constructor
   * @param {string} programName name of the program
   * @param {Page} page Unique page instance for this actor
   */
  private constructor(programName: string, page: Page) {
    this.programName = programName
    this.page = page
    this.adminPrograms = new AdminPrograms(page)
    this.adminQuestions = new AdminQuestions(page)
    this.adminPredicates = new AdminPredicates(page)
  }

  /**
   * Simplifies creation of the {FastForwardCiviformAdminActor}
   * @param {string} programName
   * @param {Browser} browser instance from Playwright
   * @returns {Promise<FastForwardCiviformAdminActor>} Promise to an instance of this class
   */
  public static async create(
    programName: string,
    browser: Browser,
  ): Promise<FastForwardCiviformAdminActor> {
    const context = await browser.newContext()
    return new FastForwardCiviformAdminActor(
      programName,
      await context.newPage(),
    )
  }

  /**
   * Log in the civiform admin actor
   */
  async login() {
    await this.page.goto('/programs')
    await closeWarningMessage(this.page)
    await loginAsAdmin(this.page)
  }

  /**
   * Log out the civiform admin actor
   */
  async logout() {
    await logout(this.page)
  }

  /**
   * Add a new program. This will navigate from /admin/programs/new until arrival on the program
   * block edit page
   */
  async addProgram() {
    await test.step('addProgram', async () => {
      await this.adminPrograms.addProgram(this.programName)
    })
  }

  /**
   * Edit an existing program. This will navigate to the default program block edit page
   */
  async editProgram() {
    await this.adminPrograms.editProgram(this.programName)
  }

  /**
   * Publishes a program
   */
  async publish() {
    await test.step('publish', async () => {
      await this.adminPrograms.publishProgram(this.programName)
    })
  }

  /**
   * List of questions to add to the system
   * @param {Array<Question>} questions
   */
  async addQuestions(questions: Question[]) {
    for (const question of questions) {
      await test.step(`add question ${question}`, async () => {
        await this.adminQuestions.addTextQuestion({
          questionName: question,
          questionText: question,
        })
      })
    }
  }

  /**
   * Navigate to the program block edit page
   */
  async gotoEditDraftProgramPage() {
    await test.step('gotoEditDraftProgramPage', async () => {
      await this.adminPrograms.gotoEditDraftProgramPage(this.programName)
    })
  }

  /**
   * Define the desired new block states to add to the program
   * @param {Array<BlockDefinition>} blockDefs list to add
   */
  async addQuestionsToNewBlocks(blockDefs: BlockDefinition[]) {
    for (const blockDef of blockDefs) {
      await test.step(`add ${blockDef.questions.length} question(s) to new block ${blockDef.block}`, async () => {
        await this.adminPrograms.addProgramBlockUsingSpecWhenAlreadyOnEditDraftPage(
          {
            name: blockDef.block,
            questions: blockDef.questions.map(
              (question) => <QuestionSpec>{name: question},
            ),
          },
          /* editBlockScreenDetails */ false,
        )
      })

      await this.configureQuestionEligibility(blockDef)
    }
  }

  /**
   * Define the desired block states to update on the program
   * @param {Array<BlockDefinition>} blockDefs list to add
   */
  async addQuestionsToExistingBlocks(blockDefs: BlockDefinition[]) {
    for (const blockDef of blockDefs) {
      await test.step(`add ${blockDef.questions.length} question(s) to existing block ${blockDef.block}`, async () => {
        await this.adminPrograms.addQuestionsToProgramBlock({
          name: blockDef.block,
          questions: blockDef.questions.map(
            (question) => <QuestionSpec>{name: question},
          ),
        })
      })

      await this.configureQuestionEligibility(blockDef)
    }
  }

  /**
   * Set up eligibility conditions on the specified block
   * @param {BlockDefinition} blockDef
   */
  private async configureQuestionEligibility(blockDef: BlockDefinition) {
    if (blockDef.eligibilityValue === undefined) {
      return
    }

    await test.step(`Navigate to edit block eligibility page for block ${blockDef.block}`, async () => {
      await this.adminPrograms.goToEditBlockEligibilityPredicatePage(
        this.programName,
        blockDef.block,
      )
    })

    await test.step(`Remove eligibility if already configured on block ${blockDef.block}`, async () => {
      const removeExistingEligibilityButtonLocator = this.page.getByRole(
        'button',
        {name: 'Remove existing eligibility condition'},
      )

      if (await removeExistingEligibilityButtonLocator.isEnabled()) {
        await this.adminPredicates.clickRemovePredicateButton('eligibility')
      }
    })

    await test.step(`Add eligibility predicate to block ${blockDef.block}`, async () => {
      await this.adminPredicates.addPredicates({
        questionName: blockDef.questions[0],
        scalar: 'text',
        operator: 'is equal to',
        value: blockDef.eligibilityValue,
      })
    })

    await this.gotoEditDraftProgramPage()
  }

  /**
   * Define the desired block states with the list of questions to remove from the block
   * @param {Array<BlockDefinition>} blockDefs
   */
  async removeQuestionsFromBlock(blockDefs: BlockDefinition[]) {
    for (const blockDef of blockDefs) {
      await test.step(`remove ${blockDef.questions.length} question(s) from block ${blockDef.block}`, async () => {
        await this.adminPrograms.removeQuestionFromProgram(
          this.programName,
          blockDef.block,
          blockDef.questions,
        )
      })
    }
  }

  /**
   * Gets the program id from the admin program edit page url
   * @returns {number} programId. If the ID is not found a failed assertion will end the test.
   */
  getProgramIdFromEditProgramUrl() {
    const pattern = /\/admin\/programs\/(?<id>[0-9]*)\/*/
    const id = parseInt(this.page.url().match(pattern)?.groups?.id || '-1')
    expect(id).toBeGreaterThan(0)

    return id
  }
}

/**
 * This class maintains the state and logic used by the Applicant actor
 */
class FastForwardApplicantActor {
  private programName: string
  private page: Page
  private applicantQuestions: ApplicantQuestions

  /**
   * @constructor
   * @param {string} programName name of the program
   * @param {Page} page Unique page instance for this actor
   */
  private constructor(programName: string, page: Page) {
    this.programName = programName
    this.page = page
    this.applicantQuestions = new ApplicantQuestions(page)
  }

  /**
   * Simplifies creation of the {FastForwardApplicantActor}
   * @param {string} programName
   * @param {Browser} browser instance from Playwright
   * @returns {Promise<FastForwardApplicantActor>} Promise to an instance of this class
   */
  public static async create(
    programName: string,
    browser: Browser,
  ): Promise<FastForwardApplicantActor> {
    const context = await browser.newContext()
    return new FastForwardApplicantActor(programName, await context.newPage())
  }

  /**
   * Log in the applicant actor
   */
  async login() {
    await this.page.goto('/programs')
    await closeWarningMessage(this.page)
    await loginAsTestUser(this.page)
  }

  /**
   * Logout the applicant actor
   */
  async logout() {
    await logout(this.page)
  }

  /**
   * Get the card list locator for the desired application status
   * @param {ApplicationStatus} applicationStatus to find
   * @returns {Locator} Locator to the card list
   */
  getCardListLocator(applicationStatus: ApplicationStatus): Locator {
    return this.page.getByRole('list', {name: applicationStatus})
  }

  /**
   * Get the locator for program card heading in the desired list
   * @param {ApplicationStatus} applicationStatus to find
   * @returns {Locator} Locator for program card in the desired list
   */
  getCardHeadingLocator(applicationStatus: ApplicationStatus): Locator {
    return this.getCardListLocator(applicationStatus).getByRole('heading', {
      name: this.programName,
    })
  }

  /**
   * Get the eligibile tag for the desired application status
   * @param {ApplicationStatus} applicationStatus to find
   * @returns {Locator} Locator to the eligible tag
   */
  getCardEligibleTagLocator(applicationStatus: ApplicationStatus): Locator {
    return this.getCardListLocator(applicationStatus).locator(
      '.cf-eligible-tag',
    )
  }

  /**
   * Get the not eligibile tag for the desired application status
   * @param {ApplicationStatus} applicationStatus to find
   * @returns {Locator} Locator to the not eligible tag
   */
  getCardNotEligibleTagLocator(applicationStatus: ApplicationStatus): Locator {
    return this.getCardListLocator(applicationStatus).locator(
      '.cf-not-eligible-tag',
    )
  }

  /**
   * Navigate to `/programs`
   */
  async gotoApplicantHomePage() {
    await this.applicantQuestions.gotoApplicantHomePage()
  }

  /**
   * Apply to the program
   *
   * Must be on the `/programs` page
   */
  async applyToProgram() {
    await test.step(`apply to program ${this.programName}`, async () => {
      await this.applicantQuestions.applyProgram(this.programName)
    })
  }

  /**
   * Navigates to the application edit page
   *
   * Must be on a application review page
   * @param question
   */
  async gotoEditQuestionPage(question: Question) {
    await this.page.getByLabel(`Edit ${question}`).click()
  }

  /**
   * Navigates to the next page of an application
   *
   * Must be on an application edit page
   * @param question
   */
  async gotoNextPage() {
    await this.applicantQuestions.clickNext()
  }

  /**
   * Fills in the answer to a question
   *
   * Must be on a application edit page
   * @param question
   */
  async answerQuestions(questions: Question[]) {
    for (const question of questions) {
      await test.step(`answer question ${question}`, async () => {
        await this.applicantQuestions.answerTextQuestion(
          `${question}-text-answer`,
        )
        await this.applicantQuestions.clickNext()
      })
    }
  }

  /**
   * Fills in the answer to a question that was previous answered
   *
   * Must be on a application edit page
   * @param question
   */
  private async reanswerQuestion(question: Question) {
    await test.step(`reanswer question ${question}`, async () => {
      await this.page.getByLabel(`${question}`).click()
      await this.applicantQuestions.answerTextQuestion(
        `${question}-text-answer`,
      )
      await this.applicantQuestions.clickNext()
    })
  }

  private async continueAnswerApplicationQuestions() {
    await test.step('continue working on application', async () => {
      await this.applicantQuestions.clickContinue()
    })
  }

  /**
   * Fill the apply button on the desired program on the /programs page
   */
  async clickApplyProgramButton() {
    await this.applicantQuestions.clickApplyProgramButton(this.programName)
  }

  /**
   * Submit the application
   */
  async submitApplication() {
    await this.applicantQuestions.submitFromReviewPage()
  }

  /**
   * Extracts the program id from the supplied locators id attribute
   * @param {Locator} locator to the application card containing the .cf-application-card-####-title class
   * @returns {Promise<number>} Promise to to programId. If the ID is not found a failed assertion will end the test.
   */
  async getProgramIdFromLocator(locator: Locator): Promise<number> {
    const idAttribute = (await locator.getAttribute('id')) || ''
    const pattern = /cf-application-card-(?<id>[0-9]*)-title/

    const id = parseInt(idAttribute?.match(pattern)?.groups?.id || '-1')
    expect(id).toBeGreaterThan(0)

    return id
  }

  /**
   * Gets the program id from the application review page url
   * @returns {number} programId. If the ID is not found a failed assertion will end the test.
   */
  getProgramIdFromApplicationReviewUrl(): number {
    const pattern = /\/programs\/(?<id>[0-9]*)\/*/
    const id = parseInt(this.page.url().match(pattern)?.groups?.id || '-1')
    expect(id).toBeGreaterThan(0)

    return id
  }
}

/**
 * This class maintains the state and logic used by the Program Admin actorfs
 */
class FastForwardProgramAdminActor {
  private programName: string
  private page: Page
  private adminPrograms: AdminPrograms

  /**
   * @constructor
   * @param {string} programName name of the program
   * @param {Page} page Unique page instance for this actor
   */
  private constructor(programName: string, page: Page) {
    this.programName = programName
    this.page = page
    this.adminPrograms = new AdminPrograms(page)
  }

  /**
   * Simplifies creation of the {FastForwardProgramAdminActor}
   * @param {string} programName
   * @param {Browser} browser instance from Playwright
   * @returns {Promise<FastForwardProgramAdminActor>} Promise to an instance of this class
   */
  public static async create(
    programName: string,
    browser: Browser,
  ): Promise<FastForwardProgramAdminActor> {
    const context = await browser.newContext()
    return new FastForwardProgramAdminActor(
      programName,
      await context.newPage(),
    )
  }

  /**
   * Log in the program admin actor
   */
  async login() {
    await this.page.goto('/programs')
    await closeWarningMessage(this.page)
    await loginAsProgramAdmin(this.page)
  }

  /**
   * Log out the program admin actor
   */
  async logout() {
    await logout(this.page)
  }

  /**
   * Navigate to the view applications page at `/admin/programs/:programId/applications
   */
  async viewApplications() {
    await this.adminPrograms.viewApplications(this.programName)
  }

  /**
   * Get a locator any admin application cards. There may be 0-♾️
   * @returns {Locator} Locator to admin application card
   */
  getCardLocator(): Locator {
    return this.page.locator('.cf-admin-application-card')
  }

  /**
   * Gets the program id from the href of the locator
   * @param {Locator} locator to an admin application card element containing an href to the application
   * @returns {number} programId. If the ID is not found a failed assertion will end the test.
   */
  async parseProgramIdFromLocator(locator: Locator) {
    const valueToSearch = (await locator.getAttribute('href')) || ''
    const pattern = /\/admin\/programs\/(?<id>[0-9]*)\/applications\/*/

    const id = parseInt(valueToSearch?.match(pattern)?.groups?.id || '-1')
    expect(id).toBeGreaterThan(0)

    return id
  }
}

/**
 * Test suite infernal definition of a block and its questions configuration
 */
interface BlockDefinition {
  block: Block
  questions: Question[]
  eligibilityValue?: string
}

/**
 * List of all question names used in this test suite
 */
enum Question {
  A = 'question-name-a',
  B = 'question-name-b',
  C = 'question-name-c',
  D = 'question-name-d',
  E = 'question-name-e',
  F = 'question-name-f',
  G = 'question-name-g',
  H = 'question-name-h',
}

/**
 * List of all block names used in this test suite
 */
enum Block {
  First = 'Screen 1',
  Second = 'Screen 2',
  Third = 'Screen 3',
  Fourth = 'Screen 4',
  Fifth = 'Screen 5',
}

/**
 * List of application statuses used in this test suite
 */
enum ApplicationStatus {
  InProgress = 'In progress',
  NotStarted = 'Not started',
  Submitted = 'Submitted',
}
