import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {Eligibility} from '../../support/admin_programs'
import {CardSectionName} from '../../support/applicant_program_list'

test.describe('Applicant navigation flow', {tag: ['@northstar']}, () => {
  test.describe('navigation with eligibility conditions', () => {
    // Create a program with 3 questions and an eligibility condition.
    const fullProgramName = 'Test program for eligibility navigation flows'
    const eligibilityQuestionId = 'nav-predicate-number-q'

    test.beforeEach(
      async ({page, adminQuestions, adminPredicates, adminPrograms}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await loginAsAdmin(page)

        await adminQuestions.addNumberQuestion({
          questionName: eligibilityQuestionId,
        })
        await adminQuestions.addEmailQuestion({
          questionName: 'nav-predicate-email-q',
        })
        await adminQuestions.addTextQuestion({
          questionName: 'nav-predicate-text-q',
        })

        // Add the full program.
        await adminPrograms.addProgram(fullProgramName)
        await adminPrograms.editProgramBlock(
          fullProgramName,
          'first description',
          ['nav-predicate-number-q'],
        )
        await adminPrograms.goToEditBlockEligibilityPredicatePage(
          fullProgramName,
          'Screen 1',
        )
        await adminPredicates.addPredicates({
          questionName: 'nav-predicate-number-q',
          scalar: 'number',
          operator: 'is equal to',
          value: '5',
        })

        await adminPrograms.addProgramBlock(
          fullProgramName,
          'second description',
          ['nav-predicate-email-q'],
        )

        await adminPrograms.addProgramBlock(
          fullProgramName,
          'third description',
          ['nav-predicate-text-q'],
        )

        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(fullProgramName)
        await logout(page)
      },
    )

    test("does not show 'not eligible' when there is no answer", async ({
      applicantQuestions,
      applicantProgramOverview,
    }) => {
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        fullProgramName,
      )
      await applicantQuestions.clickReview(/* northStarEnabled= */ true)
      await applicantQuestions.expectMayNotBeEligibleAlertToBeHidden()
    })

    test("shows 'not eligible' alerts with ineligible answer and gating eligibility", async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        fullProgramName,
        /* northStarEnabled= */ true,
      )

      await test.step('fill out application with ineligible answer', async () => {
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      })

      await test.step('Verify no eligibility tags on in-progress application', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })

      await test.step('verify not eligible alert is shown on review page', async () => {
        await applicantQuestions.clickApplyProgramButton(fullProgramName)
        await applicantQuestions.clickReview(/* northStarEnabled= */ true)

        await applicantQuestions.expectMayNotBeEligibileAlertToBeVisible()
        await applicantQuestions.expectIneligibleQuestionInReviewPageAlert(
          AdminQuestions.NUMBER_QUESTION_TEXT,
        )
        await validateScreenshot(
          page,
          'application-ineligible-review-page',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
        await validateAccessibility(page)
      })

      await test.step('answer the other question', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.clickApplyProgramButton(fullProgramName)
        await applicantQuestions.answerEmailQuestion('email@email.com')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerTextQuestion('text!')
      })

      await test.step("submit and expect to be told it's ineligible", async () => {
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayNotBeEligibileAlertToBeVisible()
        await applicantQuestions.expectIneligibleQuestionInReviewPageAlert(
          AdminQuestions.NUMBER_QUESTION_TEXT,
        )
        await applicantQuestions.clickSubmitApplication()
        await applicantQuestions.expectMayNotBeEligibileAlertToBeVisible()
        await applicantQuestions.expectIneligibleQuestionInReviewPageAlert(
          AdminQuestions.NUMBER_QUESTION_TEXT,
        )
      })
    })

    test("shows 'eligible' alerts with an eligible answer and gating eligibility", async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        fullProgramName,
        /* northStarEnabled= */ true,
      )

      await test.step('fill out application without submitting and verify message on edit page', async () => {
        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
        await validateScreenshot(
          page,
          'application-eligible-edit-page',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })

      await test.step('verify eligibility banner not visible on subsequent edit pages', async () => {
        await applicantQuestions.answerEmailQuestion('test@test.com')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayBeEligibileAlertToBeHidden()
      })

      await test.step('fill out application without submitting and verify message on review page', async () => {
        await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
        await validateScreenshot(
          page,
          'application-eligible-review-page',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })

      await test.step('verify no eligibility tags on in-progress application card', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })

      await test.step('finish submitting application and verify eligibility message', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.answerTextQuestion('text!')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      await test.step('verify no eligibility tags on submitted application', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })
    })

    test("shows 'not eligible' alerts and tags on program with gating eligibility with ineligible answer from another application", async ({
      page,
      adminPrograms,
      applicantQuestions,
      applicantProgramList,
      applicantProgramOverview,
    }) => {
      const overlappingOneQProgramName =
        'Test program with one overlapping question for eligibility navigation flows'

      await test.step('add program to partially complete', async () => {
        await loginAsAdmin(page)
        await adminPrograms.addProgram(overlappingOneQProgramName)
        await adminPrograms.editProgramBlock(
          overlappingOneQProgramName,
          'first description',
          [eligibilityQuestionId],
        )
        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(overlappingOneQProgramName)
        await logout(page)
      })

      await test.step('Answer overlapping question', async () => {
        await applicantQuestions.applyProgram(
          overlappingOneQProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
      })

      await test.step('verify ineligible tag on main program', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ false,
        )
        await validateScreenshot(
          applicantProgramList.getCardLocator(
            CardSectionName.ProgramsAndServices,
            fullProgramName,
          ),
          'ineligible-home-page-program-card-with-tag',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })
      await test.step('verify ineligibility message on review page of overlapping program', async () => {
        await applicantQuestions.clickApplyProgramButton(fullProgramName)
        await applicantProgramOverview.startApplicationFromProgramOverviewPage(
          fullProgramName,
        )
        await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        await applicantQuestions.expectMayNotBeEligibileAlertToBeVisible()
        await applicantQuestions.expectIneligibleQuestionInReviewPageAlert(
          AdminQuestions.NUMBER_QUESTION_TEXT,
        )
        await validateAccessibility(page)
      })
    })

    test("shows 'eligible' tags on program with gating eligibility with eligible answer from another application", async ({
      page,
      adminPrograms,
      applicantQuestions,
      applicantProgramList,
    }) => {
      const overlappingOneQProgramName =
        'Test program with one overlapping question for eligibility navigation flows'

      await test.step('add program to partially complete', async () => {
        await loginAsAdmin(page)
        await adminPrograms.addProgram(overlappingOneQProgramName)
        await adminPrograms.editProgramBlock(
          overlappingOneQProgramName,
          'first description',
          [eligibilityQuestionId],
        )
        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(overlappingOneQProgramName)
        await logout(page)
      })

      await test.step('Answer overlapping question', async () => {
        await applicantQuestions.applyProgram(
          overlappingOneQProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.clickContinue()
      })

      await test.step('verify eligible tag on main program', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ true,
        )
        await validateScreenshot(
          applicantProgramList.getCardLocator(
            CardSectionName.ProgramsAndServices,
            fullProgramName,
          ),
          'eligible-home-page-program-card-with-tag',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
        await validateAccessibility(page)
      })
    })

    test("shows 'eligible' alerts with non-gating eligibility", async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('set up program with non-gating eligibility', async () => {
        await loginAsAdmin(page)
        await adminPrograms.createNewVersion(fullProgramName)
        await adminPrograms.setProgramEligibility(
          fullProgramName,
          Eligibility.IS_NOT_GATING,
        )
        await adminPrograms.publishProgram(fullProgramName)
        await logout(page)
      })

      await test.step('fill out application without submitting', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.clickContinue()
      })

      await test.step('verify applicant home page card does not have tags for in-progress application', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })

      await test.step('verify eligibility banner shows on review page', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.answerEmailQuestion('test@test.com')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerTextQuestion('text!')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
        await expect(
          page.getByLabel('Success: You may be eligible for this program'),
        ).toBeVisible()
        await applicantQuestions.clickSubmitApplication()
      })

      await test.step('verify applicant home page card does not have tags for submitted application', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })
    })

    test("does not show 'not eligible' alerts with non-gating eligibility", async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('set up program with nongating eligibility', async () => {
        await loginAsAdmin(page)
        await adminPrograms.createNewVersion(fullProgramName)
        await adminPrograms.setProgramEligibility(
          fullProgramName,
          Eligibility.IS_NOT_GATING,
        )
        await adminPrograms.publishProgram(fullProgramName)
        await logout(page)
      })

      await test.step('fill out application with ineligible answer without submitting', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayNotBeEligibleAlertToBeHidden()
      })

      await test.step('verify no eligibility tags on applicant home page card for in-progress application', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })

      await test.step('go back to in-progress application and validate no eligibility alert and submit', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.answerEmailQuestion('test@test.com')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerTextQuestion('text!')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayNotBeEligibleAlertToBeHidden()
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })
    })

    test("does not show 'not eligible' tags on program with non-gating eligibility with ineligible answer from another application", async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      const overlappingOneQProgramName =
        'Test program with one overlapping question for eligibility navigation flows'

      await test.step('set up program with nongating eligibility', async () => {
        await loginAsAdmin(page)
        await adminPrograms.createNewVersion(fullProgramName)
        await adminPrograms.setProgramEligibility(
          fullProgramName,
          Eligibility.IS_NOT_GATING,
        )
        await adminPrograms.publishProgram(fullProgramName)
      })

      await test.step('add program to partially complete', async () => {
        await adminPrograms.addProgram(overlappingOneQProgramName)
        await adminPrograms.editProgramBlock(
          overlappingOneQProgramName,
          'first description',
          [eligibilityQuestionId],
        )
        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(overlappingOneQProgramName)
        await logout(page)
      })

      await test.step('Answer overlapping question', async () => {
        await applicantQuestions.applyProgram(
          overlappingOneQProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
      })

      await test.step('verify no ineligible tag on main program', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
        await validateAccessibility(page)
      })
    })

    test("shows 'eligible' tags on program with non-gating eligibility with eligible answer from another application", async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      const overlappingOneQProgramName =
        'Test program with one overlapping question for eligibility navigation flows'

      await test.step('set up program with nongating eligibility', async () => {
        await loginAsAdmin(page)
        await adminPrograms.createNewVersion(fullProgramName)
        await adminPrograms.setProgramEligibility(
          fullProgramName,
          Eligibility.IS_NOT_GATING,
        )
        await adminPrograms.publishProgram(fullProgramName)
      })

      await test.step('add program to partially complete', async () => {
        await adminPrograms.addProgram(overlappingOneQProgramName)
        await adminPrograms.editProgramBlock(
          overlappingOneQProgramName,
          'first description',
          [eligibilityQuestionId],
        )
        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(overlappingOneQProgramName)
        await logout(page)
      })

      await test.step('Answer overlapping question', async () => {
        await applicantQuestions.applyProgram(
          overlappingOneQProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.clickContinue()
      })

      await test.step('verify eligible tag on main program', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ true,
        )
        await validateAccessibility(page)
      })
    })

    test('ineligible page renders markdown', async ({
      page,
      adminQuestions,
      applicantQuestions,
      adminPredicates,
      adminPrograms,
    }) => {
      const questionName = 'question-with-markdown'
      const programName = 'Program with markdown question'

      await test.step('Set up program with markdown in question text', async () => {
        await loginAsAdmin(page)
        await adminQuestions.addTextQuestion({
          questionName: questionName,
          questionText:
            'This is a _question_ with some [markdown](https://www.example.com) and \n line \n\n breaks',
          markdown: true,
        })
        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlock(programName, 'first description', [
          questionName,
        ])
        // Add an eligiblity condition on the markdown question
        await adminPrograms.goToEditBlockEligibilityPredicatePage(
          programName,
          'Screen 1',
        )
        await adminPredicates.addPredicates({
          questionName: questionName,
          scalar: 'text',
          operator: 'is equal to',
          value: 'foo',
        })
        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(programName)
        await logout(page)
      })

      await test.step('apply to program with ineligible answer and verify text', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerTextQuestion('bar')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectIneligiblePage(
          /* northStarEnabled= */ true,
        )
        await validateScreenshot(
          page,
          'northstar-ineligible-page-with-markdown',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })
    })
  })
})
