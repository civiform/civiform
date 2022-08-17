import {Browser, Page} from 'playwright'
import {
  startSession,
  loginAsAdmin,
  loginAsTrustedIntermediary,
  endSession,
  AdminTIGroups,
  TIDashboard,
  ClientInformation,
  waitForPageJsLoad,
} from './support'

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

  it('expect Dashboard Contain New Client', async () => {
    await loginAsTrustedIntermediary(page)

    const tiDashboard = new TIDashboard(page)
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
  })

  it('managing trusted intermediary ', async () => {
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
  })
})
