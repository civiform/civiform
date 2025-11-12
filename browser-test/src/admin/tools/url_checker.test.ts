import {test, expect} from '../../support/civiform_fixtures'
import {isLocalDevEnvironment, loginAsAdmin} from '../../support'
import {UrlCheckerPage} from '../../page/admin/tools/url_checker_page'

test.describe('admin url checker page', () => {
  test('checks', async ({page}) => {
    // Skip on cloud. Trying to find url options that
    // work locally and in the different clouds has proved
    // difficult.
    test.skip(!isLocalDevEnvironment(), 'Skip on cloud')

    const urlCheckerPage = new UrlCheckerPage(page)

    await loginAsAdmin(page)
    await urlCheckerPage.goto()
    await expect(urlCheckerPage.getPageHeading()).toBeVisible()

    await test.step('has no url', async () => {
      await urlCheckerPage.fillUrl('')
      await urlCheckerPage.clickCheckButton()
      await expect(urlCheckerPage.getOutput()).toContainText('')
    })

    await test.step('has invalid url', async () => {
      await urlCheckerPage.fillUrl('http://')
      await urlCheckerPage.clickCheckButton()
      await expect(urlCheckerPage.getOutput()).toContainText('Invalid url')
    })

    await test.step('has unreachable url', async () => {
      await urlCheckerPage.fillUrl('http://localhost')
      await urlCheckerPage.clickCheckButton()
      await expect(urlCheckerPage.getOutput()).toContainText('Error')
    })

    await test.step('has reachable url', async () => {
      await urlCheckerPage.fillUrl('http://127.0.0.1:9000')
      await urlCheckerPage.clickCheckButton()
      await expect(urlCheckerPage.getOutput()).toContainText('OK')
    })
  })
})
