import {
  createTestContext,
  gotoEndpoint,
  loginAsGuest,
  NotFoundPage,
  setLangEsUS,
} from './support'

describe('error pages', () => {
  const ctx = createTestContext()
  it('test 404 page', async () => {
    const {page} = ctx
    page.setDefaultTimeout(4000)

    const notFound = new NotFoundPage(page)

    await notFound.gotoNonExistentPage(page)
    await notFound.checkPageHeader()

    await gotoEndpoint(page, '/')
    await loginAsGuest(page)
    await setLangEsUS(page)
    await notFound.gotoNonExistentPage(page)
    await notFound.checkPageHeader('es-US')
  })
})
