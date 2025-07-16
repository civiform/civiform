import {test} from './support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  loginAsAdmin,
  logout,
  closeWarningMessage,
  AdminPredicates,
} from './support'
import {ProgramVisibility, QuestionSpec} from './support/admin_programs'
import {Browser, Page} from '@playwright/test'

test.describe('Caching bug', () => {
  const programName = 'program-fastforward-example'
  const qs: QuestionArray = Array.from({ length: 10}, (_, i) => i + 1).map(x => 'question-name-' + int2roman(x) + '')

  function int2roman(original: number): string {
    const numerals = [
      ['i', 'ii', 'iii', 'iv', 'v', 'vi', 'vii', 'viii', 'ix'], // 1-9
      ['x', 'xx', 'xxx', 'xl', 'l', 'lx', 'lxx', 'lxxx', 'xc'], // 10-90
      ['c', 'cc', 'ccc', 'cd', 'd', 'dc', 'dcc', 'dccc', 'cm'], // 100-900
      ['m', 'mm', 'mmm'], // 1000-3000
    ];
  
    // Round number and split into digits
    const digits = Math.round(original).toString().split('');
    let position = digits.length - 1;
  
    return digits.reduce((roman, digit) => {
      if (digit !== '0') {
        roman += numerals[position][parseInt(digit) - 1]; // Fixed closing bracket
      }
      position -= 1;
      return roman;
    }, '');
  }

  test('setup - create questions and programs', async ({browser, request}) => {
    await test.step('Clear database', async () => {
      await request.post('/dev/seed/clear')
    })

    const civiformAdminActor = await FastForwardCiviformAdminActor.create(programName, browser)
    
    await test.step('login actors', async () => {
      await civiformAdminActor.login()
    })

    await test.step('create all questions', async () => {
      await civiformAdminActor.addQuestions(qs)
    })

    await test.step('create and publish application', async () => {
      await civiformAdminActor.addProgram()
      await civiformAdminActor.publish()
    })

    await civiformAdminActor.closeBrowserContext()
  })

  test('error1 - creates two programs on the same version', async ({browser}) => {
    const questionList = [...qs]

    function getRange(start?: number, end?: number) : QuestionArray {
      const results = questionList.slice(start, end || 1)
      console.log(results)
      return results
    }
  
    function getQuestion(index: number) : string {
      const result = questionList[index] 
      console.log(result)
      return result
    }



    const civiformAdminActor = await FastForwardCiviformAdminActor.create(programName, browser)

    await test.step('Login actors', async () => {
      await civiformAdminActor.login()
    })

    const run : Run = {
      passes: [
        {
          addQuestionsToExistingBlocks : [
            {
              block: Block.First,
              questions: getRange(0),
            },
          ],
          addQuestionsToNewBlocks: [
            {
              block: Block.Second,
              questions: getRange(1),
              // eligibilityValue: `${getQuestion(1)}-text-answer`,
            },
          ],
          removeEligibilityFromBlockDefinitions: [],
          removeQuestionsFromBlock: []
        }, 
        {
          addQuestionsToExistingBlocks: [
            {
              block: Block.First,
              questions: getRange(6)
            }
          ],
          addQuestionsToNewBlocks: [],
          removeEligibilityFromBlockDefinitions: [
            {
              block: Block.Second,
              questions: getRange(1),
            },
          ],
          removeQuestionsFromBlock: [
            {
              block: Block.First,
              questions: getRange(0),
            }
          ]
        }
      ]
    }

    console.log(JSON.stringify(run, null, 2))


    for (let i = 0; i < run.passes.length; i++) {
      const pass = run.passes[i]

      await test.step(`run v${i+1}`, async () => {
        await civiformAdminActor.editProgram()

        if (pass.removeEligibilityFromBlockDefinitions.length > 0) {
          await civiformAdminActor.removeEligibilityFromBlockDefinitions(pass.removeEligibilityFromBlockDefinitions)
        }

        if (pass.removeQuestionsFromBlock.length > 0) {
          await civiformAdminActor.removeQuestionsFromBlock(pass.removeQuestionsFromBlock)
        }

        if (pass.addQuestionsToNewBlocks.length > 0) {
          await civiformAdminActor.addQuestionsToNewBlocks(pass.addQuestionsToNewBlocks)
        }

        if (pass.addQuestionsToExistingBlocks.length > 0) {
          await civiformAdminActor.addQuestionsToExistingBlocks(pass.addQuestionsToExistingBlocks)
        }

        await civiformAdminActor.publish()
      })
    }

    await civiformAdminActor.closeBrowserContext()
  })

  test('error2', async ({browser}) => {
    const questionList = [...qs]

    function getRange(start: number, end?: number) : QuestionArray {
      const results = questionList.splice(start, end || 1)
      console.log(results)
      console.log(questionList)
      return results
    }

    const civiformAdminActor = await FastForwardCiviformAdminActor.create(programName, browser)

    await test.step('Login actors', async () => {
      await civiformAdminActor.login()
    })

    // const range1 = getRange(0)
    // const range2 = getRange(0)
    // const range3 = getRange(0, 5)

    const run : Run = {
      passes: []
    }

    let range1 = getRange(0)
    let range2 = getRange(0)
    let oldRange : QuestionArray = []

    for (let i = 1; i <= 4; i++) {
      
      run.passes.push({
        removeEligibilityFromBlockDefinitions: [],
        removeQuestionsFromBlock: [
          {
            block: `Screen ${i}`,
            questions: oldRange,
          }
        ],
        addQuestionsToExistingBlocks : [],
        addQuestionsToNewBlocks: [          
          {
            block: `Screen ${i+1}`,
            questions: range1,
          }
        ]
      })

      oldRange = range1
      range1 = range2
      range2 = getRange(0)
    }


    // const run : Run = {
    //   passes: [
    //     {
    //       removeEligibilityFromBlockDefinitions: [],
    //       removeQuestionsFromBlock: [],
    //       addQuestionsToExistingBlocks : [
    //         {
    //           block: Block.First,
    //           questions: range1,
    //         },
    //       ],
    //       addQuestionsToNewBlocks: [
    //         {
    //           block: Block.Second,
    //           questions: range2,
    //         },
    //       ]
    //     }, 
    //     {
    //       removeEligibilityFromBlockDefinitions: [],
    //       removeQuestionsFromBlock: [
    //         {
    //           block: Block.First,
    //           questions: range1,
    //         }
    //       ],
    //       addQuestionsToExistingBlocks: [
    //         {
    //           block: Block.First,
    //           questions: range3
    //         }
    //       ],
    //       addQuestionsToNewBlocks: [],
    //     }
    //   ]
    // }

    console.log(JSON.stringify(run, null, 2))


    for (let i = 0; i < run.passes.length; i++) {
      const pass = run.passes[i]

      await test.step(`run v${i+1}`, async () => {
        await civiformAdminActor.editProgram()

        if (pass.removeEligibilityFromBlockDefinitions.length > 0) {
          await civiformAdminActor.removeEligibilityFromBlockDefinitions(pass.removeEligibilityFromBlockDefinitions)
        }

        if (pass.removeQuestionsFromBlock.length > 0) {
          await civiformAdminActor.removeQuestionsFromBlock(pass.removeQuestionsFromBlock)
        }

        if (pass.addQuestionsToExistingBlocks.length > 0) {
          await civiformAdminActor.addQuestionsToExistingBlocks(pass.addQuestionsToExistingBlocks)
        }

        if (pass.addQuestionsToNewBlocks.length > 0) {
          await civiformAdminActor.addQuestionsToNewBlocks(pass.addQuestionsToNewBlocks)
        }


        // await civiformAdminActor.publish()
      })
    }

    await civiformAdminActor.closeBrowserContext()
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
    const context = await browser.newContext({
      recordVideo: {
        dir: 'tmp/videos/',
      },
    })
    return new FastForwardCiviformAdminActor(
      programName,
      await context.newPage(),
    )
  }

  /**
   * Get the playwright page object bound to this actor
   */
  getPage(): Page {
    return this.page
  }

  /**
   * Close to cleanup at the end of the test
   */
  async closeBrowserContext() {
    await this.page.context().close()
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
    await test.step('editProgram', async () => {
      await this.adminPrograms.editProgram(this.programName)
    })
  }

  /**
   * Set the program visibility as disabled
   */
  async disableProgram() {
    await this.adminPrograms.editProgram(
      this.programName,
      ProgramVisibility.DISABLED,
    )
  }

  /**
   * Publishes a program
   */
  async publish() {
    await test.step('publish', async () => {
      await this.adminPrograms.publishAllDrafts()
    })
  }

  /**
   * List of questions to add to the system
   * @param {Array<Question>} questions
   */
  async addQuestions(questions: QuestionArray) {
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

    await this.removeEligibilityFromBlockDefinition(blockDef)

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

  async removeEligibilityFromBlockDefinitions(blockDefs: BlockDefinition[]) {
    for (const blockDef of blockDefs) {
      await test.step(`Navigate to edit block eligibility page for block ${blockDef.block}`, async () => {
        await this.adminPrograms.goToEditBlockEligibilityPredicatePage(
          this.programName,
          blockDef.block,
        )
      })

      await this.removeEligibilityFromBlockDefinition(blockDef)
    }
  }

  private async removeEligibilityFromBlockDefinition(
    blockDef: BlockDefinition,
  ) {
    await test.step(`Remove eligibility if already configured on block ${blockDef.block}`, async () => {
      const removeExistingEligibilityButtonLocator = this.page.getByRole(
        'button',
        {name: 'Remove existing eligibility condition'},
      )

      if (await removeExistingEligibilityButtonLocator.isEnabled()) {
        await this.adminPredicates.clickRemovePredicateButton('eligibility')
      }
    })
  }

  /**
   * Define the desired block states with the list of questions to remove from the block
   * @param {Array<BlockDefinition>} blockDefs
   */
  async removeQuestionsFromBlock(blockDefs: BlockDefinition[]) {
    for (const blockDef of blockDefs) {
      for (const question of blockDef.questions)
        await test.step(`remove question ${question} from block ${blockDef.block}`, async () => {
          await this.adminPrograms.removeQuestionFromProgram(
            this.programName,
            blockDef.block,
            blockDef.questions,
          )
        })
    }
  }
}

interface Pass {
  removeEligibilityFromBlockDefinitions: BlockDefinition[],
  removeQuestionsFromBlock: BlockDefinition[],
  addQuestionsToExistingBlocks: BlockDefinition[],
  addQuestionsToNewBlocks: BlockDefinition[],
}

interface Run {
  passes: Pass[]
}

interface BlockDefinition {
  block: string
  questions: QuestionArray
  eligibilityValue?: string
}

type QuestionArray = string[]

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
