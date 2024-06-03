import {expect, test} from '../support/civiform_fixtures'
// import {Page} from '@playwright/test'
import {
  ClientInformation,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateAccessibility,
  loginAsTrustedIntermediary,
  waitForPageJsLoad,
} from '../support'
import {ProgramVisibility} from '../support/admin_programs'

test.describe(
  'North Star Common Intake Upsell Tests',
  {tag: ['@northstar']},
  () => {
    const programName = 'Common Intake Program'
    const eligibleProgram1 = 'Eligible Program 1'

    test.beforeEach(async ({page, adminPrograms}) => {
      await enableFeatureFlag(page, 'intake_form_enabled')

      await loginAsAdmin(page)

      await test.step('Setup: Publish common intake program', async () => {
        await adminPrograms.addProgram(
          programName,
          'Display description',
          'https://usa.gov',
          ProgramVisibility.PUBLIC,
          'admin description',
          /* isCommonIntake= */ true,
        )
        await adminPrograms.publishProgram(programName)
        await adminPrograms.expectActiveProgram(programName)
      })
    })

    test('view application submitted page with one eligible program', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('Setup: publish one program', async () => {
        await adminPrograms.addProgram(eligibleProgram1)
        await adminPrograms.publishProgram(eligibleProgram1)
        await logout(page)
      })

      await loginAsTestUser(page)

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      expect(await page.textContent('html')).toContain(
        'Programs you may qualify for',
      )
      expect(await page.textContent('html')).toContain(eligibleProgram1)

      await validateScreenshot(
        page,
        'upsell-north-star-common-intake',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )

      await validateAccessibility(page)
    })

    test('view application submitted page with zero eligible programs', async ({
      page,
      applicantQuestions,
    }) => {
      await logout(page)

      await loginAsTestUser(page)

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      expect(await page.textContent('html')).toContain(
        'The pre-screener could not find programs you may qualify for at this time',
      )
    })

    test('As TI, view application submitted page with one eligible program', async ({
      page,
      tiDashboard,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('Setup: publish one program', async () => {
        await adminPrograms.addProgram(eligibleProgram1)
        await adminPrograms.publishProgram(eligibleProgram1)
        await logout(page)
      })

      await test.step('Create client', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.gotoTIDashboardPage(page)
        await waitForPageJsLoad(page)
        const client: ClientInformation = {
          emailAddress: 'test@sample.com',
          firstName: 'first',
          middleName: 'middle',
          lastName: 'last',
          dobDate: '2021-06-10',
        }
        await tiDashboard.createClient(client, true)
        await tiDashboard.expectDashboardContainClient(client)
      })

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await tiDashboard.clickOnViewApplications()
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      expect(await page.textContent('html')).toContain(
        'Programs your client may qualify for',
      )
      expect(await page.textContent('html')).toContain(eligibleProgram1)
    })

    test('As TI, view application submitted page with 0 eligible programs', async ({
      page,
      tiDashboard,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('Setup: publish one program', async () => {
        await logout(page)
      })

      await test.step('Create client', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.gotoTIDashboardPage(page)
        await waitForPageJsLoad(page)
        const client: ClientInformation = {
          emailAddress: 'test@sample.com',
          firstName: 'first',
          middleName: 'middle',
          lastName: 'last',
          dobDate: '2021-06-10',
        }
        await tiDashboard.createClient(client, true)
        await tiDashboard.expectDashboardContainClient(client)
      })

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await tiDashboard.clickOnViewApplications()
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      expect(await page.textContent('html')).toContain(
        'The pre-screener could not find programs your client may qualify for at this time',
      )
    })
  },
)
