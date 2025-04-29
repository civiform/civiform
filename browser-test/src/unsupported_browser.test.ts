import {test, expect} from './support/civiform_fixtures'

test.describe('unsupported browser', {tag: ['@parallel-candidate']}, () => {
  test.use({
    userAgent: 'Mozilla/5.0 (Windows NT 6.1; Trident/7.0; rv:11.0) like Gecko',
  })

  test('redirect ie user agent to page unsupported page', async ({page}) => {
    await page.goto('/')
    expect(page.url()).toContain('/support/unsupportedBrowser')
  })
})
