import {
  createTestContext,
  loginAsAdmin,
  seedPrograms,
  validateScreenshot,
} from './support'
import {BASE_URL} from './support/config'

describe('Viewing API docs', () => {
  const ctx = createTestContext()

  it('Views API docs', async () => {
    const {page, adminPrograms} = ctx
    await seedPrograms(page)

    await page.goto(BASE_URL)
    await loginAsAdmin(page)

    await adminPrograms.publishAllDrafts()

    await page.click('text=API docs')
    await validateScreenshot(page, 'api-docs-page-comprehensive-program')

    await page.selectOption('#select-slug', {value: 'minimal-sample-program'})
    await validateScreenshot(page, 'api-docs-page-minimal-program')

    await page.click('text=How does this work?')
    await validateScreenshot(page, 'api-docs-page-accordion-open')
  })
})
