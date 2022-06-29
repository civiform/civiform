import {
  startSession,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsGuest,
  loginAsTestUser,
  logout,
  endSession,
  NotFoundPage,
} from './support'

describe('error pages', () => {
  it('test 404 page', async () => {
    const {browser, page} = await startSession()
    page.setDefaultTimeout(4000)

    const notFound = new NotFoundPage(page)

    await notFound.gotoNonExistentPage(page)

    await notFound.checkPageHeaderEnUS()

    await notFound.checkNotLoggedIn()

    await notFound.loginAsGuest()

    await notFound.gotoNonExistentPage(page)

    await page.pause()
    await notFound.checkIsGuest()

    await endSession(browser)
  })
})
