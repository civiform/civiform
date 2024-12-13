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
  enableFeatureFlag,
  disableFeatureFlag,
} from '../support'
import {ProgramVisibility, QuestionSpec} from '../support/admin_programs'
import {
  ApplicantProgramList,
  CardSectionName,
} from '../support/applicant_program_list'
import {Browser, Locator, Page} from '@playwright/test'

test.describe(
  'Application Version Fast-Forward Flow',
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page, request}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Clear database', async () => {
        await request.post('/dev/seed/clear')
      })
    })

    test('all major steps - fast forward flag disabled', async ({browser}) => {
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

      await disableFeatureFlag(
        civiformAdminActor.getPage(),
        'FASTFORWARD_ENABLED',
      )
      await disableFeatureFlag(applicantActor.getPage(), 'FASTFORWARD_ENABLED')
      await disableFeatureFlag(
        programAdminActor.getPage(),
        'FASTFORWARD_ENABLED',
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

      This test captures a multi-state, multi-actor workflow in order to both verify
      the current behavior and in preparation for the future state when the draft
      application is automatically fast-forwarded to the latest active version of a program.

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
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardHeadingLocator(
              CardSectionName.ProgramsAndServices,
            ),
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
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()

          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV1)
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
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
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
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()
          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV1)
        })

        await test.step('As applicant - navigate to a question edit page and wait there', async () => {
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
          await applicantActor.gotoReviewPage()
          await applicantActor.submitApplication()
        })

        await test.step('As applicant - check program list has one submitted application for program v1', async () => {
          await applicantActor.gotoApplicantHomePage()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()

          // Once submitted the program id on the edit button will be for the latest version
          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV3)
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

        await test.step('does not see submitted application with questions from program version v2 or v3', async () => {
          const cardLocator = programAdminActor.getCardLocator()
          await expect(cardLocator).toHaveCount(1)

          const cardButton = cardLocator.getByRole('link', {name: 'View'})
          expect(
            await programAdminActor.parseProgramIdFromLocator(cardButton),
          ).not.toBe(programIdV2)

          expect(
            await programAdminActor.parseProgramIdFromLocator(cardButton),
          ).not.toBe(programIdV3)
        })
      })
    })

    test('all major steps - fast forward flag enabled', async ({browser}) => {
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

      await enableFeatureFlag(
        civiformAdminActor.getPage(),
        'FASTFORWARD_ENABLED',
      )
      await enableFeatureFlag(applicantActor.getPage(), 'FASTFORWARD_ENABLED')
      await enableFeatureFlag(
        programAdminActor.getPage(),
        'FASTFORWARD_ENABLED',
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

      Program: v3       Remove (E), add (H), no-change (G, B, A, E, F)
        Block: First
          Question: G
        Block: Second
          Question: B  ---> Eligibility
        Block: Third
          Question: A
        Block: Fourth
          Question: H ---> Eligibility
        Block: Fifth
          Question: F
    */

      /*
      High level overview

      This test captures a multi-state, multi-actor workflow in order to both verify
      the current behavior and in preparation for the future state when the draft
      application is automatically fast-forwarded to the latest active version of a program.

      - Civiform Admin creates program v1
      - Applicant fills out application to program v1; does not submit
      - Civiform Admin creates program v2
      - Applicant
          * Goes to application review page; now on program v2
          * Message displayed to user
      - Applicant goes into the edit appliction page and stays in there
      - Civiform Admin creates program v3
      - Applicant
          * Still on the edit page
          * Attempt to submit application for program v3
          * Taken to review page; now on program v3
          * Message displayed to user
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
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardHeadingLocator(
              CardSectionName.ProgramsAndServices,
            ),
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
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()

          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV1)
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

          await test.step('remove eligibility from questions', async () => {
            await civiformAdminActor.removeEligibilityFromBlockDefinitions([
              {
                block: Block.Third,
                questions: [Question.C],
              },
            ])
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
        await test.step('As applicant - check program list has one in-progress application for program v2', async () => {
          await applicantActor.gotoApplicantHomePage()

          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()
          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV2)
        })

        await test.step('As applicant - load application and verify it is on program v2', async () => {
          await applicantActor.clickApplyProgramButton()
          expect(applicantActor.getProgramIdFromApplicationReviewUrl()).toBe(
            programIdV2,
          )
        })

        await test.step('As applicant - check program list has one in-progress application for program v2', async () => {
          await applicantActor.gotoApplicantHomePage()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()
          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV2)
        })

        await test.step('As applicant - navigate to a question edit page and wait there', async () => {
          await applicantActor.clickApplyProgramButton()

          expect(applicantActor.getProgramIdFromApplicationReviewUrl()).toBe(
            programIdV2,
          )
          await applicantActor.reanswerQuestion(Question.G)
          await applicantActor.reanswerQuestion(Question.F)
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

          await test.step('remove questions from program', async () => {
            await civiformAdminActor.removeQuestionsFromBlock([
              {
                block: Block.Fourth,
                questions: [Question.E],
              },
            ])
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
        await applicantActor.getPage().reload()

        await test.step('As applicant - submit application', async () => {
          await applicantActor.continueAnswerApplicationQuestions()
          await applicantActor.answerQuestions([Question.H])
          await applicantActor.submitApplication()
        })

        await test.step('As applicant - check program list has one submitted application for program v3', async () => {
          await applicantActor.gotoApplicantHomePage()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()

          // Once submitted the program id on the edit button will be for the latest version
          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV3)
        })
      })

      // Program Admin can view applications submitted for program v3
      await test.step('As program admin - active program v3', async () => {
        await test.step('Log in program admin', async () => {
          // Log in Program Admin here and not at the start. If logged in before the program is created by
          // a civiform admin, the program admin won't have permissions to the program.
          await programAdminActor.login()
          await programAdminActor.viewApplications()
        })

        await test.step('sees submitted application with questions from program version v3', async () => {
          const cardLocator = programAdminActor.getCardLocator()
          await expect(cardLocator).toHaveCount(1)

          const cardButton = cardLocator.getByRole('link', {name: 'View'})
          expect(
            await programAdminActor.parseProgramIdFromLocator(cardButton),
          ).toBe(programIdV3)
        })

        await test.step('does not see submitted application with questions from program version v1 or v2', async () => {
          const cardLocator = programAdminActor.getCardLocator()
          await expect(cardLocator).toHaveCount(1)

          const cardButton = cardLocator.getByRole('link', {name: 'View'})
          expect(
            await programAdminActor.parseProgramIdFromLocator(cardButton),
          ).not.toBe(programIdV1)

          expect(
            await programAdminActor.parseProgramIdFromLocator(cardButton),
          ).not.toBe(programIdV2)
        })
      })

      await civiformAdminActor.closeBrowserContext()
      await applicantActor.closeBrowserContext()
      await programAdminActor.closeBrowserContext()
    })

    test('all major steps - fast forward flag enabled - disabled visibility enabled', async ({
      browser,
    }) => {
      const programName = 'program-fastforward-example'

      const civiformAdminActor = await FastForwardCiviformAdminActor.create(
        programName,
        browser,
      )
      const applicantActor = await FastForwardApplicantActor.create(
        programName,
        browser,
      )

      await enableFeatureFlag(
        civiformAdminActor.getPage(),
        'FASTFORWARD_ENABLED',
      )
      await enableFeatureFlag(applicantActor.getPage(), 'FASTFORWARD_ENABLED')

      await enableFeatureFlag(
        civiformAdminActor.getPage(),
        'DISABLED_VISIBILITY_CONDITION_ENABLED',
      )
      await enableFeatureFlag(
        applicantActor.getPage(),
        'DISABLED_VISIBILITY_CONDITION_ENABLED',
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

      Program: v3       Remove (E), add (H), no-change (G, B, A, E, F)
        Block: First
          Question: G
        Block: Second
          Question: B  ---> Eligibility
        Block: Third
          Question: A
        Block: Fourth
          Question: H ---> Eligibility
        Block: Fifth
          Question: F
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
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardHeadingLocator(
              CardSectionName.ProgramsAndServices,
            ),
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
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()

          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV1)
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

          await test.step('remove eligibility from questions', async () => {
            await civiformAdminActor.removeEligibilityFromBlockDefinitions([
              {
                block: Block.Third,
                questions: [Question.C],
              },
            ])
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
        await test.step('As applicant - check program list has one in-progress application for program v2', async () => {
          await applicantActor.gotoApplicantHomePage()

          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()
          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV2)
        })

        await test.step('As applicant - load application and verify it is on program v2', async () => {
          await applicantActor.clickApplyProgramButton()
          expect(applicantActor.getProgramIdFromApplicationReviewUrl()).toBe(
            programIdV2,
          )
        })

        await test.step('As applicant - check program list has one in-progress application for program v2', async () => {
          await applicantActor.gotoApplicantHomePage()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.ProgramsAndServices,
            ),
          ).not.toBeAttached()
          await expect(
            applicantActor.getCardSectionLocator(
              CardSectionName.MyApplications,
            ),
          ).toBeAttached()

          const headingLocator = applicantActor.getCardHeadingLocator(
            CardSectionName.MyApplications,
          )
          await expect(headingLocator).toBeAttached()
          expect(
            await applicantActor.getProgramIdFromLocator(headingLocator),
          ).toBe(programIdV2)
        })

        await test.step('As applicant - navigate to a question edit page and wait there', async () => {
          await applicantActor.clickApplyProgramButton()

          expect(applicantActor.getProgramIdFromApplicationReviewUrl()).toBe(
            programIdV2,
          )
          await applicantActor.reanswerQuestion(Question.G)
          await applicantActor.reanswerQuestion(Question.F)
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

          await test.step('remove questions from program', async () => {
            await civiformAdminActor.removeQuestionsFromBlock([
              {
                block: Block.Fourth,
                questions: [Question.E],
              },
            ])
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

            await civiformAdminActor.disableProgram()
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
        await applicantActor.getPage().reload()
        await applicantActor.waitForDisabledPage()
      })
    })
  },
)

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
    await this.adminPrograms.editProgram(this.programName)
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
  private applicantProgramList: ApplicantProgramList

  /**
   * @constructor
   * @param {string} programName name of the program
   * @param {Page} page Unique page instance for this actor
   */
  private constructor(programName: string, page: Page) {
    this.programName = programName
    this.page = page
    this.applicantQuestions = new ApplicantQuestions(page)
    this.applicantProgramList = new ApplicantProgramList(page)
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
    const context = await browser.newContext({
      recordVideo: {
        dir: 'tmp/videos/',
      },
    })
    return new FastForwardApplicantActor(programName, await context.newPage())
  }

  /**
   * Close to cleanup at the end of the test
   */
  async closeBrowserContext() {
    await this.page.context().close()
  }

  /**
   * Get the playwright page object bound to this actor
   */
  getPage(): Page {
    return this.page
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
   * Get the card section locator for the desired application status card section name
   * @param {CardSectionName} cardSectionName to find
   * @returns {Locator} Locator to the card list
   */
  getCardSectionLocator(cardSectionName: CardSectionName): Locator {
    return this.applicantProgramList.getCardSectionLocator(cardSectionName)
  }

  /**
   * Get the locator for program card heading in the desired list
   * @param {CardSectionName} cardSectionName to find
   * @returns {Locator} Locator for program card in the desired list
   */
  getCardHeadingLocator(cardSectionName: CardSectionName): Locator {
    return this.applicantProgramList.getCardHeadingLocator(
      cardSectionName,
      this.programName,
    )
  }

  /**
   * Get the eligibile tag for the desired application status card group name
   * @param {CardSectionName} cardSectionName to find
   * @returns {Locator} Locator to the eligible tag
   */
  getCardEligibleTagLocator(cardSectionName: CardSectionName): Locator {
    return this.applicantProgramList.getCardEligibleTagLocator(cardSectionName)
  }

  /**
   * Get the not eligibile tag for the desired application status card group name
   * @param {CardSectionName} cardSectionName to find
   * @returns {Locator} Locator to the not eligible tag
   */
  getCardNotEligibleTagLocator(cardSectionName: CardSectionName): Locator {
    return this.applicantProgramList.getCardNotEligibleTagLocator(
      cardSectionName,
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
      await this.applicantQuestions.applyProgram(this.programName, true)
    })
  }

  /**
   * Navigates to the application edit page
   *
   * Must be on a application review page
   * @param question
   */
  async gotoEditQuestionPage(question: Question) {
    await this.page
      .locator('li')
      .filter({hasText: question})
      .getByRole('link')
      .click()
  }

  /**
   * Navigates to the review page of an application
   *
   * Must be on an application edit page
   */
  async gotoReviewPage() {
    await this.applicantQuestions.clickReview(true)
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
        await this.applicantQuestions.clickContinue()
      })
    }
  }

  /**
   * Fills in the answer to a question that was previous answered
   *
   * Must be on a application edit page
   * @param question
   */
  async reanswerQuestion(question: Question) {
    await test.step(`reanswer question ${question}`, async () => {
      await this.page.getByLabel(`${question}`).click()
      await this.applicantQuestions.answerTextQuestion(
        `${question}-text-answer`,
      )
      await this.applicantQuestions.clickContinue()
    })
  }

  async continueAnswerApplicationQuestions() {
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
    await this.applicantQuestions.submitFromReviewPage(true)
  }

  /**
   * Extracts the program id from the supplied locators id attribute
   * @param {Locator} locator to the application card containing the .cf-application-card-####-title class
   * @returns {Promise<number>} Promise to to programId. If the ID is not found a failed assertion will end the test.
   */
  async getProgramIdFromLocator(locator: Locator): Promise<number> {
    const id = parseInt((await locator.getAttribute('data-program-id')) || '-1')
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

  /**
   * Waits for the program disabled page to load
   */
  async waitForDisabledPage(): Promise<void> {
    await this.page.waitForURL(`**/programs/${this.programName}/disabled`)
    await expect(
      this.page.getByRole('heading', {name: 'This program is no longer'}),
    ).toBeVisible()
  }
}

/**
 * This class maintains the state and logic used by the Program Admin actor
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
    const context = await browser.newContext({
      recordVideo: {
        dir: 'tmp/videos/',
      },
    })
    return new FastForwardProgramAdminActor(
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
 * Test suite internal definition of a block and its questions configuration
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
