import { Browser, Page } from 'playwright'
import {
  startSession,
  loginAsAdmin,
  endSession,
  AdminTIGroups,
} from './support'

describe('normal application flow', () => {
  let browser: Browser
  let page: Page

  beforeEach(async () => {
    var session = await startSession()

    browser = session.browser
    page = session.page
  })

  afterEach(async () => {
    await endSession(browser)
  })

  it('all major steps', async () => {
    await loginAsAdmin(page)
    const adminGroups = new AdminTIGroups(page)
    await adminGroups.gotoAdminTIPage()
    await adminGroups.fillInGroupBasics('group name', 'group description')
    await adminGroups.expectGroupExist('group name', 'group description')

    await adminGroups.editGroup('group name')
    await adminGroups.addGroupMember('foo@bar.com')
    await adminGroups.expectGroupMemberExist('<Unnamed User>', 'foo@bar.com')
  })
})
