import {test, expect} from '../support/civiform_fixtures'
import {
  ApplicantQuestions,
  AdminPrograms,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
  seedProgramsAndCategories,
} from '../support'
import {Page} from 'playwright'
import {ProgramVisibility} from '../support/admin_programs'

test.describe('applicant program index page', () => {
  const primaryProgramName = 'Application index primary program'
  const otherProgramName = 'Application index other program'

  const firstQuestionText = 'This is the first question'
  const secondQuestionText = 'This is the second question'

  test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
    await loginAsAdmin(page)

    // Create a program with two questions on separate blocks so that an applicant can partially
    // complete an application.
    await adminPrograms.addProgram(primaryProgramName)
    await adminQuestions.addTextQuestion({
      questionName: 'first-q',
      questionText: firstQuestionText,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'second-q',
      questionText: secondQuestionText,
    })
    await adminPrograms.addProgramBlock(primaryProgramName, 'first block', [
      'first-q',
    ])
    await adminPrograms.addProgramBlock(primaryProgramName, 'second block', [
      'second-q',
    ])

    await adminPrograms.addProgram(otherProgramName)
    await adminPrograms.addProgramBlock(otherProgramName, 'first block', [
      'first-q',
    ])

    await adminPrograms.publishAllDrafts()
    await logout(page)
  })

  test('shows value of APPLICANT_PORTAL_NAME in welcome text', async ({
    page,
    adminSettings,
  }) => {
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()
    await adminSettings.setStringSetting(
      'APPLICANT_PORTAL_NAME',
      'Awesome Sauce',
    )
    await adminSettings.expectStringSetting(
      'APPLICANT_PORTAL_NAME',
      'Awesome Sauce',
    )
    await adminSettings.saveChanges()
    await logout(page)

    expect(
      await page
        .getByText(/Log in to your (\w|\s)+ account or create/)
        .textContent(),
    ).toContain('Awesome Sauce')
  })

  test('shows log in button for guest users', async ({
    page,
    applicantQuestions,
  }) => {
    await validateAccessibility(page)

    // We cannot check that the login/create account buttons redirect the user to a particular
    // URL because it varies between environments, so just check for their existence.
    expect(await page.textContent('#login-button')).toContain('Log in')
    expect(await page.textContent('#create-account')).toContain(
      'Create account',
    )
    await applicantQuestions.gotoApplicantHomePage()
    await logout(page)
  })

  test('shows login prompt for guest users when they click apply', async ({
    page,
  }) => {
    await validateAccessibility(page)

    // Click Apply on the primary program. This should show the login prompt modal.
    await page.click(
      `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
    )
    expect(await page.textContent('html')).toContain(
      'Create an account or sign in',
    )
    await validateScreenshot(
      page,
      'apply-program-login-prompt',
      /* fullPage= */ false,
    )

    // Close the modal and click Apply again. This time, we should not see the login prompt modal.
    await page.click(`.cf-modal .cf-modal-close`)
    await page.click(
      `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
    )
    expect(await page.textContent('html')).not.toContain(
      'Create an account or sign in',
    )

    // End guest session and start a new one. Login prompt should show this time upon clicking Apply.
    await logout(page)
    await page.click(
      `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
    )
    expect(await page.textContent('html')).toContain(
      'Create an account or sign in',
    )
  })

  test('categorizes programs for draft and applied applications', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)
    // Navigate to the applicant's program index and validate that both programs appear in the
    // "Not started" section.
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [primaryProgramName, otherProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [],
    })

    // Fill out first application block and confirm that the program appears in the "In progress"
    // section.
    await applicantQuestions.applyProgram(primaryProgramName)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.gotoApplicantHomePage()
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName],
      wantInProgressPrograms: [primaryProgramName],
      wantSubmittedPrograms: [],
    })

    // Finish the application and confirm that the program appears in the "Submitted" section.
    await applicantQuestions.applyProgram(primaryProgramName)
    await applicantQuestions.answerTextQuestion('second answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [primaryProgramName],
    })

    // Logout, then login as guest and confirm that everything appears unsubmitted (https://github.com/civiform/civiform/pull/3487).
    await logout(page)
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName, primaryProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [],
    })
  })

  test('common intake form not present', async ({page}) => {
    await validateScreenshot(page, 'common-intake-form-not-set')
    await validateAccessibility(page)
  })

  test.describe('common intake form present', () => {
    const commonIntakeFormProgramName = 'Benefits finder'

    test.beforeEach(async ({page, adminPrograms}) => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(
        commonIntakeFormProgramName,
        'program description',
        'https://usa.gov',
        ProgramVisibility.PUBLIC,
        'admin description',
        /* isCommonIntake= */ true,
      )
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    test('shows common intake form', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(primaryProgramName)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickNext()
      await applicantQuestions.gotoApplicantHomePage()

      await validateScreenshot(page, 'common-intake-form-sections')
      await applicantQuestions.expectPrograms({
        wantNotStartedPrograms: [otherProgramName],
        wantInProgressPrograms: [primaryProgramName],
        wantSubmittedPrograms: [],
      })
      await applicantQuestions.expectCommonIntakeForm(
        commonIntakeFormProgramName,
      )
      await validateAccessibility(page)
    })

    test('shows a different title for the common intake form', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.clickApplyProgramButton(primaryProgramName)
      expect(await page.innerText('h2')).toContain(
        'Program application summary',
      )

      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.clickApplyProgramButton('Benefits finder')
      expect(await page.innerText('h2')).toContain(
        'Benefits pre-screener summary',
      )
    })
  })

  test('shows previously answered on text for questions that had been answered', async ({
    page,
    applicantQuestions,
  }) => {
    // Fill out application with one question and confirm it shows previously answered at the end.
    await applicantQuestions.applyProgram(otherProgramName)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [primaryProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [otherProgramName],
    })

    // Check that the question repeated in the program with two questions shows previously answered.
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
    await applicantQuestions.validateNoPreviouslyAnsweredText(
      secondQuestionText,
    )
    await validateScreenshot(page, 'question-shows-previously-answered')

    // Fill out second question.
    await applicantQuestions.clickContinue()
    await applicantQuestions.answerTextQuestion('second answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    // Check that the original program shows previously answered.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(otherProgramName)
    await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
    await applicantQuestions.clickSubmit()

    // Change first response on second program.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    await applicantQuestions.clickEdit()
    await applicantQuestions.answerTextQuestion('first answer 2')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    // Check that the other program shows the previously answered text too.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(otherProgramName)
    await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
    await validateScreenshot(page, 'other-program-shows-previously-answered')
  })

  test.describe('applicant program index page with program filtering', () => {
    test.beforeEach(async ({page, adminPrograms}) => {
      await enableFeatureFlag(page, 'program_filtering_enabled')

      await test.step('seed categories', async () => {
        await seedProgramsAndCategories(page)
        await page.goto('/')
      })

      await test.step('go to program edit form and add categories to primary program', async () => {
        await loginAsAdmin(page)
        await adminPrograms.gotoViewActiveProgramPageAndStartEditing(
          primaryProgramName,
        )
        await page.getByRole('button', {name: 'Edit program details'}).click()
        await page.getByText('Education').check()
        await page.getByText('Healthcare').check()
        await adminPrograms.submitProgramDetailsEdits()
      })

      await test.step('add different categories to other program', async () => {
        await adminPrograms.gotoViewActiveProgramPageAndStartEditing(
          otherProgramName,
        )
        await page.getByRole('button', {name: 'Edit program details'}).click()
        await page.getByText('General').check()
        await page.getByText('Utilities').check()
        await adminPrograms.submitProgramDetailsEdits()
      })
    })

    test('shows category filter chips', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('check that filter chips do not appear on homepage while categories on draft programs only', async () => {
        await logout(page)
        await expect(
          page.getByRole('checkbox', {name: 'Education'}),
        ).toBeHidden()
        await expect(
          page.getByRole('checkbox', {name: 'Healthcare'}),
        ).toBeHidden()
        await expect(page.getByRole('checkbox', {name: 'General'})).toBeHidden()
        await expect(
          page.getByRole('checkbox', {name: 'Utilities'}),
        ).toBeHidden()
      })

      await test.step('publish programs with categories', async () => {
        await loginAsAdmin(page)
        await adminPrograms.publishAllDrafts()
        await logout(page)
      })

      const filterChips = page.locator('#category-filter-form')

      await test.step('check that filter chips appear on homepage', async () => {
        await expect(filterChips.getByText('Education')).toBeVisible()
        await expect(filterChips.getByText('Healthcare')).toBeVisible()
        await expect(filterChips.getByText('General')).toBeVisible()
        await expect(filterChips.getByText('Utilities')).toBeVisible()
      })

      await test.step('select some filter chips and take screenshot', async () => {
        await filterChips.getByText('Education').check()
        await filterChips.getByText('General').check()
        await validateScreenshot(
          page.locator('#main-content'),
          'category-filter-chips',
        )
      })

      await test.step('start applying to a program', async () => {
        await applicantQuestions.applyProgram(primaryProgramName)
        await applicantQuestions.answerTextQuestion('first answer')
        await applicantQuestions.clickNext()
        await applicantQuestions.gotoApplicantHomePage()
      })

      await test.step('check that categories only on started program are removed from filters', async () => {
        await expect(filterChips.getByText('Education')).toBeHidden()
        await expect(filterChips.getByText('Healthcare')).toBeHidden()
        await expect(filterChips.getByText('General')).toBeVisible()
        await expect(filterChips.getByText('Utilities')).toBeVisible()
      })

      await validateAccessibility(page)
    })

    test('with program filters enabled, categorizes programs correctly', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('publish programs with categories', async () => {
        await adminPrograms.publishAllDrafts()
      })

      await test.step('Navigate to program index and validate that all programs appear in Programs and Services', async () => {
        await logout(page)
        await loginAsTestUser(page)
        await applicantQuestions.expectProgramsWithFilteringEnabled({
          expectedProgramsInInProgressSection: [],
          expectedProgramsInSubmittedSection: [],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
            'Minimal Sample Program',
            'Comprehensive Sample Program',
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        })
      })

      await test.step('Fill out first application block and confirm that the program appears in the In progress section', async () => {
        await applicantQuestions.applyProgram(primaryProgramName)
        await applicantQuestions.answerTextQuestion('first answer')
        await applicantQuestions.clickNext()
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.expectProgramsWithFilteringEnabled({
          expectedProgramsInInProgressSection: [primaryProgramName],
          expectedProgramsInSubmittedSection: [],
          expectedProgramsInProgramsAndServicesSection: [
            otherProgramName,
            'Minimal Sample Program',
            'Comprehensive Sample Program',
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        })
      })

      await test.step('Finish the application and confirm that the program appears in the "Submitted" section', async () => {
        await applicantQuestions.applyProgram(primaryProgramName)
        await applicantQuestions.answerTextQuestion('second answer')
        await applicantQuestions.clickNext()
        await applicantQuestions.submitFromReviewPage()
        await applicantQuestions.returnToProgramsFromSubmissionPage()
        await applicantQuestions.expectProgramsWithFilteringEnabled({
          expectedProgramsInInProgressSection: [],
          expectedProgramsInSubmittedSection: [primaryProgramName],
          expectedProgramsInProgramsAndServicesSection: [
            otherProgramName,
            'Minimal Sample Program',
            'Comprehensive Sample Program',
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        })
      })

      await test.step('Click on a filter and see the Recommended and Other programs sections', async () => {
        await page.locator('#category-filter-form').getByText('General').check()
        await applicantQuestions.expectProgramsWithFilteringEnabled(
          {
            expectedProgramsInInProgressSection: [],
            expectedProgramsInSubmittedSection: [primaryProgramName],
            expectedProgramsInProgramsAndServicesSection: [],
            expectedProgramsInRecommendedSection: [otherProgramName],
            expectedProgramsInOtherProgramsSection: [
              'Minimal Sample Program',
              'Comprehensive Sample Program',
            ],
          },
          true,
        )
      })

      await validateAccessibility(page)

      await test.step('Logout, then login as guest and confirm that everything appears unsubmitted', async () => {
        await logout(page)
        await applicantQuestions.expectProgramsWithFilteringEnabled({
          expectedProgramsInInProgressSection: [],
          expectedProgramsInSubmittedSection: [],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
            'Minimal Sample Program',
            'Comprehensive Sample Program',
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        })
      })
    })
  })

  test.describe(
    'applicant program index page with northstar UI',
    {tag: ['@northstar']},
    () => {
      test.beforeEach(async ({page}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
      })

      test('validate initial page load as guest user', async ({page}) => {
        await validateScreenshot(
          page,
          'program-index-page-initial-load-northstar',
        )
      })

      test('validate accessibility', async ({page}) => {
        await validateAccessibility(page)
      })

      test('shows log in button for guest users', async ({page}) => {
        // We cannot check that the login/create account buttons redirect the user to a particular
        // URL because it varies between environments, so just check for their existence.
        expect(await page.textContent('#login-button')).toContain('Log in')
        expect(await page.textContent('#create-account')).toContain(
          'Create account',
        )
      })

      test('shows login prompt for guest users when they click apply', async ({
        page,
      }) => {
        await test.step('Verify that login dialog appears', async () => {
          // Click Apply on the primary program. This should show the login prompt modal.
          await page.click(
            `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
          )
          expect(await page.textContent('html')).toContain(
            'Create an account or sign in',
          )
          await validateScreenshot(
            page.getByRole('dialog').getByTestId('login'),
            'apply-program-login-prompt-northstar',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('Verify that login dialog does not appear a second time', async () => {
          // Close the modal and click Apply again. This time, we should not see the login prompt modal.
          await page.click(`.cf-ns-modal .usa-modal__close`)
          await page.click(
            `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
          )
          expect(await page.textContent('html')).not.toContain(
            'Create an account or sign in',
          )
        })
      })

      test('categorizes programs for draft and applied applications as guest user', async ({
        applicantQuestions,
        page,
      }) => {
        await loginAsTestUser(page)

        await test.step('Programs start in not started', async () => {
          await applicantQuestions.expectPrograms({
            wantNotStartedPrograms: [primaryProgramName, otherProgramName],
            wantInProgressPrograms: [],
            wantSubmittedPrograms: [],
          })
        })

        await test.step('First application is in "in progress" section once filled out', async () => {
          await applicantQuestions.applyProgram(primaryProgramName)
          await applicantQuestions.answerTextQuestion('first answer')
          await applicantQuestions.clickContinue()
          await applicantQuestions.gotoApplicantHomePage()
          await applicantQuestions.expectPrograms({
            wantNotStartedPrograms: [otherProgramName],
            wantInProgressPrograms: [primaryProgramName],
            wantSubmittedPrograms: [],
          })
          await validateScreenshot(
            page,
            'program-index-page-inprogress-northstar',
          )
        })

        await test.step('First application is in "Submitted" section once submitted', async () => {
          await applicantQuestions.applyProgram(primaryProgramName)
          await applicantQuestions.answerTextQuestion('second answer')
          await applicantQuestions.clickContinue()
          await applicantQuestions.submitFromReviewPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.returnToProgramsFromSubmissionPage(
            /* northStarEnabled= */ true,
          )
          await applicantQuestions.expectPrograms({
            wantNotStartedPrograms: [otherProgramName],
            wantInProgressPrograms: [],
            wantSubmittedPrograms: [primaryProgramName],
          })
          await validateScreenshot(
            page,
            'program-index-page-submitted-northstar',
          )
        })

        await test.step('When logged out, everything appears unsubmitted (https://github.com/civiform/civiform/pull/3487)', async () => {
          await logout(page, false)

          await applicantQuestions.expectPrograms({
            wantNotStartedPrograms: [otherProgramName, primaryProgramName],
            wantInProgressPrograms: [],
            wantSubmittedPrograms: [],
          })
        })
      })

      test('Click "View details" button and expect to open a new tab with the external program URL', async ({
        page,
      }) => {
        // Locate the first card (doesn't really matter which one)
        const firstCard = page.locator('.usa-card-group li').nth(0)
        await firstCard.getByText('View details').click()

        // Clicking the button opens a new tab
        const popupPromise = page.waitForEvent('popup')
        const popup = await popupPromise
        const popupURL = await popup.evaluate('location.href')

        // Verify the program details URL matches the external link
        expect(popupURL).toMatch('https://www.usa.gov/')
      })
    },
  )
})

test.describe('applicant program index page with images', () => {
  test('shows program with wide image', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    const programName = 'Wide Image Program'
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await validateScreenshot(page, 'program-image-wide')
    await validateAccessibility(page)
  })

  test(
    'shows program with wide image in North Star',
    {tag: ['@northstar']},
    async ({page, adminPrograms, adminProgramImage}) => {
      const programName = 'Wide Image Program'
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )
      await adminPrograms.publishAllDrafts()
      await logout(page)

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await validateScreenshot(page, 'north-star-program-image-wide')
      await validateAccessibility(page)
    },
  )

  test('shows program with tall image', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    const programName = 'Tall Image Program'
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-tall.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await validateScreenshot(page, 'program-image-tall')
  })

  test('shows program with image and status', async ({
    page,
    adminPrograms,
    adminProgramStatuses,
    adminProgramImage,
    applicantQuestions,
  }) => {
    const programName = 'Image And Status Program'
    await loginAsAdmin(page)

    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )

    const approvedStatusName = 'Approved'
    await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    await adminProgramStatuses.createStatus(approvedStatusName)
    await adminPrograms.publishProgram(programName)
    await adminPrograms.expectActiveProgram(programName)
    await logout(page)

    await submitApplicationAndApplyStatus(
      page,
      programName,
      approvedStatusName,
      adminPrograms,
      applicantQuestions,
    )

    // Verify program card shows both the Accepted status and image
    await loginAsTestUser(page)
    await validateScreenshot(page, 'program-image-with-status')
  })

  // This test puts programs with different specs in the different sections of the homepage
  // to verify that different card formats appear correctly next to each other and across sections.
  test('shows programs with and without images in all sections', async ({
    page,
    adminPrograms,
    adminProgramStatuses,
    adminProgramImage,
    adminQuestions,
    applicantQuestions,
  }) => {
    test.slow()

    // Common Intake: Basic (no image or status)
    await loginAsAdmin(page)
    const commonIntakeFormProgramName = 'Benefits finder'
    await adminPrograms.addProgram(
      commonIntakeFormProgramName,
      'program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ true,
    )

    // In Progress: Image
    const programNameInProgressImage = 'In Progress Program [Image]'
    await adminPrograms.addProgram(programNameInProgressImage)
    await adminQuestions.addTextQuestion({
      questionName: 'first-q',
      questionText: 'first question text',
    })
    await adminPrograms.addProgramBlock(
      programNameInProgressImage,
      'first block',
      ['first-q'],
    )

    await adminPrograms.goToProgramImagePage(programNameInProgressImage)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programNameInProgressImage)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.gotoApplicantHomePage()
    await logout(page)

    // Submitted #1: Image and status
    const programNameSubmittedWithImageAndStatus =
      'Submitted Program [Image and Status]'
    const approvedStatusName = 'Approved'
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programNameSubmittedWithImageAndStatus)
    await adminPrograms.goToProgramImagePage(
      programNameSubmittedWithImageAndStatus,
    )
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.gotoDraftProgramManageStatusesPage(
      programNameSubmittedWithImageAndStatus,
    )
    await adminProgramStatuses.createStatus(approvedStatusName)
    await adminPrograms.publishProgram(programNameSubmittedWithImageAndStatus)
    await adminPrograms.expectActiveProgram(
      programNameSubmittedWithImageAndStatus,
    )
    await logout(page)

    await submitApplicationAndApplyStatus(
      page,
      programNameSubmittedWithImageAndStatus,
      approvedStatusName,
      adminPrograms,
      applicantQuestions,
    )

    // Submitted #2: Basic
    const programNameSubmittedBasic = 'Submitted Program [Basic]'
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programNameSubmittedBasic)
    await adminPrograms.publishProgram(programNameSubmittedBasic)
    await adminPrograms.expectActiveProgram(programNameSubmittedBasic)
    await logout(page)

    await loginAsTestUser(page)
    await applicantQuestions.clickApplyProgramButton(programNameSubmittedBasic)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // Submitted #3: Status
    const programNameSubmittedWithStatus = 'Submitted Program [Status]'
    await loginAsAdmin(page)

    await adminPrograms.addProgram(programNameSubmittedWithStatus)
    await adminPrograms.gotoDraftProgramManageStatusesPage(
      programNameSubmittedWithStatus,
    )
    await adminProgramStatuses.createStatus(approvedStatusName)
    await adminPrograms.publishProgram(programNameSubmittedWithStatus)
    await adminPrograms.expectActiveProgram(programNameSubmittedWithStatus)
    await logout(page)

    await submitApplicationAndApplyStatus(
      page,
      programNameSubmittedWithStatus,
      approvedStatusName,
      adminPrograms,
      applicantQuestions,
    )

    // Submitted #4 (on new row): Image
    const programNameSubmittedImage = 'Submitted Program [Image]'
    await loginAsAdmin(page)

    await adminPrograms.addProgram(programNameSubmittedImage)
    await adminPrograms.goToProgramImagePage(programNameSubmittedImage)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await loginAsTestUser(page)
    await applicantQuestions.clickApplyProgramButton(programNameSubmittedImage)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // Not Started #1: Basic
    await loginAsAdmin(page)
    await adminPrograms.addProgram('Not Started Program [Basic]')

    // Not Started #2: Image
    const programNameNotStartedImage = 'Not Started Program [Image]'
    await adminPrograms.addProgram(programNameNotStartedImage)
    await adminPrograms.goToProgramImagePage(programNameNotStartedImage)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    // Verify homepage
    await loginAsTestUser(page)
    await validateScreenshot(page, 'program-image-all-types')
    await validateAccessibility(page)
  })

  async function submitApplicationAndApplyStatus(
    page: Page,
    programName: string,
    statusName: string,
    adminPrograms: AdminPrograms,
    applicantQuestions: ApplicantQuestions,
  ) {
    // Submit an application as a test user.
    await loginAsTestUser(page)
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // Set a status as a program admin
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    const modal = await adminPrograms.setStatusOptionAndAwaitModal(statusName)
    await adminPrograms.confirmStatusUpdateModal(modal)
    await logout(page)
  }
})
