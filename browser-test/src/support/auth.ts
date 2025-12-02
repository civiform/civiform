import {test, Page} from '@playwright/test'
import {waitForPageJsLoad} from './wait'
import {
  TEST_USER_AUTH_STRATEGY,
  TEST_USER_LOGIN,
  TEST_USER_PASSWORD,
  TEST_USER_DISPLAY_NAME,
} from './config'
import {dismissToast, validateToastMessage} from './helpers'

/**
 * Different auth strategies that are being exercised in this test. Each strategy
 * requires different logic for login (which fields to fill and button to click on
 * login page) and logout (some logout flows require confirmation).
 */
enum AuthStrategy {
  FAKE_OIDC = 'fake-oidc',
  AWS_STAGING = 'aws-staging',
  SEATTLE_STAGING = 'seattle-staging',
}

export const logout = async (page: Page, closeToast = true) => {
  await test.step('Logout', async () => {
    await page.click('#logout-button')
    // If the user logged in through OIDC previously - during logout they are
    // redirected to dev-oidc:PORT/session/end page. There they need to confirm
    // logout.
    if (page.url().match('dev-oidc.*/session/end')) {
      const pageContent = await page.textContent('html')
      if (pageContent!.includes('Do you want to sign-out from')) {
        // OIDC central provider confirmation page
        await page.click('button:has-text("Yes")')
      }
    }

    // Logout is handled by the play framework so it doesn't land on a
    // page with civiform js where we should waitForPageJsLoad. Because
    // the process goes through a sequence of redirects we need to wait
    // for the final destination URL (the programs index page), to make tests reliable.
    await page.waitForURL('**/programs')
    await validateToastMessage(page, 'Your session has ended.')
    if (closeToast) await dismissToast(page)
  })
}

export const loginAsAdmin = async (page: Page) => {
  await test.step('Login as Civiform Admin', async () => {
    await page.click('#debug-content-modal-button')
    await page.click('#admin')
    await waitForPageJsLoad(page)
  })
}

export const loginAsProgramAdmin = async (page: Page) => {
  await test.step('Login as Program Admin', async () => {
    await page.click('#debug-content-modal-button')
    await page.click('#program-admin')
    await waitForPageJsLoad(page)
  })
}

export const loginAsCiviformAndProgramAdmin = async (page: Page) => {
  await test.step('Login as Civiform and Program Admin', async () => {
    await page.click('#debug-content-modal-button')
    await page.click('#dual-admin')
    await waitForPageJsLoad(page)
  })
}

export const loginAsTrustedIntermediary = async (page: Page) => {
  await test.step('Login as Trusted Intermediary', async () => {
    await page.click('#debug-content-modal-button')
    await page.click('#trusted-intermediary')
    await waitForPageJsLoad(page)
  })
}

/**
 * Logs in via an auth provider.
 * @param loginButton Selector of a button on current page that starts auth
 *     login. Normally it's "Log in" button on main page, but in some cases
 *     login can be initiated from different pages, for example after program
 *     submission.
 */
export const loginAsTestUser = async (
  page: Page,
  loginButton = 'a:has-text("Log in")',
  isTi = false,
  displayName: string = '',
) => {
  await test.step('Login as Test User', async () => {
    switch (TEST_USER_AUTH_STRATEGY) {
      case AuthStrategy.FAKE_OIDC:
        await loginAsTestUserFakeOidc(page, loginButton, isTi)
        break
      case AuthStrategy.AWS_STAGING:
        await loginAsTestUserAwsStaging(page, loginButton, isTi)
        break
      case AuthStrategy.SEATTLE_STAGING:
        await loginAsTestUserSeattleStaging(page, loginButton)
        break
      default:
        throw new Error(
          `Unrecognized or unset TEST_USER_AUTH_STRATEGY environment variable of '${TEST_USER_AUTH_STRATEGY}'`,
        )
    }
    await waitForPageJsLoad(page)
    if (displayName === '') {
      displayName = testUserDisplayName()
    }
    await page.waitForSelector(`:has-text("Logged in as ${displayName}")`)
  })
}

async function loginAsTestUserSeattleStaging(page: Page, loginButton: string) {
  await page.click(loginButton)
  // Wait for the IDCS login page to make sure we've followed all redirects.
  // If running this against a site with a real IDCS (i.e. staging) and this
  // test fails with a timeout try re-running the tests. Sometimes there are
  // just transient network hiccups that will pass on a second run.
  // In short: If using a real IDCS retry test if this has a timeout failure.
  await page.waitForURL('**/#/login*')
  await page.fill('input[name=userName]', TEST_USER_LOGIN)
  await page.fill('input[name=password]', TEST_USER_PASSWORD)
  await page.click('button:has-text("Login"):not([disabled])')
  // eslint-disable-next-line playwright/no-wait-for-navigation
  await page.waitForNavigation({waitUntil: 'networkidle'})
}

async function loginAsTestUserAwsStaging(
  page: Page,
  loginButton: string,
  isTi: boolean,
) {
  await Promise.all([
    // eslint-disable-next-line playwright/no-networkidle
    page.waitForURL('**/u/login*', {waitUntil: 'networkidle'}),
    page.click(loginButton),
  ])

  await page.fill('input[name=username]', TEST_USER_LOGIN)
  await page.fill('input[name=password]', TEST_USER_PASSWORD)
  await Promise.all([
    page.waitForURL(isTi ? '**/admin/**' : /.*\/programs.*/, {
      // eslint-disable-next-line playwright/no-networkidle
      waitUntil: 'networkidle',
    }),
    // Auth0 has an additional hidden "Continue" button that does nothing for some reason
    // getByRole selects items by their accessible name, so it only selects the visible button
    page.getByRole('button', {name: 'Continue', exact: true}).click(),
  ])
}

async function loginAsTestUserFakeOidc(
  page: Page,
  loginButton: string,
  isTi: boolean,
) {
  await page.click(loginButton)
  await page.waitForURL('**/interaction/*')

  // If the user has previously signed in to the provider, a prompt is shown
  // to reauthorize rather than sign-in. In this case, click "Continue" instead
  // and skip filling out any login information. If we want to support logging
  // in as multiple users, this will need to be adjusted.
  const pageText = await page.innerText('html')
  if (
    pageText.includes(
      'the client is asking you to confirm previously given authorization',
    )
  ) {
    throw new Error(
      'Unexpected reauthorization page. Central logout should fully logout user.',
    )
  }

  await page.fill('input[name=login]', TEST_USER_LOGIN)
  await page.fill('input[name=password]', TEST_USER_PASSWORD)

  await page.click('button:has-text("Sign-in"):not([disabled])')
  await page.waitForURL('**/interaction/*')

  // A screen is shown prompting the user to authorize a set of scopes.
  // This screen is skipped if the user has already logged in once.
  await page.click('button:has-text("Continue")')
  await page.waitForURL(isTi ? '**/admin/**' : /\/programs.*/)
}

export const testUserDisplayName = () => {
  if (!TEST_USER_DISPLAY_NAME) {
    throw new Error(
      'Empty or unset TEST_USER_DISPLAY_NAME environment variable',
    )
  }
  return TEST_USER_DISPLAY_NAME
}
