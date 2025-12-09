import {test, expect} from '../support/civiform_fixtures'
import {
  ApplicantQuestions,
  AdminPrograms,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  normalizeElements,
  selectApplicantLanguage,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'
import {Locator, Page} from 'playwright'
import {ProgramCategories, ProgramVisibility} from '../support/admin_programs'
import {BASE_URL} from '../support/config'

test.describe('applicant program index page', {tag: ['@northstar']}, () => {
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

    await expect(page.getByText(/Discover services you may/)).toBeVisible()
  })

  test('validate initial page load as guest user', async ({
    page,
    applicantQuestions,
  }) => {
    await validateScreenshot(page, 'program-index-page-initial-load')
    await applicantQuestions.expectTitle(page, 'Find programs')
  })

  test('validate accessibility and validate skip link', async ({page}) => {
    const skipLinkLocator: Locator = page.getByRole('link', {
      name: 'Skip to main content',
    })
    await test.step('Tab and verify focus on skip link', async () => {
      await page.keyboard.press('Tab')
      await expect(skipLinkLocator).toBeFocused()
      await expect(skipLinkLocator).toBeVisible()
    })

    await test.step('Click on skip link and skip to main content', async () => {
      await skipLinkLocator.click()
      await expect(page.locator('main')).toBeFocused()
      await page.keyboard.press('Tab')
      await expect(
        page.getByRole('link', {name: 'View and apply'}).first(),
      ).toBeFocused()
    })

    await validateAccessibility(page)
  })

  test('shows log in button for guest users', async ({page}) => {
    // We cannot check that the login button redirects the user to a particular
    // URL because it varies between environments, so just check for their existence.
    await expect(page.getByRole('button', {name: 'Log in'})).toBeVisible()
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
    await selectApplicantLanguage(page, 'es-US')
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
        await waitForPageJsLoad(page)
        expect(redirectedToCallback).toBe(false)
      })
    }
  })

  test('Puts a submitted tag on program card when application submitted', async ({
    applicantQuestions,
    page,
  }) => {
    await loginAsTestUser(page)

    await test.step('Apply to the primary program', async () => {
      await applicantQuestions.applyProgram(primaryProgramName)
      // Screen 1 has no questions, so expect to navigate directly to screen 2
      await expect(page.getByText('Screen 2')).toBeVisible()
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
      // Expect clicking 'Continue' navigates to the next incomplete block. In this case, it is screen 3
      await expect(page.getByText('Screen 3')).toBeVisible()
      await applicantQuestions.answerTextQuestion('second answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()
      await applicantQuestions.returnToProgramsFromSubmissionPage()
    })

    await test.step('Expect submitted tag shows on program card', async () => {
      await normalizeElements(page)
      await expect(page.getByText('Submitted on 1/1/30')).toBeVisible()
    })

    await test.step('Expect editing submitted application takes user to review page', async () => {
      await applicantQuestions.applyProgram(
        primaryProgramName,
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
  })

  test.describe('program filtering', () => {
    const externalProgramName = 'External Program'

    test.beforeEach(async ({page, adminPrograms, seeding}) => {
      await enableFeatureFlag(page, 'external_program_cards_enabled')

      await test.step('seed categories', async () => {
        await seeding.seedProgramsAndCategories()
        await page.goto('/')
      })

      await test.step('add categories to primary and other program', async () => {
        await loginAsAdmin(page)
        await adminPrograms.selectProgramCategories(
          primaryProgramName,
          [ProgramCategories.EDUCATION, ProgramCategories.HEALTHCARE],
          /* isActive= */ true,
        )
        await adminPrograms.selectProgramCategories(
          otherProgramName,
          [ProgramCategories.GENERAL, ProgramCategories.UTILITIES],
          /* isActive= */ true,
        )
      })

      await test.step('add external program with categories', async () => {
        await adminPrograms.addExternalProgram(
          externalProgramName,
          /* shortDescription= */ 'description',
          /* externalLink= */ 'https://usa.gov',
          ProgramVisibility.PUBLIC,
        )
        await adminPrograms.selectProgramCategories(
          externalProgramName,
          [ProgramCategories.GENERAL],
          /* isActive= */ false,
        )
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

        const externalProgramCard = page.locator('.cf-application-card', {
          has: page.getByText(externalProgramName),
        })
        await expect(externalProgramCard.getByText('Education')).toBeHidden()
        await expect(externalProgramCard.getByText('Healthcare')).toBeHidden()
        await expect(externalProgramCard.getByText('General')).toBeVisible()
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
        await applicantQuestions.applyProgram(primaryProgramName)

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

    test('formats filter chips correctly for right to left languages', async ({
      page,
      adminPrograms,
    }) => {
      await test.step('publish programs with categories', async () => {
        await adminPrograms.publishAllDrafts()
        await logout(page)
      })

      await test.step('change language to Arabic', async () => {
        await selectApplicantLanguage(page, 'ar')
      })

      await test.step('validate screenshot desktop', async () => {
        await validateScreenshot(page, 'filter-chips-right-to-left-desktop')
      })

      await test.step('validate screenshot mobile', async () => {
        await page.setViewportSize({width: 360, height: 800})
        await validateScreenshot(page, 'filter-chips-right-to-left-mobile', {
          fullPage: false,
        })
      })
    })

    test('with program filters selected, categorizes programs correctly', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('publish programs with categories', async () => {
        await adminPrograms.publishAllDrafts()
      })

      await test.step('Navigate to program index and validate that all programs appear in Programs and Services', async () => {
        await logout(page)
        await applicantQuestions.expectProgramsinCorrectSections(
          {
            expectedProgramsInMyApplicationsSection: [],
            expectedProgramsInProgramsAndServicesSection: [
              primaryProgramName,
              otherProgramName,
              'Minimal Sample Program',
              'Comprehensive Sample Program',
              externalProgramName,
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
        )

        // Check the program count in the section
        await expect(
          page.locator(
            '#unfiltered-programs .usa-card-group .cf-application-card',
          ),
        ).toHaveCount(5)
      })

      await test.step('Fill out first application block and confirm that the program appears in the "My Applications" section', async () => {
        await applicantQuestions.applyProgram(primaryProgramName)
        await applicantQuestions.answerTextQuestion('first answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.gotoApplicantHomePage()
        await applicantQuestions.expectProgramsinCorrectSections(
          {
            expectedProgramsInMyApplicationsSection: [primaryProgramName],
            expectedProgramsInProgramsAndServicesSection: [
              otherProgramName,
              'Minimal Sample Program',
              'Comprehensive Sample Program',
              externalProgramName,
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
        )
      })

      await test.step('Finish the application and confirm that the program appears in the "My applications" section', async () => {
        await applicantQuestions.applyProgram(
          primaryProgramName,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.answerTextQuestion('second answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.submitFromReviewPage()
        await applicantQuestions.returnToProgramsFromSubmissionPage(true)
        await applicantQuestions.expectProgramsinCorrectSections(
          {
            expectedProgramsInMyApplicationsSection: [primaryProgramName],
            expectedProgramsInProgramsAndServicesSection: [
              otherProgramName,
              'Minimal Sample Program',
              'Comprehensive Sample Program',
              externalProgramName,
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
        )
      })

      await test.step('Select a filter, click the filter submit button and validate screenshot', async () => {
        await applicantQuestions.filterProgramsByCategory('General')

        await validateScreenshot(
          page.locator('#programs-list'),
          'homepage-programs-filtered',
        )
      })

      await test.step('Verify the contents of the Recommended and Other programs sections', async () => {
        await applicantQuestions.expectProgramsinCorrectSections(
          {
            expectedProgramsInMyApplicationsSection: [primaryProgramName],
            expectedProgramsInProgramsAndServicesSection: [],
            expectedProgramsInRecommendedSection: [
              otherProgramName,
              externalProgramName,
            ],
            expectedProgramsInOtherProgramsSection: [
              'Minimal Sample Program',
              'Comprehensive Sample Program',
            ],
          },
          /* filtersOn= */ true,
        )

        // Check the program count in the section headings
        await expect(
          page.getByRole('heading', {
            name: 'Programs based on your selections (2)',
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
        await applicantQuestions.expectProgramsinCorrectSections(
          {
            expectedProgramsInMyApplicationsSection: [],
            expectedProgramsInProgramsAndServicesSection: [
              primaryProgramName,
              otherProgramName,
              'Minimal Sample Program',
              'Comprehensive Sample Program',
              externalProgramName,
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
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
            name: 'Programs based on your selections (2)',
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

        await expect(page.locator('#not-started-programs')).toBeVisible()

        await expect(
          page.locator(
            '#not-started-programs .usa-card-group .cf-application-card',
          ),
        ).toHaveCount(5)

        await expect(
          page.getByRole('heading', {
            name: 'Programs based on your selections (2)',
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

  test('pre-screener form not present', async ({page}) => {
    await validateScreenshot(page, 'pre-screener-form-not-set')
    await validateAccessibility(page)
  })

  test.describe('pre-screener form present', () => {
    const preScreenerFormProgramName = 'Benefits finder'

    test.beforeEach(async ({page, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminPrograms.addPreScreener(
        preScreenerFormProgramName,
        'short program description',
        ProgramVisibility.PUBLIC,
      )

      await adminPrograms.addProgramBlockUsingSpec(preScreenerFormProgramName, {
        name: 'Screen 2',
        description: 'first block',
        questions: [{name: 'first-q'}],
      })
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    test('shows pre-screener form card when an application has not been started', async ({
      page,
      applicantQuestions,
    }) => {
      await validateScreenshot(
        page.getByLabel('Get Started'),
        'pre-screener-form',
      )
      await applicantQuestions.expectProgramsinCorrectSections(
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
      )
    })

    test('puts pre-screener card in My applications section when application is in progress or submitted', async ({
      applicantQuestions,
      page,
    }) => {
      await test.step('Start applying to the pre-screener', async () => {
        await applicantQuestions.applyProgram(
          preScreenerFormProgramName,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.answerTextQuestion('answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.gotoApplicantHomePage()
      })

      await applicantQuestions.expectProgramsinCorrectSections(
        {
          expectedProgramsInMyApplicationsSection: [preScreenerFormProgramName],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
      )

      await validateScreenshot(
        page.locator('.cf-application-card', {
          has: page.getByText(preScreenerFormProgramName),
        }),
        'pre-screener-form-in-progress',
      )

      await expect(page.getByLabel('Get Started')).toHaveCount(0)

      await test.step('Submit application to the pre-screener', async () => {
        await applicantQuestions.applyProgram(
          preScreenerFormProgramName,
          /* showProgramOverviewPage= */ false,
        )
        await applicantQuestions.clickSubmitApplication()
        await applicantQuestions.gotoApplicantHomePage()
      })

      await applicantQuestions.expectProgramsinCorrectSections(
        {
          expectedProgramsInMyApplicationsSection: [preScreenerFormProgramName],
          expectedProgramsInProgramsAndServicesSection: [
            primaryProgramName,
            otherProgramName,
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
      )

      await expect(page.getByLabel('Get Started')).toHaveCount(0)
    })

    test('shows pre-screener form', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(primaryProgramName)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.gotoApplicantHomePage()

      await validateScreenshot(page, 'pre-screener-form-sections')
      await applicantQuestions.expectPrograms({
        wantNotStartedPrograms: [otherProgramName],
        wantInProgressOrSubmittedPrograms: [primaryProgramName],
      })
      await applicantQuestions.expectPreScreenerForm(preScreenerFormProgramName)
    })

    test('shows a different title for the pre-screener form', async ({
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
      await applicantQuestions.applyProgram(otherProgramName)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
      await applicantQuestions.submitFromReviewPage()
      await applicantQuestions.returnToProgramsFromSubmissionPage(true)
      await applicantQuestions.expectPrograms({
        wantNotStartedPrograms: [primaryProgramName],
        wantInProgressOrSubmittedPrograms: [otherProgramName],
      })
    })

    await test.step('Check that the question repeated in the program with two questions shows previously answered', async () => {
      await applicantQuestions.applyProgram(primaryProgramName)
      await applicantQuestions.clickReview(true)
      await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)

      await applicantQuestions.validateNoPreviouslyAnsweredText(
        secondQuestionText,
      )
      await validateScreenshot(page, 'question-shows-previously-answered')
    })

    await test.step('Fill out second question and check that the original program shows previously answered', async () => {
      await applicantQuestions.clickContinue()
      await applicantQuestions.answerTextQuestion('second answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage()

      await applicantQuestions.returnToProgramsFromSubmissionPage(true)
      await applicantQuestions.clickApplyProgramButton(otherProgramName)
      await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
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
      await applicantQuestions.submitFromReviewPage()

      await applicantQuestions.returnToProgramsFromSubmissionPage(true)
      await applicantQuestions.clickApplyProgramButton(otherProgramName)

      await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
      await validateScreenshot(page, 'other-program-shows-previously-answered')
    })
  })

  test('formats card correctly when description is one long word', async ({
    page,
    adminPrograms,
  }) => {
    await test.step('Add a program with one long word as the description', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(
        'Program with long word description',
        'description',
        'abracadabracadabracadabracadabracadabracadabracadabracadabracadabracadabracadabra',
      )
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await validateScreenshot(
      page.locator('#unfiltered-programs'),
      'applicant-homepage-cards-long-word-description',
    )
  })

  test('formats index page correctly for right to left languages', async ({
    page,
  }) => {
    await test.step('change language to Arabic', async () => {
      await selectApplicantLanguage(page, 'ar')
    })

    await test.step('validate screenshot desktop', async () => {
      await validateScreenshot(page, 'applicant-homepage-right-to-left-desktop')
    })

    await test.step('validate screenshot mobile', async () => {
      await page.setViewportSize({width: 360, height: 800})
      await validateScreenshot(
        page,
        'applicant-homepage-right-to-left-mobile',
        {fullPage: false},
      )
    })
  })

  test('applies color theming on home page when enabled', async ({
    page,
    adminSettings,
  }) => {
    await enableFeatureFlag(page, 'CUSTOM_THEME_COLORS_ENABLED')
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()

    await adminSettings.setStringSetting('THEME_COLOR_PRIMARY', '#6d4bfa')
    await adminSettings.setStringSetting('THEME_COLOR_PRIMARY_DARK', '#a72f10')

    await adminSettings.saveChanges()
    await logout(page)

    await validateScreenshot(page, 'program-index-page-theme')
  })

  test('applies primary color only when primary dark is empty', async ({
    page,
    adminSettings,
  }) => {
    await enableFeatureFlag(page, 'CUSTOM_THEME_COLORS_ENABLED')
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()

    await adminSettings.setStringSetting('THEME_COLOR_PRIMARY', '#6d4bfa')

    await adminSettings.saveChanges()
    await logout(page)

    await validateScreenshot(page, 'program-index-page-theme-primary-only')
  })

  test('applies primary dark color only when primary is empty', async ({
    page,
    adminSettings,
  }) => {
    await enableFeatureFlag(page, 'CUSTOM_THEME_COLORS_ENABLED')
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()

    await adminSettings.setStringSetting('THEME_COLOR_PRIMARY_DARK', '#a72f10')

    await adminSettings.saveChanges()
    await logout(page)

    await validateScreenshot(page, 'program-index-page-theme-primary-dark-only')
  })

  test('does not apply color theming on home page when disabled', async ({
    page,
    adminSettings,
  }) => {
    await disableFeatureFlag(page, 'CUSTOM_THEME_COLORS_ENABLED')
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()

    await adminSettings.setStringSetting('THEME_COLOR_PRIMARY', '#6d4bfa')
    await adminSettings.setStringSetting('THEME_COLOR_PRIMARY_DARK', '#a72f10')

    await adminSettings.saveChanges()
    await logout(page)

    await validateScreenshot(page, 'program-index-page-initial-load')
  })
})

test.describe(
  'applicant program index page with images',
  {tag: ['@northstar']},
  () => {
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

      await validateScreenshot(page, 'program-image-wide')
      await validateAccessibility(page)

      await test.step('Fill out part of the program application', async () => {
        await applicantQuestions.applyProgram(programName)
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

      await loginAsTestUser(page)
      await test.step('verify no available programs info alert appears', async () => {
        await expect(
          page.getByText(
            'You have started or submitted an application for all programs that are available at this time.',
          ),
        ).toBeVisible()
      })

      // Verify program card shows both the Accepted status and image
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

      const programNameInProgressImage = 'In Progress Program [Image]'
      const approvedStatusName = 'Approved'

      await test.step('create program with image as admin', async () => {
        await loginAsAdmin(page)
        const preScreenerFormProgramName = 'Benefits finder'
        await adminPrograms.addPreScreener(
          preScreenerFormProgramName,
          'short program description',
          ProgramVisibility.PUBLIC,
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
        await applicantQuestions.applyProgram(programNameInProgressImage)
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
        await applicantQuestions.applyProgram(programNameSubmittedBasic)
        await applicantQuestions.submitFromReviewPage()
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
        await applicantQuestions.applyProgram(programNameSubmittedImage)
        await applicantQuestions.submitFromReviewPage()
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
        await validateScreenshot(page, 'program-image-all-types')
        // accessibility fails
        // await validateAccessibility(page)
      })
    })

    test('External program card is not shown when feature flag is off', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      const externalProgramName = 'External Program'

      await test.step('add external program as an admin', async () => {
        await loginAsAdmin(page)

        // Feature flag must be enabled to be able to add an external program
        await enableFeatureFlag(page, 'external_program_cards_enabled')
        await adminPrograms.addExternalProgram(
          externalProgramName,
          /* shortDescription= */ 'description',
          /* externalLink= */ 'https://usa.gov',
          ProgramVisibility.PUBLIC,
        )
        await adminPrograms.publishProgram(externalProgramName)
        await logout(page)
      })

      await test.step('disable feature flag', async () => {
        await disableFeatureFlag(page, 'external_program_cards_enabled')
      })

      await test.step('external program card is not shown to applicant', async () => {
        await applicantQuestions.expectProgramsinCorrectSections(
          {
            expectedProgramsInMyApplicationsSection: [],
            expectedProgramsInProgramsAndServicesSection: [],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
        )
      })
    })

    test('External program cards are show when feature flag is on', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      const externalProgramAName = 'External Program A'
      const externalProgramALink = 'https://www.usa.gov'
      const externalProgramBName = 'External Program B'
      const externalProgramBLink = 'https://civiform.us'

      await test.step('add external programs', async () => {
        await enableFeatureFlag(page, 'external_program_cards_enabled')

        await loginAsAdmin(page)
        await adminPrograms.addExternalProgram(
          externalProgramAName,
          /* shortDescription= */ 'description',
          externalProgramALink,
          ProgramVisibility.PUBLIC,
        )
        await adminPrograms.addExternalProgram(
          externalProgramBName,
          /* shortDescription= */ 'description',
          externalProgramBLink,
          ProgramVisibility.PUBLIC,
        )
        await adminPrograms.publishAllDrafts()
        await logout(page)
      })

      await test.step("'Programs and Services' section includes cards for the external programs", async () => {
        await applicantQuestions.expectProgramsinCorrectSections(
          {
            expectedProgramsInMyApplicationsSection: [],
            expectedProgramsInProgramsAndServicesSection: [
              externalProgramAName,
              externalProgramBName,
            ],
            expectedProgramsInRecommendedSection: [],
            expectedProgramsInOtherProgramsSection: [],
          },
          /* filtersOn= */ false,
        )

        // Button for external program card has a different text.
        const externalProgramCard = page.locator('.cf-application-card', {
          has: page.getByText(externalProgramAName),
        })
        await expect(
          externalProgramCard.getByRole('button', {
            name: 'View External Program A in new tab',
          }),
        ).toBeVisible()

        await validateAccessibility(page)
      })

      await test.step('card for external program A opens a modal', async () => {
        await applicantQuestions.clickApplyProgramButton(externalProgramAName)

        // Verify external program modal is visible
        const modal = page.getByRole('dialog', {state: 'visible'})
        await expect(
          modal.getByRole('heading', {
            name: 'This will open a different website',
          }),
        ).toBeVisible()
        await expect(
          modal.getByText(
            "To go to the program's website where you can get more details and apply, click Continue",
          ),
        ).toBeVisible()
        const continueButton = modal.getByRole('link', {name: 'Continue'})
        await expect(continueButton).toBeVisible()
        await expect(modal.getByRole('button', {name: 'Go back'})).toBeVisible()
      })

      await test.step("accepting external program A modal redirects to the program's external site", async () => {
        const modal = page.getByRole('dialog', {state: 'visible'})
        const continueButton = modal.getByRole('link', {name: 'Continue'})

        const pagePromise = page.context().waitForEvent('page')
        await continueButton.click()
        const newPage = await pagePromise
        await newPage.waitForLoadState()
        expect(newPage.url()).toMatch(externalProgramALink)

        await newPage.close()
      })

      await test.step('go back to the applicant home page', async () => {
        await page.goto(BASE_URL)
      })

      await test.step('card for external program B opens a modal', async () => {
        await applicantQuestions.clickApplyProgramButton(externalProgramBName)

        // We don't need to check each modal element's visibility since
        // previous step verified them
        const modal = page.getByRole('dialog', {state: 'visible'})
        const continueButton = modal.getByRole('link', {name: 'Continue'})
        await expect(continueButton).toBeVisible()
      })

      await test.step("accepting external program B modal redirects to the program's external site", async () => {
        const modal = page.getByRole('dialog', {state: 'visible'})
        const continueButton = modal.getByRole('link', {name: 'Continue'})

        const pagePromise = page.context().waitForEvent('page')
        await continueButton.click()
        const newPage = await pagePromise
        await newPage.waitForLoadState()
        expect(newPage.url()).toMatch(externalProgramBLink)

        await newPage.close()
      })
    })

    test('External program cards are included in program filters', async ({
      page,
      adminPrograms,
      applicantQuestions,
      seeding,
    }) => {
      const externalProgramName = 'External Program'
      const externalProgramLink = 'https://civiform.us'

      await test.step('enable required features', async () => {
        await enableFeatureFlag(page, 'external_program_cards_enabled')
      })

      await test.step('seed categories', async () => {
        // The program filtering feature requires seeding the categories. This
        // will add two programs: "Comprehensive Sample Program" and "Minimal
        // Sample Program".
        await seeding.seedProgramsAndCategories()
        await page.goto('/')
      })

      await test.step("add an external program with 'Education' category", async () => {
        await loginAsAdmin(page)
        await adminPrograms.addExternalProgram(
          externalProgramName,
          /* shortDescription= */ 'description',
          externalProgramLink,
          ProgramVisibility.PUBLIC,
        )
        await adminPrograms.selectProgramCategories(
          externalProgramName,
          [ProgramCategories.EDUCATION],
          /* isActive= */ false,
        )

        await adminPrograms.publishAllDrafts()
        await logout(page)
      })

      await test.step("filter programs by 'Education' category", async () => {
        await loginAsTestUser(page)
        await applicantQuestions.filterProgramsByCategory('Education')
      })

      await test.step("external program card appeasrs in the 'Recommended' section", async () => {
        await applicantQuestions.expectProgramsinCorrectSections(
          {
            expectedProgramsInMyApplicationsSection: [],
            expectedProgramsInProgramsAndServicesSection: [],
            expectedProgramsInRecommendedSection: [externalProgramName],
            expectedProgramsInOtherProgramsSection: [
              'Comprehensive Sample Program',
              'Minimal Sample Program',
            ],
          },
          /* filtersOn= */ true,
        )
      })

      await test.step('clicking on external program card opens a modal', async () => {
        await applicantQuestions.clickApplyProgramButton(externalProgramName)

        const modal = page.getByRole('dialog', {state: 'visible'})
        const continueButton = modal.getByRole('link', {name: 'Continue'})
        await expect(continueButton).toBeVisible()
      })

      await test.step("selecting 'go back' closes the modal", async () => {
        const modal = page.getByRole('dialog', {state: 'visible'})
        const goBackButton = modal.getByRole('button', {name: 'Go back'})
        await expect(goBackButton).toBeVisible()
        await goBackButton.click()
        await expect(modal).toBeHidden()
      })

      await test.step('trigger the external program modal again', async () => {
        await applicantQuestions.clickApplyProgramButton(externalProgramName)

        const modal = page.getByRole('dialog', {state: 'visible'})
        const continueButton = modal.getByRole('link', {name: 'Continue'})
        await expect(continueButton).toBeVisible()
      })

      await test.step('clicking outside the modal closes the modal', async () => {
        const modalWrapper = page
          .locator('.usa-modal-wrapper')
          .filter({hasText: 'This will open a different website'})
        const wrapperBox = await modalWrapper.boundingBox()
        // Click in the wrapper top left corner, which should  be outside the
        // actual amodal
        if (wrapperBox) {
          await page.mouse.click(wrapperBox.x, wrapperBox.y)
        }

        const modal = page.getByRole('dialog', {state: 'visible'})
        await expect(modal).toBeHidden()
      })

      await test.step('trigger the external program modal again', async () => {
        await applicantQuestions.clickApplyProgramButton(externalProgramName)

        const modal = page.getByRole('dialog', {state: 'visible'})
        const continueButton = modal.getByRole('link', {name: 'Continue'})
        await expect(continueButton).toBeVisible()
      })

      await test.step("selecting 'continue' redirects to the program's external site", async () => {
        const modal = page.getByRole('dialog', {state: 'visible'})
        const continueButton = modal.getByRole('link', {name: 'Continue'})

        const pagePromise = page.context().waitForEvent('page')
        await continueButton.click()
        const newPage = await pagePromise
        await newPage.waitForLoadState()
        expect(newPage.url()).toMatch(externalProgramLink)

        await newPage.close()
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
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.submitFromReviewPage()
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
