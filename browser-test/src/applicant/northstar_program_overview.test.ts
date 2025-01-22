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
        questions: [{name: 'text question', isOptional: true}],
      })
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })
  })

  test('can view program overview', async ({
    page,
    applicantProgramOverview: applicantProgramOverview,
  }) => {
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

  test('redirects to disabled program info page when program is disabled', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'disabled_visibility_condition_enabled')
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
          name: 'Apply to programs in one place',
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
        name: 'Apply to programs in one place',
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
      await expect(page.getByRole('link', {name: 'Logout'})).toBeVisible()
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

    await expect(page.getByRole('link', {name: 'Log in'})).toBeVisible()

    await test.step('verify the create account alert is visible', async () => {
      await expect(
        page.getByRole('link', {name: 'Start application with an account'}),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {
          name: 'Create an account to save your application information',
        }),
      ).toBeVisible()
    })

    await test.step('click "Start application as a guest" and verify that it goes to the application', async () => {
      await page
        .getByRole('link', {name: 'Start application as a guest'})
        .click()
      await applicantProgramOverview.expectFirstPageOfApplication()
    })
  })
})
