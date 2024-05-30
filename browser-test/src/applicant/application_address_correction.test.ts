import {test} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../support'

/** Tests for the address correction view and navigation to and from that view. */
test.describe('address correction', () => {
  const multiBlockMultiAddressProgram =
    'Address correction multi-block, multi-address program'
  const singleBlockMultiAddressProgram =
    'Address correction single-block, multi-address program'
  const singleBlockSingleAddressProgram =
    'Address correction single-block, single-address program'
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

    await test.step('Create multi-block, multi-address program', async () => {
      await adminPrograms.addProgram(multiBlockMultiAddressProgram)

      await adminPrograms.editProgramBlockWithOptional(
        multiBlockMultiAddressProgram,
        'first block',
        [addressWithCorrectionQuestionId],
        addressWithoutCorrectionQuestionId,
      )

      await adminPrograms.addProgramBlock(
        multiBlockMultiAddressProgram,
        'second block',
        [textQuestionId],
      )

      await adminPrograms.goToBlockInProgram(
        multiBlockMultiAddressProgram,
        'Screen 1',
      )
      await adminPrograms.clickAddressCorrectionToggleByName(
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(multiBlockMultiAddressProgram)
    })

    await test.step('Create single-block, multi-address program', async () => {
      await adminPrograms.addProgram(singleBlockMultiAddressProgram)

      await adminPrograms.editProgramBlockWithOptional(
        singleBlockMultiAddressProgram,
        'first block',
        [addressWithCorrectionQuestionId],
        addressWithoutCorrectionQuestionId,
      )

      await adminPrograms.goToBlockInProgram(
        singleBlockMultiAddressProgram,
        'Screen 1',
      )
      await adminPrograms.clickAddressCorrectionToggleByName(
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(singleBlockMultiAddressProgram)
    })

    await test.step('Create single-block, single-address program', async () => {
      await adminPrograms.addProgram(singleBlockSingleAddressProgram)

      await adminPrograms.editProgramBlock(
        singleBlockSingleAddressProgram,
        'first block',
        [addressWithCorrectionQuestionId],
      )

      await adminPrograms.goToBlockInProgram(
        singleBlockSingleAddressProgram,
        'Screen 1',
      )
      await adminPrograms.clickAddressCorrectionToggleByName(
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(singleBlockSingleAddressProgram)
    })

    await test.step('Create optional address program', async () => {
      await adminPrograms.addProgram(optionalAddressProgram)

      await adminPrograms.editProgramBlockWithOptional(
        optionalAddressProgram,
        'first block',
        [],
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.goToBlockInProgram(optionalAddressProgram, 'Screen 1')
      await adminPrograms.clickAddressCorrectionToggleByName(
        addressWithCorrectionQuestionId,
      )

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(optionalAddressProgram)
    })

    await logout(page)
  })

  if (isLocalDevEnvironment()) {
    test('can correct address multi-block, multi-address program', async ({
      page,
      applicantQuestions,
    }) => {
      await enableFeatureFlag(page, 'esri_address_correction_enabled')
      await applicantQuestions.applyProgram(multiBlockMultiAddressProgram)

      // Fill out application and submit.
      await applicantQuestions.answerAddressQuestion(
        '500 Harrison',
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
      await applicantQuestions.clickNext()
      await applicantQuestions.expectVerifyAddressPage(true)
      await applicantQuestions.clickConfirmAddress()
      await applicantQuestions.answerTextQuestion('Some text')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        addressWithCorrectionText,
        'Address In Area',
      )
      await applicantQuestions.clickSubmit()
      await logout(page)
    })

    test('can correct address single-block, multi-address program', async ({
      page,
      applicantQuestions,
    }) => {
      await enableFeatureFlag(page, 'esri_address_correction_enabled')
      await applicantQuestions.applyProgram(singleBlockMultiAddressProgram)

      await applicantQuestions.answerAddressQuestion(
        '500 Harrison',
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
      await applicantQuestions.clickNext()
      await applicantQuestions.expectVerifyAddressPage(true)

      await applicantQuestions.selectAddressSuggestion(
        'Address With No Service Area Features',
      )
      await applicantQuestions.clickConfirmAddress()

      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        addressWithCorrectionText,
        'Address With No Service Area Features',
      )
      await applicantQuestions.clickSubmit()
      await logout(page)
    })

    test('can correct address single-block, single-address program', async ({
      page,
      applicantQuestions,
    }) => {
      await enableFeatureFlag(page, 'esri_address_correction_enabled')
      await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

      await applicantQuestions.answerAddressQuestion(
        'Legit Address',
        '',
        'Redlands',
        'CA',
        '92373',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.expectVerifyAddressPage(true)

      // Only doing accessibility and screenshot checks for address correction page
      // once since they are all the same
      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'verify-address-with-suggestions',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )

      await applicantQuestions.clickConfirmAddress()
      await applicantQuestions.expectQuestionAnsweredOnReviewPage(
        addressWithCorrectionText,
        'Address In Area',
      )
      await applicantQuestions.clickSubmit()
      await logout(page)
    })

    test('skips address correction if optional address question is not answered', async ({
      page,
      applicantQuestions,
    }) => {
      await enableFeatureFlag(page, 'esri_address_correction_enabled')
      await applicantQuestions.applyProgram(optionalAddressProgram)

      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()

      await applicantQuestions.clickSubmit()
      await logout(page)
    })

    test('prompts user to edit if no suggestions are returned', async ({
      page,
      applicantQuestions,
    }) => {
      await enableFeatureFlag(page, 'esri_address_correction_enabled')
      await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

      // Fill out application and submit.
      await applicantQuestions.answerAddressQuestion(
        'Bogus Address',
        '',
        'Seattle',
        'WA',
        '98109',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.expectVerifyAddressPage(false)

      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'verify-address-no-suggestions',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )

      // Can continue on anyway
      await applicantQuestions.clickConfirmAddress()
      await applicantQuestions.clickSubmit()
      await logout(page)
    })

    test('prompts user to edit if an error is returned from the Esri service', async ({
      page,
      applicantQuestions,
    }) => {
      // This is currently the same as when no suggestions are returned.
      // We may change this later.
      await enableFeatureFlag(page, 'esri_address_correction_enabled')
      await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

      // Fill out application and submit.
      await applicantQuestions.answerAddressQuestion(
        'Error Address',
        '',
        'Seattle',
        'WA',
        '98109',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.expectVerifyAddressPage(false)

      await validateAccessibility(page)
      await validateScreenshot(
        page,
        'verify-address-esri-service-error',
        /* fullPage= */ true,
        // Since this page is currently the same as the no-suggestions page,
        // don't get extra mobile screenshots of the same page.
        /* mobileScreenshot= */ false,
      )

      // Can continue on anyway
      await applicantQuestions.clickConfirmAddress()
      await applicantQuestions.clickSubmit()
      await logout(page)
    })

    test('skips address correction screen if address exactly matches suggestions', async ({
      page,
      applicantQuestions,
    }) => {
      await enableFeatureFlag(page, 'esri_address_correction_enabled')

      await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)
      // Fill out application with address that is contained in findAddressCandidates.json
      // (the list of suggestions returned from FakeEsriClient.fetchAddressSuggestions())
      await applicantQuestions.answerAddressQuestion(
        'Address In Area',
        '',
        'Redlands',
        'CA',
        '92373',
      )
      await applicantQuestions.clickNext()
      await applicantQuestions.expectReviewPage()

      await logout(page)
    })
  }

  test('address correction page does not show if feature is disabled', async ({
    page,
    applicantQuestions,
  }) => {
    test.slow()

    await disableFeatureFlag(page, 'esri_address_correction_enabled')
    await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

    // Fill out application and submit.
    await applicantQuestions.answerAddressQuestion(
      '305 Harrison',
      '',
      'Seattle',
      'WA',
      '98109',
    )
    await applicantQuestions.clickNext()

    // Expect the review page, not the address correction page
    await applicantQuestions.expectQuestionAnsweredOnReviewPage(
      addressWithCorrectionText,
      '305 Harrison',
    )
    await applicantQuestions.clickSubmit()
    await logout(page)
  })

  test.describe('with North Star flag enabled', {tag: ['@northstar']}, () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
      await enableFeatureFlag(page, 'esri_address_correction_enabled')
    })

    test('can correct address single-block, single-address program', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

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

        await validateScreenshot(
          page,
          'north-star-verify-address-with-suggestions',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })

      await test.step('Confirm user can confirm address and submit', async () => {
        await applicantQuestions.clickConfirmAddress()

        await applicantQuestions.clickReview()
        await applicantQuestions.checkAddressQuestionValue(
          'Address In Area',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickContinue()
        await applicantQuestions.clickSubmit()
      })
    })

    test('prompts user to edit if no suggestions are returned', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

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

        await validateScreenshot(
          page,
          'north-star-verify-address-no-suggestions',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })

      await test.step('Confirm user can confirm address and submit', async () => {
        await applicantQuestions.clickConfirmAddress()
        await applicantQuestions.clickSubmit()
      })
    })

    test('go back and edit does not save address selection', async ({
      applicantQuestions,
    }) => {
      await test.step('Answer address question', async () => {
        await applicantQuestions.applyProgram(singleBlockSingleAddressProgram)

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
      })

      await test.step('Select suggestion, but click go back and edit should not save the suggestion', async () => {
        await applicantQuestions.selectAddressSuggestion(
          'Address With No Service Area Features',
        )

        await applicantQuestions.clickGoBackAndEdit()

        await applicantQuestions.validateQuestionIsOnPage(
          addressWithCorrectionText,
        )

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

    test('validate accessibility', async ({page}) => {
      await validateAccessibility(page)
    })
  })
})

if (isLocalDevEnvironment()) {
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

      // Create a program with 3 blocks:
      // - Block 1: Optional email question
      // - Block 2: Required address question
      // - Block 3: Optional number question
      // Having blocks before and after the address question lets us verify
      // the previous and next buttons work correctly.
      // Making the questions optional lets us click "Review" and "Previous"
      // without seeing the "error saving answers" modal, since that modal will
      // trigger if there are validation errors (like missing required questions).
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

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Email block',
        [],
        'email-test-q',
      )

      await adminPrograms.addProgramBlock(programName, 'Address block', [
        'address-question-test-q',
      ])
      await adminPrograms.goToBlockInProgram(programName, 'Screen 2')
      await adminPrograms.clickAddressCorrectionToggleByName(
        addressQuestionText,
      )

      await adminPrograms.addProgramBlock(programName)
      await adminPrograms.goToBlockInProgram(programName, 'Screen 3')
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Number block',
        [],
        'number-test-q',
      )

      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    test.describe('previous button', () => {
      test('clicking previous on page with address question redirects to address correction (no suggestions)', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )

        await applicantQuestions.answerAddressQuestion(
          'Bogus Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )

        await applicantQuestions.clickPrevious()

        await applicantQuestions.expectVerifyAddressPage(false)
      })

      test('clicking previous on page with address question redirects to address correction (has suggestions)', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )

        await applicantQuestions.clickPrevious()

        await applicantQuestions.expectVerifyAddressPage(true)
      })

      test('address correction page saves original address when selected and redirects to previous', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickPrevious()

        await applicantQuestions.expectVerifyAddressPage(true)

        // Opt to keep the original address entered
        await applicantQuestions.selectAddressSuggestion('Legit Address')
        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the page before the address question page, which is the email question page
        await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)

        // Verify the original address was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Legit Address',
        )
      })

      test('address correction page saves suggested address when selected and redirects to previous', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickPrevious()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Opt for one of the suggested addresses
        await applicantQuestions.selectAddressSuggestion(
          'Address With No Service Area Features',
        )
        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the page before the address question page, which is the email question page
        await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)

        // Verify the suggested address was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Address With No Service Area Features',
        )
      })

      test('address correction page saves original address when no suggestions offered and redirects to previous', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Bogus Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickPrevious()
        await applicantQuestions.expectVerifyAddressPage(false)

        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the page before the address question page, which is the email question page
        await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)

        // Verify the suggested address was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Bogus Address',
        )
      })

      test('clicking previous saves address and goes to previous block if the user enters an address that exactly matches suggestions', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
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

        await applicantQuestions.clickPrevious()

        // Verify we're taken to the page before the address question page, which is the email question page
        await applicantQuestions.validateQuestionIsOnPage(emailQuestionText)

        // Verify the address was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Address In Area',
        )

        await logout(page)
      })
    })

    test.describe('review button', () => {
      test('clicking review on page with address question redirects to address correction (no suggestions)', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )

        await applicantQuestions.answerAddressQuestion(
          'Bogus Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )

        await applicantQuestions.clickReview()

        await applicantQuestions.expectVerifyAddressPage(false)
      })

      test('clicking review on page with address question redirects to address correction (has suggestions)', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )

        await applicantQuestions.clickReview()

        await applicantQuestions.expectVerifyAddressPage(true)
      })

      test('address correction page saves original address when selected and redirects to review', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickReview()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Opt to keep the original address entered
        await applicantQuestions.selectAddressSuggestion('Legit Address')

        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the review page
        await applicantQuestions.expectReviewPage()
        // Verify the original address was saved
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Legit Address',
        )

        await logout(page)
      })

      test('address correction page saves suggested address when selected and redirects to review', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickReview()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Opt for one of the suggested addresses
        await applicantQuestions.selectAddressSuggestion(
          'Address With No Service Area Features',
        )

        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the review page
        await applicantQuestions.expectReviewPage()
        // Verify the original address was saved
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Address With No Service Area Features',
        )
        await logout(page)
      })

      test('address correction page saves original address when no suggestions offered and redirects to review', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Bogus Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickReview()
        await applicantQuestions.expectVerifyAddressPage(false)

        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the review page
        await applicantQuestions.expectReviewPage()
        // Verify the original address was saved
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Bogus Address',
        )

        await logout(page)
      })

      test('clicking review saves address and goes to review page if the user enters an address that exactly matches suggestions', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
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

        await applicantQuestions.clickReview()

        await applicantQuestions.expectReviewPage()
        // Verify the applicant's answer is saved
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Address In Area',
        )

        await logout(page)
      })
    })

    test.describe('save & next button', () => {
      test('clicking next on page with address question redirects to address correction (no suggestions)', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )

        await applicantQuestions.answerAddressQuestion(
          'Bogus Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )

        await applicantQuestions.clickNext()

        await applicantQuestions.expectVerifyAddressPage(false)
      })

      test('clicking next on page with address question redirects to address correction (has suggestions)', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )

        await applicantQuestions.clickNext()

        await applicantQuestions.expectVerifyAddressPage(true)
      })

      test('address correction page saves original address when selected and redirects to next', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Opt to keep the original address entered
        await applicantQuestions.selectAddressSuggestion('Legit Address')
        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the next page, which has the number question
        await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)

        // Verify the original address was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Legit Address',
        )
      })

      test('address correction page saves suggested address when selected and redirects to next', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Opt for one of the suggested addresses
        await applicantQuestions.selectAddressSuggestion(
          'Address With No Service Area Features',
        )
        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the next page, which has the number question
        await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)

        // Verify the suggested address was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Address With No Service Area Features',
        )
      })

      test('address correction page saves original address when no suggestions offered and redirects to next', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )
        await applicantQuestions.answerAddressQuestion(
          'Bogus Address',
          '',
          'Seattle',
          'WA',
          '98109',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(false)

        await applicantQuestions.clickConfirmAddress()

        // Verify we're taken to the next page, which has the number question
        await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)

        // Verify the original address was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Bogus Address',
        )
      })

      test('clicking next saves address and goes to next block if the user enters an address that exactly matches suggestions', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
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

        await applicantQuestions.clickNext()

        // Verify we're taken to the next page, which has the number question
        await applicantQuestions.validateQuestionIsOnPage(numberQuestionText)

        // Verify the address was saved
        await applicantQuestions.clickReview()
        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          addressQuestionText,
          'Address In Area',
        )

        await logout(page)
      })
    })

    test.describe('go back and edit button', () => {
      test('clicking go back and edit on address correction goes back to page with address question', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)

        await applicantQuestions.clickGoBackAndEdit()

        await applicantQuestions.validateQuestionIsOnPage(addressQuestionText)
      })

      test('go back and edit does not save address selection', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.answerQuestionFromReviewPage(
          addressQuestionText,
        )

        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Redlands',
          'CA',
          '92373',
        )
        await applicantQuestions.clickNext()
        await applicantQuestions.expectVerifyAddressPage(true)

        // Select an address suggestion, but then click "Go back and edit",
        // which shouldn't save the suggestion
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
}
