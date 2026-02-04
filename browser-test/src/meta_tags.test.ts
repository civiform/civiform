import {test, expect} from './support/civiform_fixtures'

test.describe('navigating to a deep link', () => {
  test('has civiform build tag', async ({page, applicantQuestions}) => {
    await applicantQuestions.gotoApplicantHomePage()

    const metaTagLocator = page.locator('meta[name="civiform-build-tag"]')

    await expect(metaTagLocator).toHaveAttribute('content')
    expect(await metaTagLocator.getAttribute('content')).not.toBeNull()
  })
})
