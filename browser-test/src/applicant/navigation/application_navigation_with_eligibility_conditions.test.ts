import {test} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
} from '../../support'
import {Eligibility} from '../../support/admin_programs'

test.describe('Applicant navigation flow', () => {
  test.describe('navigation with eligibility conditions', () => {
    // Create a program with 2 questions and an eligibility condition.
    const fullProgramName = 'Test program for eligibility navigation flows'
    const eligibilityQuestionId = 'nav-predicate-number-q'

    test.beforeEach(
      async ({page, adminQuestions, adminPredicates, adminPrograms}) => {
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

      await applicantQuestions.expectQuestionHasNoEligibilityIndicator(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
    })

    test('shows not eligible with ineligible answer', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ false,
      )
      await applicantQuestions.clickApplyProgramButton(fullProgramName)

      await validateToastMessage(page, 'may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(
        page,
        'application-ineligible-same-application',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test(
      'North Star shows ineligible on home page',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await applicantQuestions.applyProgram(fullProgramName)

        await test.step('fill out application and submit', async () => {
          await applicantQuestions.answerNumberQuestion('1')
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectIneligiblePage()
        })

        await test.step('verify question is marked ineligible', async () => {
          await applicantQuestions.gotoApplicantHomePage()
          await applicantQuestions.seeEligibilityTag(
            fullProgramName,
            /* isEligible= */ false,
          )

          await validateScreenshot(
            page,
            'ineligible-home-page-program-tagnorthstar',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })
        await disableFeatureFlag(page, 'north_star_applicant_ui')
      },
    )

    test(
      'North Star shows eligible on home page',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await applicantQuestions.applyProgram(fullProgramName)

        await test.step('fill out application and submit', async () => {
          await applicantQuestions.answerNumberQuestion('5')
          await applicantQuestions.clickContinue()
        })

        await test.step('verify question is marked eligible', async () => {
          await applicantQuestions.gotoApplicantHomePage()
          await applicantQuestions.seeEligibilityTag(
            fullProgramName,
            /* isEligible= */ true,
          )

          await validateScreenshot(
            page,
            'eligible-home-page-program-tagnorthstar',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })
        await disableFeatureFlag(page, 'north_star_applicant_ui')
      },
    )

    test('shows may be eligible with an eligible answer', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and without submitting.
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, 'may qualify')
      await validateScreenshot(
        page,
        'eligible-toast',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )

      // Verify the question is marked eligible
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ true,
      )
      await validateScreenshot(
        page,
        'eligible-home-page-program-tag',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)

      // Go back to in progress application and submit.
      await applicantQuestions.applyProgram(fullProgramName)
      await applicantQuestions.answerEmailQuestion('test@test.com')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ true,
      )
    })

    test(
      'North Star shows may be eligible toast with an eligible answer',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await applicantQuestions.applyProgram(fullProgramName)

        // Fill out application and without submitting.
        await applicantQuestions.answerNumberQuestion('5')
        await applicantQuestions.clickContinue()
        await validateToastMessage(page, 'may qualify')
        await validateScreenshot(
          page,
          'north-star-eligible-toast',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
        await disableFeatureFlag(page, 'north_star_applicant_ui')
      },
    )

    test('shows not eligible with ineligible answer from another application', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      const overlappingOneQProgramName =
        'Test program with one overlapping question for eligibility navigation flows'

      // Add the partial program.
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

      await applicantQuestions.applyProgram(overlappingOneQProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()

      // Verify the question is marked ineligible.
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
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await validateToastMessage(page, 'may not qualify')
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )
      await validateScreenshot(
        page,
        'application-ineligible-preexisting-data',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('shows not eligible upon submit with ineligible answer', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ false,
      )
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )

      // Answer the other question.
      await applicantQuestions.clickContinue()
      await applicantQuestions.answerEmailQuestion('email@email.com')

      // Submit and expect to be told it's ineligible.
      await applicantQuestions.clickNext()
      await applicantQuestions.clickSubmit()
      await applicantQuestions.expectIneligiblePage()
    })

    test('shows not eligible upon submit with ineligible answer with gating eligibility', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application and submit.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()

      // Verify the question is marked ineligible.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ false,
      )
      await applicantQuestions.clickApplyProgramButton(fullProgramName)
      await applicantQuestions.expectQuestionIsNotEligible(
        AdminQuestions.NUMBER_QUESTION_TEXT,
      )

      // Answer the other question.
      await applicantQuestions.clickContinue()
      await applicantQuestions.answerEmailQuestion('email@email.com')

      // Submit and expect to be told it's ineligible.
      await applicantQuestions.clickNext()
      await applicantQuestions.clickSubmit()
      await applicantQuestions.expectIneligiblePage()
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

      // Add a question with markdown in the question text
      await loginAsAdmin(page)
      await adminQuestions.addTextQuestion({
        questionName: questionName,
        questionText:
          'This is a _question_ with some [markdown](https://www.example.com) and \n line \n\n breaks',
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

      // Apply to the program and answer the eligibility question with an ineligible answer
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('bar')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectIneligiblePage()
      await validateScreenshot(
        page,
        'ineligible-page-with-markdown',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows may be eligible with nongating eligibility', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await loginAsAdmin(page)
      await adminPrograms.createNewVersion(fullProgramName)
      await adminPrograms.setProgramEligibility(
        fullProgramName,
        Eligibility.IS_NOT_GATING,
      )
      await adminPrograms.publishProgram(fullProgramName)
      await logout(page)

      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application without submitting.
      await applicantQuestions.answerNumberQuestion('5')
      await applicantQuestions.clickNext()

      // Verify the question is marked eligible
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeEligibilityTag(
        fullProgramName,
        /* isEligible= */ true,
      )
    })

    test('does not show not eligible with nongating eligibility', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await loginAsAdmin(page)
      await adminPrograms.createNewVersion(fullProgramName)
      await adminPrograms.setProgramEligibility(
        fullProgramName,
        Eligibility.IS_NOT_GATING,
      )
      await adminPrograms.publishProgram(fullProgramName)
      await logout(page)

      await applicantQuestions.applyProgram(fullProgramName)

      // Fill out application without submitting.
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickNext()

      // Verify that there's no indication of eligibility.
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeNoEligibilityTags(fullProgramName)

      // Go back to in progress application and submit.
      await applicantQuestions.applyProgram(fullProgramName)
      await applicantQuestions.answerEmailQuestion('test@test.com')
      await applicantQuestions.clickNext()
      await validateToastMessage(page, '')
      await applicantQuestions.submitFromReviewPage()
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.seeNoEligibilityTags(fullProgramName)
    })
  })
})
