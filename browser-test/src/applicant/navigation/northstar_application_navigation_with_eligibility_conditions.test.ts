import {test} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {Eligibility} from '../../support/admin_programs'

test.describe('Applicant navigation flow', {tag: ['@northstar']}, () => {
  test.describe('navigation with eligibility conditions', () => {
    // Create a program with 2 questions and an eligibility condition.
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

        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(fullProgramName)
        await logout(page)
      },
    )

    test('does not show Not Eligible when there is no answer', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await applicantQuestions.clickReview(/* northStarEnabled= */ true)
      await applicantQuestions.expectMayNotBeEligibleAlertToBeHidden()
    })

    test('shows not eligible with ineligible answer', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        fullProgramName,
        /* northStarEnabled= */ true,
      )

      await test.step('fill out application and submit', async () => {
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      })

      // Verify the question is marked ineligible.
      await test.step('Verify application is marked ineligible', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ false,
        )
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
    })

    test('shows may be eligible with an eligible answer', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        fullProgramName,
        /* northStarEnabled= */ true,
      )

      await test.step('fill out application without submitting and verify message on review page', async () => {
        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
        await validateScreenshot(
          page,
          'application-eligible-review-page',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })

      await test.step('verify tag on home page', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ true,
        )
      })

      await test.step('finish submitting application and verify eligibility message', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerEmailQuestion('test@test.com')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })
    })

    test('shows not eligible with ineligible answer from another application', async ({
      page,
      adminPrograms,
      applicantQuestions,
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

      await test.step('verify overlapping programs are ineligible', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await validateScreenshot(
          page,
          'ineligible-home-page-program-tag',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ false,
        )
      })
      await test.step('verify ineligibility message on review page', async () => {
        await applicantQuestions.clickApplyProgramButton(fullProgramName)
        await applicantQuestions.clickReview(/* northStarEnabled= */ true)
        await applicantQuestions.expectMayNotBeEligibileAlertToBeVisible()
        await applicantQuestions.expectIneligibleQuestionInReviewPageAlert(
          AdminQuestions.NUMBER_QUESTION_TEXT,
        )
        await validateAccessibility(page)
      })
    })

    test('shows not eligible upon submit with ineligible answer with gating eligibility', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('setup program with gating eligibility', async () => {
        await loginAsAdmin(page)
        await adminPrograms.createNewVersion(fullProgramName)
        await adminPrograms.setProgramEligibility(
          fullProgramName,
          Eligibility.IS_GATING,
        )
        await adminPrograms.publishProgram(fullProgramName)
        await logout(page)
      })

      await test.step('fill out application with ineligible answer and submit', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      })

      await test.step('verify the question is marked ineligible', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ false,
        )
      })

      await test.step('answer the other question', async () => {
        await applicantQuestions.clickApplyProgramButton(fullProgramName)
        await applicantQuestions.answerEmailQuestion('email@email.com')
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

    test('shows may be eligible with nongating eligibility', async ({
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

      await test.step('verify home page card is marked not-eligible', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ true,
        )
      })

      await test.step('verify eligibility banner shows on pages', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerEmailQuestion('test@test.com')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
        await applicantQuestions.clickSubmitApplication()
      })
    })

    test('does not show not eligible with nongating eligibility', async ({
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
        await applicantQuestions.expectMayNotBeEligibleAlertToBeHidden()
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayNotBeEligibleAlertToBeHidden()
      })

      await test.step("verify that there's no indication of eligibility", async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })

      await test.step('go back to in progress applications and validate no eligibility alert and submit', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerEmailQuestion('test@test.com')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectMayNotBeEligibleAlertToBeHidden()
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeNoEligibilityTags(fullProgramName)
      })
    })

    test('Shows ineligible tag on home page program cards', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        fullProgramName,
        /* northStarEnabled= */ true,
      )

      await test.step('fill out application and submit', async () => {
        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      })

      await test.step('verify question is marked ineligible', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ false,
        )
      })
    })

    test('Shows eligible on home page', async ({applicantQuestions}) => {
      await test.step('fill out application and submit', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.clickContinue()
      })

      await test.step('verify program is marked eligible', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ true,
        )
      })
    })

    test('shows not eligible alert on review page with ineligible answer', async ({
      applicantQuestions,
    }) => {
      await test.step('fill out application and submit', async () => {
        await applicantQuestions.applyProgram(
          fullProgramName,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.answerNumberQuestion('1')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      })

      await test.step('verify program is marked ineligible', async () => {
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.seeEligibilityTag(
          fullProgramName,
          /* isEligible= */ false,
        )

        await applicantQuestions.clickApplyProgramButton(fullProgramName)

        // Navigate to review page
        await applicantQuestions.clickBack()
        await applicantQuestions.clickBack()

        await applicantQuestions.expectMayNotBeEligibileAlertToBeVisible()
      })
    })
  })
})
