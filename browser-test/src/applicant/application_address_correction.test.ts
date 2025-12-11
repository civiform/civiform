import {test} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from '../support'

/**
 * Tests for the address correction view and navigation to and from that view.
 */
test.describe('address correction single-block, single-address program', () => {
  const singleBlockSingleAddressProgram =
    'Address correction single-block, single-address program'
  const addressWithCorrectionQuestionId = 'address-with-correction-q'
  const addressWithoutCorrectionQuestionId = 'address-without-correction-q'
  const textQuestionId = 'text-q'
  const addressWithCorrectionText = 'With Correction'

  test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
    await loginAsAdmin(page)

    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    await test.step('Create all questions', async () => {
      await adminQuestions.addAddressQuestion({
        questionName: addressWithCorrectionQuestionId,
        questionText: addressWithCorrectionText,
      })

      await adminQuestions.addAddressQuestion({
        questionName: addressWithoutCorrectionQuestionId,
        questionText: 'Without Correction',
      })

      await adminQuestions.addTextQuestion({
        questionName: textQuestionId,
        questionText: 'text',
      })
    })

    await test.step('Create single-block, single-address program', async () => {
      await adminPrograms.addProgram(singleBlockSingleAddressProgram)

      await adminPrograms.editProgramBlockUsingSpec(
        singleBlockSingleAddressProgram,
        {
          name: 'first block',
          questions: [{name: addressWithCorrectionQuestionId}],
        },
      )

      await adminPrograms.clickAddressCorrectionToggleByName(
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(singleBlockSingleAddressProgram)
    })

    await logout(page)
  })

  if (isLocalDevEnvironment()) {
    test('can correct address single-block, single-address program', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(
          singleBlockSingleAddressProgram,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.expectTitle(
          page,
          'Address correction single-block, single-address program — 1 of 2',
        )

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickContinue()
      })

      await test.step('Validate address correction page shown', async () => {
        await applicantQuestions.expectVerifyAddressPage(true)

        await validateAccessibility(page)
        await validateScreenshot(
          page.locator('main'),
          'verify-address-with-suggestions',
          {
            mobileScreenshot: true,
          },
        )
      })

      await test.step('Confirm user can confirm address and submit', async () => {
        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
          addressWithCorrectionText,
          'Address In Area',
        )
        await applicantQuestions.clickSubmitApplication()
        await logout(page)
      })
    })

    test('Renders address correction page in right to left', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(
          singleBlockSingleAddressProgram,
          /* northStarEnabled= */ true,
        )

        await test.step('Set language to Arabic', async () => {
          await selectApplicantLanguage(page, 'ar')
        })

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await page.click('text="متابعة"')
      })

      await test.step('Validate address correction page rendered right to left', async () => {
        await validateScreenshot(
          page.locator('main'),
          'verify-address-with-suggestions-right-to-left',
          {
            mobileScreenshot: true,
          },
        )
      })
    })

    test('prompts user to edit if no suggestions are returned', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(
          singleBlockSingleAddressProgram,
          /* northStarEnabled= */ true,
        )

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Bogus Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickContinue()
      })

      await test.step('Validate address correction page shown', async () => {
        await applicantQuestions.expectVerifyAddressPage(false)

        await validateAccessibility(page)
        await validateScreenshot(
          page.locator('main'),
          'verify-address-no-suggestions',
          {
            mobileScreenshot: true,
          },
        )
      })

      await test.step('Confirm user can confirm address and submit', async () => {
        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.clickSubmitApplication()
        await logout(page)
      })
    })

    test('prompts user to edit if an error is returned from the Esri service', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        // This is currently the same as when no suggestions are returned.
        // We may change this later.
        await applicantQuestions.applyProgram(
          singleBlockSingleAddressProgram,
          /* northStarEnabled= */ true,
        )

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Error Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickContinue()
      })

      await test.step('Validate address correction page shown', async () => {
        await applicantQuestions.expectVerifyAddressPage(false)

        await validateAccessibility(page)
        await validateScreenshot(
          page.locator('main'),
          'verify-address-esri-service-error',
          {
            // Since this page is currently the same as the no-suggestions page,
            // don't get extra mobile screenshots of the same page.
            mobileScreenshot: false,
          },
        )
      })

      await test.step('Confirm user can confirm address and submit', async () => {
        // Can continue on anyway
        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.clickSubmitApplication()
      })
      await logout(page)
    })

    test('prompts user to edit if an Esri error response object is returned from the Esri service', async ({
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        // This is currently the same as when no suggestions are returned.
        // We may change this later.
        await applicantQuestions.applyProgram(
          singleBlockSingleAddressProgram,
          /* northStarEnabled= */ true,
        )

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Esri Error Response',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickContinue()
      })

      await test.step('Validate address correction page shown', async () => {
        await applicantQuestions.expectVerifyAddressPage(false)
      })
    })

    test('skips address correction screen if address exactly matches suggestions', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(
          singleBlockSingleAddressProgram,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerAddressQuestion(
          'Address In Area',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickContinue()
      })
      await test.step('Validate review page is shown', async () => {
        await applicantQuestions.expectReviewPage()
      })
      await logout(page)
    })
  }

  test('address correction page does not show if feature is disabled', async ({
    page,
    applicantQuestions,
  }) => {
    test.slow()

    await disableFeatureFlag(page, 'esri_address_correction_enabled')
    await test.step('Answer address question', async () => {
      await applicantQuestions.applyProgram(
        singleBlockSingleAddressProgram,
        /* northStarEnabled= */ true,
      )

      await applicantQuestions.answerAddressQuestion(
        '305 Harrison',
        '',
        'Seattle',
        'WA',
        '98109',
      )
      await applicantQuestions.clickContinue()
    })
    await test.step('Validate review page is shown and user can submit', async () => {
      await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
        addressWithCorrectionText,
        '305 Harrison',
      )
      await applicantQuestions.clickSubmitApplication()
    })
    await logout(page)
  })
})

if (isLocalDevEnvironment()) {
  /**
   * Tests for the address correction view and navigation to and from that view.
   */
  test.describe('address correction optional address program', () => {
    const optionalAddressProgram = 'Address correction optional address program'

    const addressWithCorrectionQuestionId = 'address-with-correction-q'
    const addressWithoutCorrectionQuestionId = 'address-without-correction-q'
    const textQuestionId = 'text-q'

    const addressWithCorrectionText = 'With Correction'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      await test.step('Create all questions', async () => {
        await adminQuestions.addAddressQuestion({
          questionName: addressWithCorrectionQuestionId,
          questionText: addressWithCorrectionText,
        })

        await adminQuestions.addAddressQuestion({
          questionName: addressWithoutCorrectionQuestionId,
          questionText: 'Without Correction',
        })

        await adminQuestions.addTextQuestion({
          questionName: textQuestionId,
          questionText: 'text',
        })
      })

      await test.step('Create optional address program', async () => {
        await adminPrograms.addProgram(optionalAddressProgram)

        await adminPrograms.editProgramBlockUsingSpec(optionalAddressProgram, {
          name: 'first block',
          questions: [
            {name: addressWithCorrectionQuestionId, isOptional: true},
          ],
        })

        await adminPrograms.clickAddressCorrectionToggleByName(
          addressWithCorrectionQuestionId,
        )

        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(optionalAddressProgram)
      })

      await logout(page)
    })

    test('skips address correction if optional address question is not answered', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(
          optionalAddressProgram,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.clickContinue()
      })
      await test.step('Validate review page is shown and user can submit', async () => {
        await applicantQuestions.expectReviewPage()

        await applicantQuestions.clickSubmitApplication()
      })
      await logout(page)
    })
  })

  /**
   * Tests for the address correction view and navigation to and from that view.
   */
  test.describe('address correction multi-block, multi-address program', () => {
    const multiBlockMultiAddressProgram =
      'Address correction multi-block, multi-address program'

    const addressWithCorrectionQuestionId = 'address-with-correction-q'
    const addressWithoutCorrectionQuestionId = 'address-without-correction-q'
    const textQuestionId = 'text-q'

    const addressWithCorrectionText = 'With Correction'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      await test.step('Create all questions', async () => {
        await adminQuestions.addAddressQuestion({
          questionName: addressWithCorrectionQuestionId,
          questionText: addressWithCorrectionText,
        })

        await adminQuestions.addAddressQuestion({
          questionName: addressWithoutCorrectionQuestionId,
          questionText: 'Without Correction',
        })

        await adminQuestions.addTextQuestion({
          questionName: textQuestionId,
          questionText: 'text',
        })
      })

      await test.step('Create multi-block, multi-address program', async () => {
        await adminPrograms.addProgram(multiBlockMultiAddressProgram)

        await adminPrograms.editProgramBlockUsingSpec(
          multiBlockMultiAddressProgram,
          {
            name: 'first block',
            questions: [
              {name: addressWithCorrectionQuestionId},
              {name: addressWithoutCorrectionQuestionId, isOptional: true},
            ],
          },
        )

        await adminPrograms.addProgramBlockUsingSpec(
          multiBlockMultiAddressProgram,
          {name: 'second block', questions: [{name: textQuestionId}]},
        )

        await adminPrograms.goToBlockInProgram(
          multiBlockMultiAddressProgram,
          'first block',
        )

        await adminPrograms.clickAddressCorrectionToggleByName(
          addressWithCorrectionQuestionId,
        )

        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(multiBlockMultiAddressProgram)
      })

      await logout(page)
    })

    test('can correct address multi-block, multi-address program', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(
          multiBlockMultiAddressProgram,
          /* northStarEnabled= */ true,
        )

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Address In Area',
          '',
          'Seattle',
          'WA',
          '98109',
          0,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Seattle',
          'WA',
          '98109',
          1,
        )
        await applicantQuestions.clickContinue()
      })
      await test.step('Validate address correction page shown and user can finish application', async () => {
        await applicantQuestions.expectVerifyAddressPage(true)
        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.answerTextQuestion('Some text')
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
          addressWithCorrectionText,
          'Address In Area',
        )
        await applicantQuestions.clickSubmitApplication()
      })
      await logout(page)
    })
  })

  /**
   * Tests for the address correction view and navigation to and from that view.
   */
  test.describe('address correction single-block, multi-address program', () => {
    const singleBlockMultiAddressProgram =
      'Address correction single-block, multi-address program'

    const addressWithCorrectionQuestionId = 'address-with-correction-q'
    const addressWithoutCorrectionQuestionId = 'address-without-correction-q'
    const textQuestionId = 'text-q'

    const addressWithCorrectionText = 'With Correction'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      await test.step('Create all questions', async () => {
        await adminQuestions.addAddressQuestion({
          questionName: addressWithCorrectionQuestionId,
          questionText: addressWithCorrectionText,
        })

        await adminQuestions.addAddressQuestion({
          questionName: addressWithoutCorrectionQuestionId,
          questionText: 'Without Correction',
        })

        await adminQuestions.addTextQuestion({
          questionName: textQuestionId,
          questionText: 'text',
        })
      })

      await test.step('Create single-block, multi-address program', async () => {
        await adminPrograms.addProgram(singleBlockMultiAddressProgram)

        await adminPrograms.editProgramBlockUsingSpec(
          singleBlockMultiAddressProgram,
          {
            name: 'first block',
            questions: [
              {name: addressWithCorrectionQuestionId},
              {name: addressWithoutCorrectionQuestionId, isOptional: true},
            ],
          },
        )

        await adminPrograms.clickAddressCorrectionToggleByName(
          addressWithCorrectionQuestionId,
        )

        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(singleBlockMultiAddressProgram)
      })

      await logout(page)
    })

    test('can correct address single-block, multi-address program', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(
          singleBlockMultiAddressProgram,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Seattle',
          'WA',
          '98109',
          0,
        )
        await applicantQuestions.answerAddressQuestion(
          '500 Harrison',
          '',
          'Seattle',
          'WA',
          '98109',
          1,
        )
        await applicantQuestions.clickContinue()
      })

      await test.step('Validate address correction page shown and user can select and confirm address', async () => {
        await applicantQuestions.expectVerifyAddressPage(true)

        await applicantQuestions.selectAddressSuggestion(
          'Address With No Service Area Features',
        )
        await applicantQuestions.clickConfirmAddress()
      })

      await test.step('Validate review page shown and user can submit application', async () => {
        await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
          addressWithCorrectionText,
          'Address With No Service Area Features',
        )
        await applicantQuestions.clickSubmitApplication()
      })

      await logout(page)
    })
  })

  /**
   * Tests for the buttons on a block with an address question and on the address correction screen.
   */
  test.describe('address buttons', () => {
    const programName = 'Test program for file upload buttons'
    const emailQuestionText = 'Test email question'
    const addressQuestionText = 'Test address question'
    const numberQuestionText = 'Test number question'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      await test.step('Create all questions', async () => {
        await adminQuestions.addEmailQuestion({
          questionName: 'email-test-q',
          questionText: emailQuestionText,
        })
        await adminQuestions.addAddressQuestion({
          questionName: 'address-question-test-q',
          questionText: addressQuestionText,
        })
        await adminQuestions.addNumberQuestion({
          questionName: 'number-test-q',
          questionText: numberQuestionText,
        })
      })

      await test.step('Create program with blocks before and after the address question', async () => {
        // Having blocks before and after the address question lets us verify
        // the previous and next buttons work correctly.
        // Making the questions optional lets us click "Review" and "Previous"
        // without seeing the "error saving answers" modal, since that modal will
        // trigger if there are validation errors (like missing required questions).
        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockUsingSpec(programName, {
          name: 'Email block',
          questions: [{name: 'email-test-q', isOptional: true}],
        })

        await adminPrograms.addProgramBlockUsingSpec(programName, {
          name: 'Address block',
          questions: [{name: 'address-question-test-q'}],
        })
        await adminPrograms.clickAddressCorrectionToggleByName(
          addressQuestionText,
        )

        await adminPrograms.addProgramBlockUsingSpec(programName, {
          name: 'Number block',
          questions: [{name: 'number-test-q', isOptional: true}],
        })
        await adminPrograms.publishAllDrafts()
      })

      await logout(page)
    })

    test.describe('back button', () => {
      test('clicking back on page with address question redirects to address correction (no suggestions)', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )

          await applicantQuestions.answerAddressQuestion(
            'Bogus Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })

        await test.step('Navigate back and validate verify address page', async () => {
          await applicantQuestions.clickBack()

          await applicantQuestions.expectVerifyAddressPage(false)
        })
      })

      test('clicking back on page with address question redirects to address correction (has suggestions)', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )

          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Navigate back and validate verify address page', async () => {
          await applicantQuestions.clickBack()

          await applicantQuestions.expectVerifyAddressPage(true)
        })
      })

      test('address correction page saves original address when selected and redirects to previous', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()

          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Navigate back and validate verify address page', async () => {
          await applicantQuestions.clickBack()

          await applicantQuestions.expectVerifyAddressPage(true)
        })

        await test.step('Confirm address suggestion and validate address page', async () => {
          // Opt to keep the original address entered
          await applicantQuestions.selectAddressSuggestion('Legit Address')
          await applicantQuestions.clickConfirmAddress()
          // Verify we're taken to the page before the address question page, which is the email question page
          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
        })

        await test.step('Navigate to and validate review page', async () => {
          // Verify the original address was saved
          await applicantQuestions.clickReview()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Legit Address',
          )
        })
      })

      test('address correction page saves suggested address when selected and redirects to previous', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()

          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Navigate back and validate verify address page', async () => {
          await applicantQuestions.clickBack()
          await applicantQuestions.expectVerifyAddressPage(true)
        })

        await test.step('Confirm address suggestion and validate we are taken to the previous page', async () => {
          await applicantQuestions.selectAddressSuggestion(
            'Address With No Service Area Features',
          )
          await applicantQuestions.clickConfirmAddress()

          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
        })

        await test.step('Navigate to and validate review page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Address With No Service Area Features',
          )
        })
      })

      test('address correction page saves original address when no suggestions offered and redirects to previous', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()

          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Bogus Address',
            '',
            'Seattle',
            'WA',
            '98109',
          )
        })

        await test.step('Navigate back and validate verify address page', async () => {
          await applicantQuestions.clickBack()
          await applicantQuestions.expectVerifyAddressPage(false)
        })
        await test.step('Confirm address suggestion and validate we are taken to the previous page', async () => {
          await applicantQuestions.clickConfirmAddress()

          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
        })

        await test.step('Navigate to and validate review page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Bogus Address',
          )
        })
      })

      test('clicking back saves address and goes to previous block if the user enters an address that exactly matches suggestions', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          // Fill out application with address that is contained in findAddressCandidates.json
          // (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
          await applicantQuestions.answerAddressQuestion(
            'Address In Area',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Navigate back and validate verify address page', async () => {
          await applicantQuestions.clickBack()

          await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)
        })
        await test.step('Navigate to and validate review page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Address In Area',
          )
        })
        await logout(page)
      })
    })

    test.describe('review button', () => {
      test('clicking review on page with address question redirects to address correction (no suggestions)', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )

          await applicantQuestions.answerAddressQuestion(
            'Bogus Address',
            '',
            'Seattle',
            'WA',
            '98109',
          )
        })
        await test.step('Click review and validate verify address page', async () => {
          await applicantQuestions.clickReview()

          await applicantQuestions.expectVerifyAddressPage(false)
        })
      })

      test('clicking review on page with address question redirects to address correction (has suggestions)', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(programName, true)
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )

          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click review and validate verify address page', async () => {
          await applicantQuestions.clickReview()

          await applicantQuestions.expectVerifyAddressPage(true)
        })
      })

      test('address correction page saves original address when selected and redirects to review', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })

        await test.step('Click review and validate verify address page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectVerifyAddressPage(true)
        })
        await test.step('Confirm original address and validate review page', async () => {
          // Opt to keep the original address entered
          await applicantQuestions.selectAddressSuggestion('Legit Address')

          await applicantQuestions.clickConfirmAddress()

          // Verify we're taken to the review page
          await applicantQuestions.expectReviewPage()
          // Verify the original address was saved
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Legit Address',
          )
        })

        await logout(page)
      })

      test('address correction page saves suggested address when selected and redirects to review', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(programName, true)
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click review and validate verify address page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectVerifyAddressPage(true)
        })
        await test.step('Confirm suggested address and validate review page', async () => {
          await applicantQuestions.selectAddressSuggestion(
            'Address With No Service Area Features',
          )

          await applicantQuestions.clickConfirmAddress()

          await applicantQuestions.expectReviewPage()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Address With No Service Area Features',
          )
        })
        await logout(page)
      })

      test('address correction page saves original address when no suggestions offered and redirects to review', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Bogus Address',
            '',
            'Seattle',
            'WA',
            '98109',
          )
        })
        await test.step('Click review and validate verify address page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectVerifyAddressPage(false)
        })

        await test.step('Confirm address and validate review page', async () => {
          await applicantQuestions.clickConfirmAddress()

          // Verify we're taken to the review page
          await applicantQuestions.expectReviewPage()
          // Verify the original address was saved
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Bogus Address',
          )
        })

        await logout(page)
      })

      test('clicking review saves address and goes to review page if the user enters an address that exactly matches suggestions', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          // Fill out application with address that is contained in findAddressCandidates.json
          // (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
          await applicantQuestions.answerAddressQuestion(
            'Address In Area',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click review and validate review page', async () => {
          await applicantQuestions.clickReview()

          await applicantQuestions.expectReviewPage()
          // Verify the applicant's answer is saved
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Address In Area',
          )
        })

        await logout(page)
      })
    })

    test.describe('save & next button', () => {
      test('clicking next on page with address question redirects to address correction (no suggestions)', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )

          await applicantQuestions.answerAddressQuestion(
            'Bogus Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click continue and validate address correction page', async () => {
          await applicantQuestions.clickContinue()

          await applicantQuestions.expectVerifyAddressPage(false)
        })
      })

      test('clicking next on page with address question redirects to address correction (has suggestions)', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )

          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click continue and validate address correction page', async () => {
          await applicantQuestions.clickContinue()

          await applicantQuestions.expectVerifyAddressPage(true)
        })
      })

      test('address correction page saves original address when selected and redirects to next', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click continue and validate address correction page shown', async () => {
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectVerifyAddressPage(true)
        })

        await test.step('Confirm original address and validate next page', async () => {
          await applicantQuestions.selectAddressSuggestion('Legit Address')
          await applicantQuestions.clickConfirmAddress()

          await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)
        })

        await test.step('Click review and validate review page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Legit Address',
          )
        })
      })

      test('address correction page saves suggested address when selected and redirects to next', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click continue and validate address correction page shown', async () => {
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectVerifyAddressPage(true)
        })
        await test.step('Confirm address selection and validate next page', async () => {
          // Opt for one of the suggested addresses
          await applicantQuestions.selectAddressSuggestion(
            'Address With No Service Area Features',
          )
          await applicantQuestions.clickConfirmAddress()

          // Verify we're taken to the next page, which has the number question
          await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)
        })
        await test.step('Click review and validate review page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Address With No Service Area Features',
          )
        })
      })

      test('address correction page saves original address when no suggestions offered and redirects to next', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.answerAddressQuestion(
            'Bogus Address',
            '',
            'Seattle',
            'WA',
            '98109',
          )
        })
        await test.step('Click continue and validate address correction page shown', async () => {
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectVerifyAddressPage(false)
        })
        await test.step('Confirm address and validate next page', async () => {
          await applicantQuestions.clickConfirmAddress()

          // Verify we're taken to the next page, which has the number question
          await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)
        })
        await test.step('Click review and validate review page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Bogus Address',
          )
        })
      })

      test('clicking next saves address and goes to next block if the user enters an address that exactly matches suggestions', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )
          // Fill out application with address that is contained in findAddressCandidates.json
          // (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
          await applicantQuestions.answerAddressQuestion(
            'Address In Area',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click continue and validate next page shown', async () => {
          await applicantQuestions.clickContinue()

          await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)
        })
        await test.step('Click review and validate review page', async () => {
          await applicantQuestions.clickReview()
          await applicantQuestions.expectQuestionAnsweredOnReviewPageNorthstar(
            addressQuestionText,
            'Address In Area',
          )
        })
        await logout(page)
      })
    })

    test.describe('go back and edit button', () => {
      test('clicking go back and edit on address correction goes back to page with address question', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )

          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
        await test.step('Click continue and validate address correction page shown', async () => {
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectVerifyAddressPage(true)
        })

        await test.step('Click go back and edit and validate address question shown', async () => {
          await applicantQuestions.clickGoBackAndEdit()

          await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)
        })
      })

      test('go back and edit does not save address selection', async ({
        applicantQuestions,
      }) => {
        await test.step('Answer address question', async () => {
          await applicantQuestions.applyProgram(
            programName,
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.clickReview()
          await applicantQuestions.editQuestionFromReviewPage(
            addressQuestionText,
            /* northStarEnabled= */ true,
          )

          await applicantQuestions.answerAddressQuestion(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })

        await test.step('Click continue and validate address correction page shown', async () => {
          await applicantQuestions.clickContinue()
          await applicantQuestions.expectVerifyAddressPage(true)
        })

        await test.step('Select suggestion, but click go back and edit should not save the suggestion', async () => {
          await applicantQuestions.selectAddressSuggestion(
            'Address With No Service Area Features',
          )

          await applicantQuestions.clickGoBackAndEdit()

          await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)

          // Verify the original address (not the suggested address) is filled in on the block page
          await applicantQuestions.checkAddressQuestionValue(
            'Legit Address',
            '',
            'Redlands',
            'CA',
            '92373',
          )
        })
      })
    })
  })
}
