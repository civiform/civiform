import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  loginAsTrustedIntermediary,
  ClientInformation,
  loginAsTestUser,
  validateScreenshot,
  validateAccessibility,
} from '../support'
import {Eligibility} from '../support/admin_programs'

test.describe('Applicant program overview', {tag: ['@northstar']}, () => {
  const programName = 'test'
  const questionText = 'This is a text question'

  test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await test.step('create a new program with one text question', async () => {
      await loginAsAdmin(page)
      await adminQuestions.addTextQuestion({
        questionName: 'text question',
        questionText: questionText,
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        description: 'First block',
        questions: [{name: 'text question', isOptional: false}],
      })
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })
  })

  test('can view program overview', async ({
    page,
    applicantProgramOverview: applicantProgramOverview,
    adminPrograms,
  }) => {
    await test.step('edit the long description and application step description so that they have markdown', async () => {
      await loginAsAdmin(page)
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        /* createNewDraft= */ true,
      )
      await page
        .getByRole('textbox', {name: 'Long program description (optional)'})
        .fill(
          'This is the _program long description_ with markdown\n' +
            '[This is a link](https://www.example.com)\n' +
            'This is a list:\n' +
            '* Item 1\n' +
            '* Item 2\n' +
            '\n' +
            'There are some empty lines below this that should be preserved\n' +
            '\n' +
            '\n' +
            'Autodetected link: https://www.example.com\n',
        )

      await page
        .getByRole('textbox', {name: 'Step 1 description'})
        .fill(
          'This is the _application step_ with markdown\n' +
            'Autodetected link: https://www.example.com\n' +
            'This is a list:\n' +
            '* Item 1\n' +
            '* Item 2\n',
        )
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await page.goto(`/programs/${programName}`)

    await applicantProgramOverview.expectProgramOverviewPage(programName)
    await validateScreenshot(
      page.locator('main'),
      'program-overview',
      /* fullPage= */ false,
      /* mobileScreenshot= */ true,
    )
    await validateAccessibility(page)
  })

  test.describe('after starting an application', () => {
    test.beforeEach(async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName, true)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
    })
    test('takes guests and logged in users to the program overview', async ({
      page,
      applicantQuestions,
      applicantProgramOverview: applicantProgramOverview,
    }) => {
      // Exercise guest path
      await page.goto(`/programs/${programName}`)
      await applicantProgramOverview.expectProgramOverviewPage(programName)

      await logout(page)

      // Exercise test user path
      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(programName, true)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()

      await page.goto(`/programs/${programName}`)
      await applicantProgramOverview.expectProgramOverviewPage(programName)
    })
  })

  test('displays short description if there is no long description', async ({
    page,
    adminPrograms,
  }) => {
    await page.goto(`/programs/${programName}`)

    await test.step('expect the long description to initially be displayed', async () => {
      await expect(page.getByText('program description')).toBeVisible()
      await expect(page.getByText('short program description')).toBeHidden()
    })

    await test.step('log in as an admin and remove the long description', async () => {
      await loginAsAdmin(page)
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        /* createNewDraft= */ true,
      )
      await page
        .getByRole('textbox', {name: 'Long program description (optional)'})
        .fill('')
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
    })

    await test.step('expect the short description to be displayed', async () => {
      await page.goto(`/programs/${programName}`)
      await expect(page.getByText('short program description')).toBeVisible()
    })
  })

  test('shows the application steps', async ({page}) => {
    await page.goto(`/programs/${programName}`)
    await expect(
      page.getByRole('listitem').getByRole('heading', {
        name: 'title',
      }) /* 'title' is the heading on the first application step */,
    ).toBeVisible()

    await expect(
      page
        .getByRole('listitem')
        .getByText(
          'description',
        ) /* 'description' is the text on the first application step */,
    ).toBeVisible()

    await expect(page.getByRole('list').filter({hasText: 'title'})).toHaveCount(
      1,
    )
  })

  test('shows the correct eligibility alert under the right conditions', async ({
    page,
    adminPrograms,
    adminPredicates,
    applicantQuestions,
    applicantProgramOverview: applicantProgramOverview,
  }) => {
    const secondProgram = 'Second Program'

    await test.step('verify that the alert does not show when no eligibililty conditions exist on program', async () => {
      await page.goto(`/programs/${programName}`)
      await applicantProgramOverview.expectNoEligibilityAlerts()
    })

    await test.step('create a second program with the same text question and an eligibility condition', async () => {
      await loginAsAdmin(page)

      await adminPrograms.addProgram(secondProgram)
      await adminPrograms.editProgramBlockUsingSpec(secondProgram, {
        description: 'First block',
        questions: [{name: 'text question', isOptional: false}],
      })
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        secondProgram,
        'Screen 1',
      )
      await adminPredicates.addPredicates({
        questionName: 'text question',
        scalar: 'text',
        operator: 'is equal to',
        value: 'eligible',
      })
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(secondProgram)
      await logout(page)
    })

    await test.step('verify that no alert shows when the eligibility question has not been answered in another application', async () => {
      await page.goto(`/programs/second-program`)
      await applicantProgramOverview.expectNoEligibilityAlerts()
      await logout(page)
    })

    await test.step('apply to first program in an eligible way', async () => {
      await applicantQuestions.applyProgram(programName, true)
      await applicantQuestions.answerTextQuestion('eligible')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(true)
    })

    // Eligibility is gating by default
    await test.step('verify that the alert shows as eligible when the eligibility condition is met and eligibilty is gating', async () => {
      await page.goto(`/programs/second-program`)
      await applicantProgramOverview.expectYouAreEligibleAlert()
      await logout(page)
    })

    await test.step('apply to first program in an ineligible way', async () => {
      await applicantQuestions.applyProgram(programName, true)
      await applicantQuestions.answerTextQuestion('not eligible')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(true)
    })

    await test.step('verify that the alert shows as ineligible when the eligibility condition is not met and eligibilty is gating', async () => {
      await page.goto(`/programs/second-program`)
      await applicantProgramOverview.expectYouMayNotBeEligibleAlert()
    })

    // Eligibility is not gating
    await test.step('log in as an admin and make eligibiliy not gating on the second program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.goToProgramDescriptionPage(
        secondProgram,
        /* createNewDraft= */ true,
      )
      await adminPrograms.chooseEligibility(Eligibility.IS_NOT_GATING)
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('apply to first program in an ineligible way', async () => {
      await applicantQuestions.applyProgram(programName, true)
      await applicantQuestions.answerTextQuestion('not eligible')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(true)
    })

    await test.step('verify that no alert shows when eligibilty is not gating and the eligibility condition is not met', async () => {
      await page.goto(`/programs/second-program`)
      await applicantProgramOverview.expectNoEligibilityAlerts()
      await logout(page)
    })

    await test.step('apply to first program in an eligible way', async () => {
      await applicantQuestions.applyProgram(programName, true)
      await applicantQuestions.answerTextQuestion('eligible')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(true)
    })

    await test.step('verify that the alert shows as eligible when the eligibility condition is met and eligibilty is not gating', async () => {
      await page.goto(`/programs/second-program`)
      await applicantProgramOverview.expectYouAreEligibleAlert()
    })
  })

  test('redirects to disabled program info page when program is disabled', async ({
    page,
    adminPrograms,
  }) => {
    const disabledProgramName = 'dis'

    await test.step('create a new disabled program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addDisabledProgram(disabledProgramName)
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step(`opens the deep link of the disabled program and gets redirected to an error info page`, async () => {
      await page.goto(`/programs/${disabledProgramName}`)
      expect(page.url()).toContain('/disabled')
      await expect(
        page.getByRole('heading', {
          name: 'This program is no longer available',
        }),
      ).toBeVisible()
    })

    await test.step(`clicks on visit homepage button and it takes me to home page`, async () => {
      await page.click('#visit-home-page-button')
      expect(page.url()).toContain('/programs')
      await expect(
        page.getByRole('heading', {
          name: 'Apply for government assistance here',
        }),
      ).toBeVisible()
    })
  })

  test('trusted intermediary can view program overview with applicant id in the URL', async ({
    page,
    tiDashboard,
    applicantProgramOverview: applicantProgramOverview,
  }) => {
    await loginAsTrustedIntermediary(page)
    const client: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardContainClient(client)
    await tiDashboard.clickOnViewApplications()

    const url = page.url()
    expect(url).toContain('/applicants/')

    await page.goto(`${url}/${programName}`)

    await applicantProgramOverview.expectProgramOverviewPage(programName)
  })

  test('Going to a deep link does not retain redirect in session', async ({
    page,
  }) => {
    // Go to a deep link
    await page.goto(`/programs/${programName}`)

    // Logging out should not take us back to the program overview, but rather
    // to the program index page.
    await logout(page)

    await expect(
      page.getByRole('heading', {
        name: 'Apply for government assistance here',
      }),
    ).toBeAttached()
  })

  test('clicking on "Start an application" goes to the first page of the application', async ({
    page,
    applicantProgramOverview: applicantProgramOverview,
  }) => {
    await page.goto(`/programs/${programName}`)
    await page.getByRole('link', {name: 'Start an application'}).click()
    await applicantProgramOverview.expectFirstPageOfApplication()
  })

  test('when user is logged in', async ({
    page,
    applicantProgramOverview: applicantProgramOverview,
  }) => {
    await loginAsTestUser(page)
    await page.goto(`/programs/${programName}`)

    await test.step('verify "logged in as" and logout button are visible', async () => {
      await expect(page.getByText('Logged in as')).toBeVisible()
      await expect(page.getByRole('button', {name: 'Logout'})).toBeVisible()
    })

    const stepsAndAlertsLocator = page.getByTestId('steps-and-alerts')

    await test.step('verify there is no create account alert but, rather, a "Start an application" button', async () => {
      await expect(page.locator('#create-account')).toBeHidden()
      await expect(
        stepsAndAlertsLocator.getByRole('link', {name: 'Start an application'}),
      ).toBeVisible()
    })

    await test.step('click on 2nd "Start an application" button and verify that it goes to the application', async () => {
      await stepsAndAlertsLocator
        .getByRole('link', {name: 'Start an application'})
        .click()
      await applicantProgramOverview.expectFirstPageOfApplication()
    })
  })

  test('when user is a guest', async ({
    page,
    applicantProgramOverview: applicantProgramOverview,
  }) => {
    await page.goto(`/programs/${programName}`)

    await expect(page.getByRole('button', {name: 'Log in'})).toBeVisible()

    await test.step('verify the create account alert is visible', async () => {
      await expect(
        page.getByRole('button', {name: 'Start application with an account'}),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {
          name: 'To access your application later, create an account',
        }),
      ).toBeVisible()
    })

    await test.step('click "Start application as a guest" and verify that it goes to the application', async () => {
      await page
        .getByRole('button', {name: 'Start application as a guest'})
        .click()
      await applicantProgramOverview.expectFirstPageOfApplication()
    })
  })
})
