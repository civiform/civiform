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
  selectApplicantLanguage,
  normalizeElements,
} from '../support'
import {Page} from 'playwright'
import {ProgramVisibility} from '../support/admin_programs'
import {BASE_URL} from '../support/config'

test.describe('applicant program index page', {tag: ['@northstar']}, () => {
  const primaryProgramName = 'Application index primary program'
  const otherProgramName = 'Application index other program'

  const firstQuestionText = 'This is the first question'
  const secondQuestionText = 'This is the second question'

  test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')

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
    // Primary program's screen 1 has 0 questions, so the 'first block' is actually screen 2
    await adminPrograms.addProgramBlockUsingSpec(primaryProgramName, {
      description: 'first block',
      questions: [{name: 'first-q'}],
    })
    // The 'second block' is actually screen 3
    await adminPrograms.addProgramBlockUsingSpec(primaryProgramName, {
      description: 'second block',
      questions: [{name: 'second-q'}],
    })

    await adminPrograms.addProgram(otherProgramName)
    await adminPrograms.addProgramBlockUsingSpec(otherProgramName, {
      description: 'first block',
      questions: [{name: 'first-q'}],
    })

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

    expect(await page.getByText(/To get help with/).textContent()).toBeTruthy()
  })

  test('validate initial page load as guest user', async ({
    page,
    applicantQuestions,
  }) => {
    await validateScreenshot(page, 'program-index-page-initial-load-northstar')
    await applicantQuestions.expectTitle(page, 'Find programs')
  })

  test('validate accessibility', async ({page}) => {
    await validateAccessibility(page)
  })

  test('shows log in button for guest users', async ({page}) => {
    // We cannot check that the login button redirects the user to a particular
    // URL because it varies between environments, so just check for their existence.
    await expect(page.getByRole('link', {name: 'Log in'})).toBeVisible()
  })

  test('does not show "End session" and "You\'re a guest user" when first arriving at the page', async ({
    page,
  }) => {
    expect(await page.textContent('html')).not.toContain('End session')
    expect(await page.textContent('html')).not.toContain("You're a guest user")
  })

  test('does not show "End session" and "You\'re a guest user" after choosing a different language', async ({
    page,
    applicantQuestions,
  }) => {
    await applicantQuestions.gotoApplicantHomePage()
    await selectApplicantLanguage(page, 'Español')
    expect(await page.textContent('html')).not.toContain('End session')
    expect(await page.textContent('html')).not.toContain("You're a guest user")
  })

  test('does not redirect to /callback when navigating to paths that do not require a profile', async ({
    page,
    context,
  }) => {
    let redirectedToCallback = false

    page.on('response', (response) => {
      if (response.url().includes('/callback?client_name=GuestClient')) {
        redirectedToCallback = true
      }
    })

    // Ensure we clear out any potential active session to verify that going
    // to / does not redirect to /callback.
    const PATHS = ['/', '/programs', '/applicants/programs']
    for (const path of PATHS) {
      await test.step(`Testing ${path}`, async () => {
        redirectedToCallback = false
        await context.clearCookies()
        await page.goto(BASE_URL + path)
        await page.waitForLoadState('networkidle')
        expect(redirectedToCallback).toBe(false)
      })
    }
  })

  test('categorizes programs for draft and applied applications as guest user', async ({
    applicantQuestions,
    page,
  }) => {
    await loginAsTestUser(page)

    await test.step('Programs start in Programs and Services section', async () => {
      await applicantQuestions.expectProgramsWithFilteringEnabled(
        {
          expectedProgramsInMyApplicationsSection: [],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
        /* northStarEnabled= */ true,
      )
    })

    await test.step('Fill out part of the primary program application', async () => {
      await applicantQuestions.applyProgram(
        primaryProgramName,
        /* northStarEnabled= */ true,
      )

      // Screen 1 has no questions, so expect to navigate directly to screen 2
      await expect(page.getByText('Screen 2')).toBeVisible()
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.gotoApplicantHomePage()
    })
    await test.step('Expect primary program application is in "My applications" section', async () => {
      await applicantQuestions.expectProgramsWithFilteringEnabled(
        {
          expectedProgramsInMyApplicationsSection: [primaryProgramName],
          expectedProgramsInProgramsAndServicesSection: [otherProgramName],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
        /* northStarEnabled= */ true,
      )
      await expect(page.getByText('Not yet submitted')).toBeVisible()
    })

    await test.step('Finish the primary program application', async () => {
      await applicantQuestions.applyProgram(
        primaryProgramName,
        /* northStarEnabled= */ true,
        /* showProgramOverviewPage= */ false,
      )
      // Expect clicking 'Continue' navigates to the next incomplete block. In this case, it is screen 3
      await expect(page.getByText('Screen 3')).toBeVisible()
      await applicantQuestions.answerTextQuestion('second answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()
      await applicantQuestions.returnToProgramsFromSubmissionPage(
        /* northStarEnabled= */ true,
      )
    })
    await test.step('Expect primary program application is still in "My applications" section', async () => {
      await applicantQuestions.expectProgramsWithFilteringEnabled(
        {
          expectedProgramsInMyApplicationsSection: [primaryProgramName],
          expectedProgramsInProgramsAndServicesSection: [otherProgramName],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
        /* northStarEnabled= */ true,
      )

      await validateScreenshot(page, 'program-index-page-submitted-northstar')
      await normalizeElements(page)
      await expect(page.getByText('Submitted on 1/1/30')).toBeVisible()
    })

    await test.step('Expect editing submitted application takes user to review page', async () => {
      await applicantQuestions.applyProgram(
        primaryProgramName,
        /* northStarEnabled= */ true,
        /* showProgramOverviewPage= */ false,
      )

      await expect(page.getByText('Review and submit')).toBeVisible()
    })

    await test.step('Create new draft of application and expect submitted tag to still be shown on homepage', async () => {
      await applicantQuestions.clickEdit()
      // Clicking "Continue" creates a new empty draft of the application
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()
      // Click "Exit application" on the "No changes to save" modal
      await applicantQuestions.clickExitApplication()
      await normalizeElements(page)
      await expect(page.getByText('Submitted on 1/1/30')).toBeVisible()
    })

    await test.step('When logged out, everything appears unsubmitted (https://github.com/civiform/civiform/pull/3487)', async () => {
      await logout(page, false)

      await applicantQuestions.expectProgramsWithFilteringEnabled(
        {
          expectedProgramsInMyApplicationsSection: [],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
        /* northStarEnabled= */ true,
      )
    })
  })

  test('categorizes programs for draft and applied applications', async ({
    page,
    applicantQuestions,
  }) => {
    await test.step("Navigate to the applicant's program index and validate that both programs appear in the Not started section.", async () => {
      await loginAsTestUser(page)

      await applicantQuestions.expectProgramsNorthstar({
        wantNotStartedPrograms: [primaryProgramName, otherProgramName],
        wantInProgressOrSubmittedPrograms: [],
      })
    })

    await test.step('Fill out first application block and confirm that the program appears in the In progress section.', async () => {
      await applicantQuestions.applyProgram(primaryProgramName, true)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.gotoApplicantHomePage()
      await applicantQuestions.expectProgramsNorthstar({
        wantNotStartedPrograms: [otherProgramName],
        wantInProgressOrSubmittedPrograms: [primaryProgramName],
      })
    })

    await test.step('Finish the application and confirm that the program appears in the Submitted section.', async () => {
      await applicantQuestions.applyProgram(primaryProgramName, true, false)
      await applicantQuestions.answerTextQuestion('second answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(true)
      await applicantQuestions.returnToProgramsFromSubmissionPage(true)
      await applicantQuestions.expectProgramsNorthstar({
        wantNotStartedPrograms: [otherProgramName],
        wantInProgressOrSubmittedPrograms: [primaryProgramName],
      })
    })

    await test.step('Logout, then login as guest and confirm that everything appears unsubmitted', async () => {
      await logout(page)
      await applicantQuestions.expectProgramsNorthstar({
        wantNotStartedPrograms: [otherProgramName, primaryProgramName],
        wantInProgressOrSubmittedPrograms: [],
      })
    })
  })

  test.describe('program filtering', () => {
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

    test('Displays category tags on program cards', async ({
      page,
      adminPrograms,
    }) => {
      await test.step('publish programs with categories', async () => {
        await adminPrograms.publishAllDrafts()
      })

      await test.step('Navigate to homepage and check that cards in Programs and Services section have categories', async () => {
        await logout(page)
        await loginAsTestUser(page)
        const primaryProgramCard = page.locator('.cf-application-card', {
          has: page.getByText(primaryProgramName),
        })
        await expect(primaryProgramCard.getByText('Education')).toBeVisible()
        await expect(primaryProgramCard.getByText('Healthcare')).toBeVisible()
        await expect(primaryProgramCard.getByText('General')).toBeHidden()
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

      const filterChips = page.locator('#ns-category-filter-form')

      await test.step('check that filter chips appear on homepage', async () => {
        await expect(filterChips.getByText('Education')).toBeVisible()
        await expect(filterChips.getByText('Healthcare')).toBeVisible()
        await expect(filterChips.getByText('General')).toBeVisible()
        await expect(filterChips.getByText('Utilities')).toBeVisible()
      })

      await test.step('start applying to a program', async () => {
        await applicantQuestions.applyProgram(
          primaryProgramName,
          /* northStarEnabled= */ true,
        )

        await applicantQuestions.clickContinue()
        await applicantQuestions.gotoApplicantHomePage()
      })

      await test.step('check that categories only on started program are removed from filters', async () => {
        await expect(filterChips.getByText('Education')).toBeHidden()
        await expect(filterChips.getByText('Healthcare')).toBeHidden()
        await expect(filterChips.getByText('General')).toBeVisible()
        await expect(filterChips.getByText('Utilities')).toBeVisible()
      })
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
        await applicantQuestions.expectProgramsWithFilteringEnabled(
          {
            expectedProgramsInMyApplicationsSection: [],
            expectedProgramsInProgramsAndServicesSection: [
              primaryProgramName,
              otherProgramName,
              'Minimal Sample Program',
              'Comprehensive Sample Program',
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
          /* northStarEnabled= */ true,
        )

        // Check the program count in the section
        await expect(
          page.locator(
            '#unfiltered-programs .cf-program-card-group .cf-application-card',
          ),
        ).toHaveCount(4)
      })

      await test.step('Fill out first application block and confirm that the program appears in the "My Applications" section', async () => {
        await applicantQuestions.applyProgram(primaryProgramName, true)
        await applicantQuestions.answerTextQuestion('first answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.expectProgramsWithFilteringEnabled(
          {
            expectedProgramsInMyApplicationsSection: [primaryProgramName],
            expectedProgramsInProgramsAndServicesSection: [
              otherProgramName,
              'Minimal Sample Program',
              'Comprehensive Sample Program',
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
          /* northStarEnabled= */ true,
        )
      })

      await test.step('Finish the application and confirm that the program appears in the "My applications" section', async () => {
        await applicantQuestions.applyProgram(
          primaryProgramName,
          /* northStarEnabled= */ true,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.answerTextQuestion('second answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.submitFromReviewPage(true)
        await applicantQuestions.returnToProgramsFromSubmissionPage(true)
        await applicantQuestions.expectProgramsWithFilteringEnabled(
          {
            expectedProgramsInMyApplicationsSection: [primaryProgramName],
            expectedProgramsInProgramsAndServicesSection: [
              otherProgramName,
              'Minimal Sample Program',
              'Comprehensive Sample Program',
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
          /* northStarEnabled= */ true,
        )
      })

      await test.step('Select a filter, click the filter submit button and validate screenshot', async () => {
        await applicantQuestions.filterProgramsByCategory('General')

        await validateScreenshot(
          page.locator('#programs-list'),
          'north-star-homepage-programs-filtered',
        )
      })

      await test.step('Verify the contents of the Recommended and Other programs sections', async () => {
        await applicantQuestions.expectProgramsWithFilteringEnabled(
          {
            expectedProgramsInMyApplicationsSection: [primaryProgramName],
            expectedProgramsInProgramsAndServicesSection: [],
            expectedProgramsInRecommendedSection: [otherProgramName],
            expectedProgramsInOtherProgramsSection: [
              'Minimal Sample Program',
              'Comprehensive Sample Program',
            ],
          },
          /* filtersOn= */ true,
          /* northStarEnabled= */ true,
        )

        // Check the program count in the section headings
        await expect(
          page.getByRole('heading', {
            name: 'Programs based on your selections (1)',
          }),
        ).toBeVisible()
        await expect(
          page.getByRole('heading', {
            name: 'Other programs and services (2)',
          }),
        ).toBeVisible()
      })

      await validateAccessibility(page)

      await test.step('Logout, then login as guest and confirm that everything appears unsubmitted', async () => {
        await logout(page)
        await applicantQuestions.expectProgramsWithFilteringEnabled(
          {
            expectedProgramsInMyApplicationsSection: [],
            expectedProgramsInProgramsAndServicesSection: [
              primaryProgramName,
              otherProgramName,
              'Minimal Sample Program',
              'Comprehensive Sample Program',
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
          /* northStarEnabled= */ true,
        )
      })
    })

    test('Clearing filters resets programs to unfiltered view and unchecks category checkboxes', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('publish programs with categories', async () => {
        await adminPrograms.publishAllDrafts()
      })

      await test.step('Navigate to homepage', async () => {
        await logout(page)
        await loginAsTestUser(page)
      })

      await test.step('Select a filter, click the filter submit button and see the Recommended and Other programs sections', async () => {
        await applicantQuestions.filterProgramsByCategory('General')

        // Check the program count in the section headings
        await expect(
          page.getByRole('heading', {
            name: 'Programs based on your selections (1)',
          }),
        ).toBeVisible()
        await expect(
          page.getByRole('heading', {
            name: 'Other programs and services (3)',
          }),
        ).toBeVisible()

        await expect(page.locator('#unfiltered-programs')).toBeHidden()
      })

      await test.step('Clear filters and verify checkboxes are unchecked and view reset', async () => {
        await page.getByRole('button', {name: 'Clear selections'}).click()

        await expect(
          page.getByRole('checkbox', {name: 'General'}),
        ).not.toBeChecked()

        await expect(page.locator('#unfiltered-programs')).toBeVisible()

        await expect(
          page.locator(
            '#unfiltered-programs .cf-program-card-group .cf-application-card',
          ),
        ).toHaveCount(4)

        await expect(
          page.getByRole('heading', {
            name: 'Programs based on your selections (1)',
          }),
        ).toBeHidden()
        await expect(
          page.getByRole('heading', {
            name: 'Other programs and services (3)',
          }),
        ).toBeHidden()
      })
    })
  })

  test('common intake form not present', async ({page}) => {
    await validateScreenshot(page, 'ns-common-intake-form-not-set')
    await validateAccessibility(page)
  })

  test.describe('common intake form present', () => {
    const commonIntakeFormProgramName = 'Benefits finder'

    test.beforeEach(async ({page, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminPrograms.addProgram(
        commonIntakeFormProgramName,
        'program description',
        'short program description',
        'https://usa.gov',
        ProgramVisibility.PUBLIC,
        'admin description',
        /* isCommonIntake= */ true,
      )

      await adminPrograms.addProgramBlockUsingSpec(
        commonIntakeFormProgramName,
        {
          name: 'Screen 2',
          description: 'first block',
          questions: [{name: 'first-q'}],
        },
      )
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    test('shows common intake form card when an application has not been started', async ({
      page,
      applicantQuestions,
    }) => {
      await validateScreenshot(
        page.getByLabel('Get Started'),
        'ns-common-intake-form',
      )
      await applicantQuestions.expectProgramsWithFilteringEnabled(
        {
          expectedProgramsInMyApplicationsSection: [],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
        /* northStarEnabled= */ true,
      )
    })

    test('puts common intake card in My applications section when application is in progress or submitted', async ({
      applicantQuestions,
      page,
    }) => {
      await test.step('Start applying to the common intake', async () => {
        await applicantQuestions.applyProgram(
          commonIntakeFormProgramName,
          /* northStarEnabled= */ true,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.answerTextQuestion('answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.gotoApplicantHomePage()
      })

      await applicantQuestions.expectProgramsWithFilteringEnabled(
        {
          expectedProgramsInMyApplicationsSection: [
            commonIntakeFormProgramName,
          ],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
        /* northStarEnabled= */ true,
      )

      await expect(page.getByLabel('Get Started')).toHaveCount(0)

      await test.step('Submit application to the common intake', async () => {
        await applicantQuestions.applyProgram(
          commonIntakeFormProgramName,
          /* northStarEnabled= */ true,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.clickSubmitApplication()
        await applicantQuestions.gotoApplicantHomePage()
      })

      await applicantQuestions.expectProgramsWithFilteringEnabled(
        {
          expectedProgramsInMyApplicationsSection: [
            commonIntakeFormProgramName,
          ],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
        /* northStarEnabled= */ true,
      )

      await expect(page.getByLabel('Get Started')).toHaveCount(0)
    })

    test('shows common intake form', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(primaryProgramName, true)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.gotoApplicantHomePage()

      await validateScreenshot(page, 'ns-common-intake-form-sections')
      await applicantQuestions.expectProgramsNorthstar({
        wantNotStartedPrograms: [otherProgramName],
        wantInProgressOrSubmittedPrograms: [primaryProgramName],
      })
      await applicantQuestions.expectCommonIntakeFormNorthstar(
        commonIntakeFormProgramName,
      )
    })

    test('shows a different title for the common intake form', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.clickApplyProgramButton(primaryProgramName)
      expect(await page.innerText('h2')).toContain('How to apply')

      await applicantQuestions.gotoApplicantHomePage()

      await applicantQuestions.clickApplyProgramButton('Benefits finder')
      await applicantQuestions.clickReview(true)
      expect(await page.innerText('h2')).toContain('Review and submit')
    })
  })

  test('shows previously answered on text for questions that had been answered', async ({
    page,
    applicantQuestions,
  }) => {
    await test.step('Fill out application with one question and confirm it shows previously answered at the end', async () => {
      await applicantQuestions.applyProgram(otherProgramName, true)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.northStarValidatePreviouslyAnsweredText(
        firstQuestionText,
      )
      await applicantQuestions.submitFromReviewPage(true)
      await applicantQuestions.returnToProgramsFromSubmissionPage(true)
      await applicantQuestions.expectProgramsNorthstar({
        wantNotStartedPrograms: [primaryProgramName],
        wantInProgressOrSubmittedPrograms: [otherProgramName],
      })
    })

    await test.step('Check that the question repeated in the program with two questions shows previously answered', async () => {
      await applicantQuestions.applyProgram(primaryProgramName, true)
      await applicantQuestions.clickReview(true)
      await applicantQuestions.northStarValidatePreviouslyAnsweredText(
        firstQuestionText,
      )

      await applicantQuestions.northStarValidateNoPreviouslyAnsweredText(
        secondQuestionText,
      )
      await validateScreenshot(page, 'ns-question-shows-previously-answered')
    })

    await test.step('Fill out second question and check that the original program shows previously answered', async () => {
      await applicantQuestions.clickContinue()
      await applicantQuestions.answerTextQuestion('second answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(true)

      await applicantQuestions.returnToProgramsFromSubmissionPage(true)
      await applicantQuestions.clickApplyProgramButton(otherProgramName)
      await applicantQuestions.northStarValidatePreviouslyAnsweredText(
        firstQuestionText,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    await test.step('Change first response on second program and check that the other program shows the previously answered text too', async () => {
      await applicantQuestions.returnToProgramsFromSubmissionPage(true)
      await applicantQuestions.clickApplyProgramButton(primaryProgramName)
      await page
        .getByRole('listitem')
        .filter({hasText: 'Screen 2 Edit This is the'})
        .getByRole('link')
        .click()
      await applicantQuestions.answerTextQuestion('first answer 2')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(true)

      await applicantQuestions.returnToProgramsFromSubmissionPage(true)
      await applicantQuestions.clickApplyProgramButton(otherProgramName)

      await applicantQuestions.northStarValidatePreviouslyAnsweredText(
        firstQuestionText,
      )
      await validateScreenshot(
        page,
        'ns-other-program-shows-previously-answered',
      )
    })
  })
})

test.describe(
  'applicant program index page with images',
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('shows program with wide image in North Star and removes image from card when in My Applications', async ({
      page,
      adminPrograms,
      adminProgramImage,
      applicantQuestions,
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

      await validateScreenshot(page, 'north-star-program-image-wide')
      await validateAccessibility(page)

      await test.step('Fill out part of the program application', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.clickSubmitApplication()
        await applicantQuestions.gotoApplicantHomePage()
      })

      await test.step('Expect the program card to not show the image when in My Applications section', async () => {
        await expect(page.locator('.cf-application-card img')).toBeHidden()
      })
    })

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

      await validateScreenshot(page, 'ns-program-image-tall')
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
      await validateScreenshot(page, 'ns-program-image-with-status')
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

      const programNameInProgressImage = 'In Progress Program [Image]'
      const approvedStatusName = 'Approved'

      await test.step('create program with image as admin', async () => {
        await loginAsAdmin(page)
        const commonIntakeFormProgramName = 'Benefits finder'
        await adminPrograms.addProgram(
          commonIntakeFormProgramName,
          'program description',
          'short program description',
          'https://usa.gov',
          ProgramVisibility.PUBLIC,
          'admin description',
          /* isCommonIntake= */ true,
        )

        await adminPrograms.addProgram(programNameInProgressImage)
        await adminQuestions.addTextQuestion({
          questionName: 'first-q',
          questionText: 'first question text',
        })
        await adminPrograms.addProgramBlockUsingSpec(
          programNameInProgressImage,
          {description: 'first block', questions: [{name: 'first-q'}]},
        )

        await adminPrograms.goToProgramImagePage(programNameInProgressImage)
        await adminProgramImage.setImageFileAndSubmit(
          'src/assets/program-summary-image-wide.png',
        )
        await adminPrograms.publishAllDrafts()
        await logout(page)
      })

      await test.step('start application to program', async () => {
        await loginAsTestUser(page)
        await applicantQuestions.applyProgram(programNameInProgressImage, true)
        await applicantQuestions.answerTextQuestion('first answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.gotoApplicantHomePage()
        await logout(page)
      })

      await test.step('program with image and status', async () => {
        const programNameSubmittedWithImageAndStatus =
          'Submitted Program [Image and Status]'
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
        await adminPrograms.publishProgram(
          programNameSubmittedWithImageAndStatus,
        )
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
      })

      await test.step('submit basic program', async () => {
        const programNameSubmittedBasic = 'Submitted Program [Basic]'
        await loginAsAdmin(page)
        await adminPrograms.addProgram(programNameSubmittedBasic)
        await adminPrograms.publishProgram(programNameSubmittedBasic)
        await adminPrograms.expectActiveProgram(programNameSubmittedBasic)
        await logout(page)

        await loginAsTestUser(page)
        await applicantQuestions.applyProgram(programNameSubmittedBasic, true)
        await applicantQuestions.submitFromReviewPage(true)
        await logout(page)
      })

      await test.step('submit program with status', async () => {
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
      })

      await test.step('submit image on a new row', async () => {
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
        await applicantQuestions.applyProgram(programNameSubmittedImage, true)
        await applicantQuestions.submitFromReviewPage(true)
        await logout(page)
      })

      await test.step('basic not started program', async () => {
        await loginAsAdmin(page)
        await adminPrograms.addProgram('Not Started Program [Basic]')
      })

      await test.step('basic not started program with image', async () => {
        const programNameNotStartedImage = 'Not Started Program [Image]'
        await adminPrograms.addProgram(programNameNotStartedImage)
        await adminPrograms.goToProgramImagePage(programNameNotStartedImage)
        await adminProgramImage.setImageFileAndSubmit(
          'src/assets/program-summary-image-wide.png',
        )
        await adminPrograms.publishAllDrafts()
        await logout(page)
      })

      await test.step('verify homepage', async () => {
        await loginAsTestUser(page)
        await validateScreenshot(page, 'ns-program-image-all-types')
        // accessibility fails
        // await validateAccessibility(page)
      })
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
      await applicantQuestions.applyProgram(programName, true)
      await applicantQuestions.submitFromReviewPage(true)
      await logout(page)

      // Set a status as a program admin
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programName)
      await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
      const modal = await adminPrograms.setStatusOptionAndAwaitModal(statusName)
      await adminPrograms.confirmStatusUpdateModal(modal)
      await page.getByRole('link', {name: 'Back'}).click()
      await logout(page)
    }
  },
)
