import {
  startSession,
  loginAsGuest,
  endSession,
  gotoEndpoint,
  NotFoundPage,
} from './support'

describe('error pages', () => {
  it('test 404 page', async () => {
    const {browser, page} = await startSession()
    page.setDefaultTimeout(4000)

    const notFound = new NotFoundPage(page)

    await notFound.gotoNonExistentPage(page)
    await notFound.checkPageHeader()
    await notFound.checkNotLoggedIn()

    await notFound.loginAsGuest()
    await notFound.gotoNonExistentPage(page)
    await notFound.checkIsGuest()
    await notFound.logout()

    await notFound.gotoNonExistentPage(page)
    await notFound.loginAsGuest('es-US')
    await notFound.gotoNonExistentPage(page)
    await notFound.checkIsGuest('es-US')
    await notFound.logout('es-US')

    await endSession(browser)
  })
})
