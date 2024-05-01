import {test, expect} from '@playwright/test' // Does not need to use civiform_fixture
import {selectApplicantLanguage} from './support'

test.describe(
  'Error pages',
  {tag: ['@uses-fixtures', '@parallel-candidate']},
  () => {
    test('404 page', async ({page}) => {
      await test.step('Has heading in English', async () => {
        await page.goto('/bad/path/ezbezzdebashiboozook')
        await expect(
          page.getByRole('heading', {
            name: 'We were unable to find the page you tried to visit',
          }),
        ).toBeAttached()
      })

      await test.step('Change applicant language to Spanish', async () => {
        await page.goto('/')
        await selectApplicantLanguage(page, 'Español')
      })

      await test.step('Has heading in Spanish', async () => {
        await page.goto('/bad/path/ezbezzdebashiboozook')
        await expect(
          page.getByRole('heading', {
            name: 'No Pudimos encontrar la página que intentó visitar',
          }),
        ).toBeAttached()
      })
    })
  },
)
