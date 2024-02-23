import {
  createTestContext,
  gotoEndpoint,
  NotFoundPage,
  selectApplicantLanguage,
} from './support'

describe('error pages', () => {
  const ctx = createTestContext()
  test('test 404 page', async () => {
    const {page} = ctx

    const notFound = new NotFoundPage(ctx)

    await notFound.gotoNonExistentPage(page)
    await notFound.checkPageHeader()

    await gotoEndpoint(page, '/')
    await selectApplicantLanguage(page, 'Español')
    await notFound.gotoNonExistentPage(page)
    await notFound.checkPageHeader('es-US')
  })
})
