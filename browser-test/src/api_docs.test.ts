import {
  createTestContext,
  dropTables,
  loginAsAdmin,
  logout,
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

  it('Views active API docs without logging in', async () => {
    const {page, adminPrograms, browserContext} = ctx

    await page.goto(BASE_URL)
    await loginAsAdmin(page)

    await adminPrograms.publishAllDrafts()
    const apiDocsUrl =
      BASE_URL + (await page.getByText('API docs').getAttribute('href'))!

    // Log out and clear cookies before accessing API docs.
    await logout(page)
    await browserContext.clearCookies()
    const freshPage = await browserContext.newPage()
    await freshPage.goto(apiDocsUrl)

    expect(await freshPage.textContent('html')).toContain(
      '"program_name" : "comprehensive-sample-program"',
    )
    await validateScreenshot(
      freshPage,
      'comprehensive-program-active-version-logged-out',
    )

    await freshPage.selectOption('#select-slug', {
      value: 'minimal-sample-program',
    })
    expect(await freshPage.textContent('html')).toContain(
      '"program_name" : "minimal-sample-program"',
    )
    await validateScreenshot(
      freshPage,
      'minimal-program-active-version-logged-out',
    )
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

  it('Opens help accordion with a click', async () => {
    const {page, adminPrograms} = ctx

    await page.goto(BASE_URL)
    await loginAsAdmin(page)
    await adminPrograms.publishAllDrafts()

    await page.click('text=API docs')

    // Select minimal sample program so the screenshot will be smaller.
    await page.selectOption('#select-slug', {value: 'minimal-sample-program'})

    // Opening the accordion
    await page.click('text=How does this work?')

    await validateScreenshot(page, 'api-docs-page-accordion-open')
  })
})
