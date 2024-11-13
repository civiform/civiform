import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  ClientInformation,
  loginAsTrustedIntermediary,
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
  },
)
