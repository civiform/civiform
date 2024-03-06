import {test, expect} from './fixtures/custom_fixture'
import {
  dropTables,
  isHermeticTestEnvironment,
  loginAsAdmin,
  logout,
  seedPrograms,
  validateScreenshot,
} from './support'
import {BASE_URL} from './support/config'

test.describe('Viewing API docs', {tag: ['@migrated']}, () => {
  test.beforeEach(async ({page}) => {
    await dropTables(page)
    await seedPrograms(page)
  })

  test('Views active API docs', async ( {page, adminPrograms, adminQuestions}) => {
    // TODO: fix the problem with these test on probers
    // https://github.com/civiform/civiform/issues/6158
    if (isHermeticTestEnvironment()) {
      await page.goto(BASE_URL)
      await loginAsAdmin(page)

      await adminPrograms.publishAllDrafts()

      // Add additional option to checkbox to ensure all historical options are shown
      await adminQuestions.gotoQuestionEditPage('Sample Checkbox Question')
      await adminQuestions.deleteMultiOptionAnswer(0)
      await adminQuestions.addMultiOptionAnswer({
        adminName: 'spirograph',
        text: 'spirograph',
      })
      await adminQuestions.clickSubmitButtonAndNavigate('Update')
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
    }
  })

  test('Views active API docs without logging in', async ( {page, adminPrograms, context}) => {
    await page.goto(BASE_URL)
    await loginAsAdmin(page)

    await adminPrograms.publishAllDrafts()
    const apiDocsUrl =
      BASE_URL + (await page.getByText('API docs').getAttribute('href'))!

    // Log out and clear cookies before accessing API docs.
    await logout(page)
    await context.clearCookies()
    const freshPage = await context.newPage()
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

  test('Views draft API docs when available', async ({page}) => {
    if (isHermeticTestEnvironment()) {
      await page.goto(BASE_URL)
      await loginAsAdmin(page)
      await page.click('text=API docs')

      await page.selectOption('#select-slug', {value: 'minimal-sample-program'})
      await page.selectOption('#select-version', {value: 'draft'})
      expect(await page.textContent('html')).toContain(
        '"program_name" : "minimal-sample-program"',
      )
      await validateScreenshot(page, 'draft-available')
    }
  })

  test('Shows error on draft API docs when no draft available', async ({page, adminPrograms}) => {
    if (isHermeticTestEnvironment()) {
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
    }
  })

  test('Opens help accordion with a click', async ({page, adminPrograms} ) => {
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
