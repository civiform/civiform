import { Browser, Page } from 'playwright'
import { AdminTIGroups, endSession, loginAsAdmin, loginAsTrustedIntermediary, startSession, validateScreenshot, } from './support'

describe('Trusted intermediaries', () => {
  let browser: Browser
  let page: Page

  beforeEach(async () => {
    const session = await startSession()

    browser = session.browser
    page = session.page
  })

  afterEach(async () => {
    await endSession(browser)
  })

  it('managing trusted intermediary groups', async () => {
    await loginAsAdmin(page)
    const adminGroups = new AdminTIGroups(page)
    await adminGroups.gotoAdminTIPage()
    await adminGroups.fillInGroupBasics('group name', 'group description')
    await adminGroups.expectGroupExist('group name', 'group description')

    await adminGroups.editGroup('group name')
    await adminGroups.addGroupMember('foo@bar.com')
    await adminGroups.expectGroupMemberExist('<Unnamed User>', 'foo@bar.com')
  })

  it('logging in as a trusted intermediary', async () => {
    await loginAsTrustedIntermediary(page)
    expect(await page.innerText('#ti-dashboard-link')).toContain(
      'TRUSTED INTERMEDIARY DASHBOARD',
    )
    await validateScreenshot(page);
  })
})
