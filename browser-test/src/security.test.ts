import {test, expect} from './support/civiform_fixtures'
import {loginAsAdmin} from './support'

test.describe(
  'applicant security',
  {tag: ['@uses-fixtures', '@parallel-candidate']},
  () => {
    test('applicant cannot access admin pages', async ({request}) => {
      const response = await request.get('/admin/programs')
      await expect(response).toBeOK()
      // Redirected to a non-admin page
      expect(response.url()).not.toContain('/admin')
    })

    test('redirects to program index page when not logged in (guest)', async ({
      page,
    }) => {
      await page.goto('/')
      await expect(
        page.getByRole('heading', {
          name: 'Save time applying for programs and services',
        }),
      ).toBeAttached()
    })

    test('redirects to program dashboard when logged in as admin', async ({
      page,
    }) => {
      await loginAsAdmin(page)
      await page.goto('/')

      await expect(
        page.getByRole('heading', {name: 'Program dashboard'}),
      ).toBeAttached()
      await expect(
        page.getByRole('heading', {name: 'Create, edit and publish programs'}),
      ).toBeAttached()
    })
  },
)
