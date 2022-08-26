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

  it('expect Client Date Of Birth to be Updated', async () => {
    await loginAsTrustedIntermediary(page)
    const tiDashboard = new TIDashboard(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardContainClient(client)
    await tiDashboard.updateClientDateOfBirth(client, '2021-12-12')
    await tiDashboard.expectClientDateOfBirthUpdated(client, '2021-12-12')
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

  it('search For Client In TI Dashboard', async () => {
    await loginAsTrustedIntermediary(page)

    const tiDashboard = new TIDashboard(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)
    const client1: ClientInformation = {
      emailAddress: 'fake@sample.com',
      firstName: 'first1',
      middleName: 'middle',
      lastName: 'last1',
      dobDate: '2021-07-10',
    }
    await tiDashboard.createClient(client1)
    const client2: ClientInformation = {
      emailAddress: 'fake2@sample.com',
      firstName: 'first2',
      middleName: 'middle',
      lastName: 'last2',
      dobDate: '2021-11-10',
    }
    await tiDashboard.createClient(client2)
    const client3: ClientInformation = {
      emailAddress: 'fake3@sample.com',
      firstName: 'first3',
      middleName: 'middle',
      lastName: 'last3',
      dobDate: '2021-12-10',
    }
    await tiDashboard.createClient(client3)

    await tiDashboard.searchByDateOfBirth(client3.dobDate)
    await waitForPageJsLoad(page)
    await tiDashboard.expectDashboardContainClient(client3)
    await tiDashboard.expectDashboardNotContainClient(client1)
    await tiDashboard.expectDashboardNotContainClient(client2)
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
