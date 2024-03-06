import {test} from './fixtures/custom_fixture'
import {
  gotoEndpoint,
  selectApplicantLanguage,
} from './support'

test.describe('error pages', {tag: ['@migrated']}, () => {
  test('404 page', async ({page, notFoundPage}) => {
    await notFoundPage.gotoNonExistentPage(page)
    await notFoundPage.checkPageHeader()

    await gotoEndpoint(page, '/')
    await selectApplicantLanguage(page, 'Español')
    await notFoundPage.gotoNonExistentPage(page)
    await notFoundPage.checkPageHeader('es-US')
  })
})
