import {startSession, gotoEndpoint, loginAsGuest, setLangEsUS, endSession, NotFoundPage} from './support'

describe('error pages', () => {
  it('test 404 page', async () => {
    const {browser, page} = await startSession()
    page.setDefaultTimeout(4000)

    const notFound = new NotFoundPage(page)

    await notFound.gotoNonExistentPage(page)
    await notFound.checkPageHeader()

    await gotoEndpoint(page, '/')
    await loginAsGuest(page)
    await setLangEsUS(page)
    await notFound.gotoNonExistentPage(page)
    await notFound.checkPageHeader('es-US')

    await endSession(browser)
  })
})
