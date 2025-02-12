import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  ClientInformation,
  loginAsAdmin,
  loginAsTrustedIntermediary,
  logout,
  seedProgramsAndCategories,
  waitForPageJsLoad,
} from './support'

test.describe(
  'Trusted intermediaries with North Star specific changes',
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('sees client name and link in sub-banner while applying on behalf of applicant', async ({
      page,
      tiDashboard,
    }) => {
      await test.step('Navigate to TI dashboard', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.gotoTIDashboardPage(page)
        await waitForPageJsLoad(page)
        const client: ClientInformation = {
          emailAddress: 'fake12@sample.com',
          firstName: 'first1',
          middleName: 'middle',
          lastName: 'last1',
          dobDate: '2021-07-10',
        }
        await tiDashboard.createClient(client)
        await tiDashboard.clickOnViewApplications()
      })

      await test.step('Verify header text and behavior', async () => {
        await expect(page.getByText('Select a new client')).toBeVisible()
        await expect(
          page.getByText(
            'You are applying for last1, first1. Are you trying to apply for a different client?',
          ),
        ).toBeVisible()

        await page.getByRole('link', {name: 'Select a new client'}).click()
        // Expect to return to TI dashboard
        await expect(page.getByText('View and add clients')).toBeVisible()
      })
    })

    test('with program filters enabled, categorizes programs correctly for Trusted Intermediaries', async ({
      page,
      adminPrograms,
      adminQuestions,
      tiDashboard,
      applicantQuestions,
    }) => {
      const primaryProgramName = 'Application index primary program'
      const otherProgramName = 'Application index other program'

      const firstQuestionText = 'This is the first question'
      const secondQuestionText = 'This is the second question'

      await test.step('Setup test programs', async () => {
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
        await adminPrograms.addProgramBlock(primaryProgramName, 'first block', [
          'first-q',
        ])
        // The 'second block' is actually screen 3
        await adminPrograms.addProgramBlock(
          primaryProgramName,
          'second block',
          ['second-q'],
        )

        await adminPrograms.addProgram(otherProgramName)
        await adminPrograms.addProgramBlock(otherProgramName, 'first block', [
          'first-q',
        ])

        await adminPrograms.publishAllDrafts()
        await logout(page)
      })
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

      await test.step('publish programs with categories', async () => {
        await adminPrograms.publishAllDrafts()
      })
      await test.step('Navigate to homepage', async () => {
        await logout(page)
        await loginAsTrustedIntermediary(page)
      })
      await test.step('Create a new client', async () => {
        await tiDashboard.gotoTIDashboardPage(page)
        await waitForPageJsLoad(page)
        const client: ClientInformation = {
          emailAddress: 'fake@sample.com',
          firstName: 'first',
          middleName: 'middle',
          lastName: 'last',
          dobDate: '2021-05-10',
        }
        await tiDashboard.createClient(client)
        await tiDashboard.expectDashboardContainClient(client)
        await tiDashboard.clickOnViewApplications()
      })
      await test.step('Apply to a program and verify that applied program is under my applicatins section of view application page', async () => {
        await applicantQuestions.applyProgram(primaryProgramName, true)
        await applicantQuestions.answerTextQuestion('first answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.gotoApplicantHomePage()
        await tiDashboard.clickOnViewApplications()
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

      await test.step('Select a filter, click the filter submit button and verify the Recommended and Other programs sections with in-progress application', async () => {
        await applicantQuestions.filterProgramsAndExpectWithFilteringEnabled(
          {
            filterCategory: 'General',
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
      })

      await test.step('Finish the application and confirm that the program appears in the "My applications" section', async () => {
        await applicantQuestions.applyProgram(
          primaryProgramName,
          /* northStarEnabled= */ true,
          /* isApplicationUnstarted= */ false,
        )
        await applicantQuestions.answerTextQuestion('second answer')
        await applicantQuestions.clickContinue()
        await applicantQuestions.submitFromReviewPage(true)
        await applicantQuestions.expectConfirmationPage(true)
        await applicantQuestions.clickBackToHomepageButton()
        await tiDashboard.clickOnViewApplications()
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
      await test.step('Select a filter, click the filter submit button and verify the Recommended and Other programs sections with finished application', async () => {
        await applicantQuestions.filterProgramsAndExpectWithFilteringEnabled(
          {
            filterCategory: 'General',
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
      })
    })
  },
)
