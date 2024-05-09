import {test, expect} from './support/civiform_fixtures'
import {
  loginAsTrustedIntermediary,
  waitForPageJsLoad,
  validateScreenshot,
} from './support'

test.describe('Pagination', () => {
  test('shows 1 page and no previous or next buttons when there are 10 clients', async ({
    page,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    await tiDashboard.createMultipleClients('myname', 10)
    const cardCount = await page.locator('.usa-card__container').count()
    expect(cardCount).toBe(10)

    // No 'Previous' button
    expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
      'usa-pagination__previous-page',
    )

    // No 'Next' button
    expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
      'usa-pagination__next-page',
    )

    // There should be a page 1 button
    await tiDashboard.expectPageNumberButton('1')

    // There should be no page 2 button
    await tiDashboard.expectPageNumberButtonNotPresent('2')

    // The page 1 button should be the current page
    expect(await page.innerHTML('.usa-current')).toContain('1')

    // There should be no ellipses
    expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
      'usa-pagination__overflow',
    )
  })

  test('shows 2 pages when there are 11 clients', async ({
    page,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    await tiDashboard.createMultipleClients('myname', 11)

    // Page 1 should still only show 10 clients
    const cardCount = await page.locator('.usa-card__container').count()
    expect(cardCount).toBe(10)

    // No 'Previous' button because we're on the 1st page
    expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
      'usa-pagination__previous-page',
    )

    // There should be a 'Next' button
    expect(await page.innerHTML('.usa-pagination__list')).toContain(
      'usa-pagination__next-page',
    )

    await tiDashboard.expectPageNumberButton('1')
    await tiDashboard.expectPageNumberButton('2')

    expect(await page.innerHTML('.usa-current')).toContain('1')

    // There should be no ellipses
    expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
      'usa-pagination__overflow',
    )

    // Going to page 2
    await page.click('[aria-label=Page2]')

    const page2CardCount = await page.locator('.usa-card__container').count()
    expect(page2CardCount).toBe(1)

    // Now there should be a 'Previous' button
    expect(await page.innerHTML('.usa-pagination__list')).toContain(
      'usa-pagination__previous-page',
    )

    expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
      'usa-pagination__next-page',
    )

    await tiDashboard.expectPageNumberButton('1')
    await tiDashboard.expectPageNumberButton('2')

    expect(await page.innerHTML('.usa-current')).toContain('2')
  })

  test('shows 7 pages and no ellipses when there are 65 clients', async ({
    page,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    await tiDashboard.createMultipleClients('myname', 65)

    await tiDashboard.expectPageNumberButton('1')
    await tiDashboard.expectPageNumberButton('2')
    await tiDashboard.expectPageNumberButton('3')
    await tiDashboard.expectPageNumberButton('4')
    await tiDashboard.expectPageNumberButton('5')
    await tiDashboard.expectPageNumberButton('6')
    await tiDashboard.expectPageNumberButton('7')
    await tiDashboard.expectPageNumberButtonNotPresent('8')

    // Going to page 7
    await page.click('[aria-label=Page7]')
    expect(await page.innerHTML('.usa-current')).toContain('7')

    // There should be no ellipses
    expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
      'usa-pagination__overflow',
    )

    expect(await page.innerHTML('.usa-pagination__list')).not.toContain(
      'usa-pagination__next-page',
    )

    await validateScreenshot(
      page.locator('.usa-pagination'),
      'ti-pagination-no-ellipses',
    )
  })

  test('shows one ellipses on the right when more than 7 pages and current page is < 5', async ({
    page,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    await tiDashboard.createMultipleClients('myname', 75)

    await tiDashboard.expectPageNumberButton('1')
    await tiDashboard.expectPageNumberButton('2')
    await tiDashboard.expectPageNumberButton('3')
    await tiDashboard.expectPageNumberButton('4')
    await tiDashboard.expectPageNumberButton('5')
    // The ellipses takes the place of 6 and 7 when current page is < 5
    await tiDashboard.expectPageNumberButtonNotPresent('6')
    await tiDashboard.expectPageNumberButtonNotPresent('7')
    await tiDashboard.expectPageNumberButton('8')

    // There should be an ellipses
    expect(await page.innerHTML('.usa-pagination__list')).toContain(
      'usa-pagination__overflow',
    )

    // Going to page 4
    await page.click('[aria-label=Page4]')
    expect(await page.innerHTML('.usa-current')).toContain('4')

    await tiDashboard.expectPageNumberButtonNotPresent('6')
    await tiDashboard.expectPageNumberButtonNotPresent('7')

    expect(await page.innerHTML('.usa-pagination__list')).toContain(
      'usa-pagination__overflow',
    )

    await validateScreenshot(
      page.locator('.usa-pagination'),
      'ti-pagination-ellipses-right',
    )
  })

  test('shows two ellipses when there are 9 pages and there is overflow on both sides', async ({
    page,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    await tiDashboard.createMultipleClients('myname', 85)

    // Going to page 5
    await page.click('[aria-label=Page5]')
    expect(await page.innerHTML('.usa-current')).toContain('5')

    await tiDashboard.expectPageNumberButton('1')
    // An ellipses takes the place of 2 and 3 when current page is 5
    await tiDashboard.expectPageNumberButtonNotPresent('2')
    await tiDashboard.expectPageNumberButtonNotPresent('3')
    await tiDashboard.expectPageNumberButton('4')
    await tiDashboard.expectPageNumberButton('5')
    await tiDashboard.expectPageNumberButton('6')
    // An ellipses takes the place of 7 and 8 when current page is 5
    await tiDashboard.expectPageNumberButtonNotPresent('7')
    await tiDashboard.expectPageNumberButtonNotPresent('8')
    await tiDashboard.expectPageNumberButton('9')

    // There should be an ellipses
    expect(await page.innerHTML('.usa-pagination__list')).toContain(
      'usa-pagination__overflow',
    )

    await validateScreenshot(
      page.locator('.usa-pagination'),
      'ti-pagination-two-ellipses',
    )
  })

  test('shows one ellipses on the left when more than 7 pages and current page is one of the last 4 pages', async ({
    page,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)
    await tiDashboard.gotoTIDashboardPage(page)
    await waitForPageJsLoad(page)

    await tiDashboard.createMultipleClients('myname', 85)

    // Going to page 6 via page 5
    await page.click('[aria-label=Page5]')
    await page.click('.usa-pagination__next-page')
    expect(await page.innerHTML('.usa-current')).toContain('6')

    await tiDashboard.expectPageNumberButton('1')
    // The ellipses is on the left
    await tiDashboard.expectPageNumberButtonNotPresent('2')
    await tiDashboard.expectPageNumberButtonNotPresent('3')
    await tiDashboard.expectPageNumberButtonNotPresent('4')
    await tiDashboard.expectPageNumberButton('5')
    await tiDashboard.expectPageNumberButton('6')
    await tiDashboard.expectPageNumberButton('7')
    await tiDashboard.expectPageNumberButton('8')
    await tiDashboard.expectPageNumberButton('9')

    // There should be an ellipses
    expect(await page.innerHTML('.usa-pagination__list')).toContain(
      'usa-pagination__overflow',
    )

    await validateScreenshot(
      page.locator('.usa-pagination'),
      'ti-pagination-ellipses-left',
    )
  })
})
