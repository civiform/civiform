import {
  createTestContext,
  dropTables,
  loginAsAdmin,
  seedPrograms,
  validateScreenshot,
} from './support'
import {BASE_URL} from './support/config'

describe('Viewing API docs', () => {
  const ctx = createTestContext()

  beforeEach(async () => {
    const {page} = ctx
    await dropTables(page)
    await seedPrograms(page)
  })

  it('Views active API docs', async () => {
    const {page, adminPrograms} = ctx

    await page.goto(BASE_URL)
    await loginAsAdmin(page)

    await adminPrograms.publishAllDrafts()
    await page.click('text=API docs')

    expect(await page.textContent('html')).toContain(
      '"program_name" : "comprehensive-sample-program"',
    )
    await validateScreenshot(page, 'comprehensive-program-active-version')

    await page.selectOption('#select-slug', {value: 'minimal-sample-program'})
    expect(await page.textContent('html')).toContain(
      '"program_name" : "minimal-sample-program"',
    )
    await validateScreenshot(page, 'minimal-program-active-version')
  })

  it('Views draft API docs when available', async () => {
    const {page} = ctx

    await page.goto(BASE_URL)
    await loginAsAdmin(page)
    await page.click('text=API docs')

    await page.selectOption('#select-slug', {value: 'minimal-sample-program'})
    await page.selectOption('#select-version', {value: 'draft'})
    expect(await page.textContent('html')).toContain(
      '"program_name" : "minimal-sample-program"',
    )
    await validateScreenshot(page, 'draft-available')
  })

  it('Shows error on draft API docs when no draft available', async () => {
    const {page, adminPrograms} = ctx

    await page.goto(BASE_URL)
    await loginAsAdmin(page)
    await adminPrograms.publishAllDrafts()
    await page.click('text=API docs')

    await page.selectOption('#select-slug', {value: 'minimal-sample-program'})
    await page.selectOption('#select-version', {value: 'draft'})
    expect(await page.textContent('html')).toContain(
      'Program and version not found',
    )
    await validateScreenshot(page, 'draft-not-available')
  })

  it('Opens help accordion', async () => {
    const {page, adminPrograms} = ctx

    await page.goto(BASE_URL)
    await loginAsAdmin(page)
    await adminPrograms.publishAllDrafts()

    await page.click('text=API docs')

    // Select minimal sample program so the screenshot will be smaller.
    await page.selectOption('#select-slug', {value: 'minimal-sample-program'})

    await page.click('text=How does this work?')
    // Wait for the accordion to open, so we don't screenshot during the opening,
    // causing inconsistent screenshots.
    await page.waitForTimeout(300) // ms

    // Wait for the specific text to become visible in the accordion.
    const textToFind =
      'The API Response Preview is a sample of what the API response might look like'
    await page.waitForFunction((text) => {
      const element = document.querySelector('.cf-accordion-visible')
      return (
        element && element.textContent && element.textContent.includes(text)
      )
    }, textToFind)

    await validateScreenshot(page, 'api-docs-page-accordion-open')
  })
})
