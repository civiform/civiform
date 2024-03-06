import {APIRequestContext, expect} from '@playwright/test'
import {Page} from 'playwright'
import { AuthStrategy, dismissToast, validateToastMessage, waitForPageJsLoad } from '.'
import { TEST_USER_AUTH_STRATEGY, TEST_USER_DISPLAY_NAME, TEST_USER_LOGIN, TEST_USER_PASSWORD } from './config'

export class Helpers {

  private page!: Page
  private request!: APIRequestContext

  constructor(page: Page, request: APIRequestContext) {
    this.page = page
    this.request = request
  }

  async dismissToast () {
    await this.page.locator('#toast-container div:text("x")').click()
    await waitForPageJsLoad(this.page)
  }
  
  async logout (closeToast = true) {
    await this.page.click('#logout-button')
    // If the user logged in through OIDC previously - during logout they are
    // redirected to dev-oidc:PORT/session/end page. There they need to confirm
    // logout.
    if (this.page.url().match('dev-oidc.*/session/end')) {
      const pageContent = await this.page.textContent('html')
      if (pageContent!.includes('Do you want to sign-out from')) {
        // OIDC central provider confirmation page
        await this.page.click('button:has-text("Yes")')
      }
    }
  
    // Logout is handled by the play framework so it doesn't land on a
    // page with civiform js where we should waitForPageJsLoad. Because
    // the process goes through a sequence of redirects we need to wait
    // for the final destination URL (the programs index page), to make tests reliable.
    await this.page.waitForURL('**/programs')
    await validateToastMessage(this.page, 'Your session has ended.')
    if (closeToast) await dismissToast(this.page)
  }
  
  async loginAsAdmin() {
    await this.page.goto('/callback?client_name=FakeAdminClient&adminType=GLOBAL')
    // await this.page.click('#debug-content-modal-button')
    // await this.page.click('#admin')
    await waitForPageJsLoad(this.page)
  }
  
  async loginAsProgramAdmin () {
    await this.page.goto('/callback?client_name=FakeAdminClient&adminType=PROGRAM')
    // await this.page.click('#debug-content-modal-button')
    // await this.page.click('#program-admin')
    await waitForPageJsLoad(this.page)
  }
  
  async loginAsCiviformAndProgramAdmin () {
    await this.page.goto('/callback?client_name=FakeAdminClient&adminType=DUAL')
    // await this.page.click('#debug-content-modal-button')
    // await this.page.click('#dual-admin')
    await waitForPageJsLoad(this.page)
  }
  
  async loginAsTrustedIntermediary () {
    await this.page.goto('/callback?client_name=FakeAdminClient&adminType=TRUSTED_INTERMEDIARY')
    // await this.page.click('#debug-content-modal-button')
    // await this.page.click('#trusted-intermediary')
    await waitForPageJsLoad(this.page)
  }
  
  /**
   * Logs in via an auth provider.
   * @param loginButton Selector of a button on current page that starts auth
   *     login. Normally it's "Log in" button on main page, but in some cases
   *     login can be initiated from different pages, for example after program
   *     submission.
   */
  async loginAsTestUser (loginButton = 'a:has-text("Log in")', isTi = false) {
    switch (TEST_USER_AUTH_STRATEGY) {
      case AuthStrategy.FAKE_OIDC:
        await this.loginAsTestUserFakeOidc(loginButton, isTi)
        break
      case AuthStrategy.AWS_STAGING:
        await this.loginAsTestUserAwsStaging(loginButton, isTi)
        break
      case AuthStrategy.SEATTLE_STAGING:
        await this.loginAsTestUserSeattleStaging(loginButton)
        break
      default:
        throw new Error(
          `Unrecognized or unset TEST_USER_AUTH_STRATEGY environment variable of '${TEST_USER_AUTH_STRATEGY}'`,
        )
    }
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector(
      `:has-text("Logged in as ${this.testUserDisplayName()}")`,
    )
  }
  
  async loginAsTestUserSeattleStaging(loginButton: string) {
    await this.page.click(loginButton)
    // Wait for the IDCS login page to make sure we've followed all redirects.
    // If running this against a site with a real IDCS (i.e. staging) and this
    // test fails with a timeout try re-running the tests. Sometimes there are
    // just transient network hiccups that will pass on a second run.
    // In short: If using a real IDCS retry test if this has a timeout failure.
    await this.page.waitForURL('**/#/login*')
    await this.page.fill('input[name=userName]', TEST_USER_LOGIN)
    await this.page.fill('input[name=password]', TEST_USER_PASSWORD)
    await this.page.click('button:has-text("Login"):not([disabled])')
    await this.page.waitForNavigation({waitUntil: 'networkidle'})
  }
  
  async loginAsTestUserAwsStaging(
    loginButton: string,
    isTi: boolean,
  ) {
    await Promise.all([
      this.page.waitForURL('**/u/login*', {waitUntil: 'networkidle'}),
      this.page.click(loginButton),
    ])
  
    await this.page.fill('input[name=username]', TEST_USER_LOGIN)
    await this.page.fill('input[name=password]', TEST_USER_PASSWORD)
    await Promise.all([
      this.page.waitForURL(isTi ? '**/admin/**' : /.*\/programs.*/, {
        waitUntil: 'networkidle',
      }),
      // Auth0 has an additional hidden "Continue" button that does nothing for some reason
      this.page.click('button:visible:has-text("Continue")'),
    ])
  }
  
  async loginAsTestUserFakeOidc(
    loginButton: string,
    isTi: boolean,
  ) {
    await Promise.all([
      this.page.waitForURL('**/interaction/*', {waitUntil: 'networkidle'}),
      this.page.click(loginButton),
    ])
  
    // If the user has previously signed in to the provider, a prompt is shown
    // to reauthorize rather than sign-in. In this case, click "Continue" instead
    // and skip filling out any login information. If we want to support logging
    // in as multiple users, this will need to be adjusted.
    const pageText = await this.page.innerText('html')
    if (
      pageText.includes(
        'the client is asking you to confirm previously given authorization',
      )
    ) {
      throw new Error(
        'Unexpected reauthorization page. Central logout should fully logout user.',
      )
    }
  
    await this.page.fill('input[name=login]', TEST_USER_LOGIN)
    await this.page.fill('input[name=password]', TEST_USER_PASSWORD)
    await Promise.all([
      this.page.waitForURL('**/interaction/*', {waitUntil: 'networkidle'}),
      this.page.click('button:has-text("Sign-in"):not([disabled])'),
    ])
    // A screen is shown prompting the user to authorize a set of scopes.
    // This screen is skipped if the user has already logged in once.
    await Promise.all([
      this.page.waitForURL(isTi ? '**/admin/**' : /\/programs.*/, {
        waitUntil: 'networkidle',
      }),
      this.page.click('button:has-text("Continue")'),
    ])
  }
  
  testUserDisplayName() {
    if (!TEST_USER_DISPLAY_NAME) {
      throw new Error(
        'Empty or unset TEST_USER_DISPLAY_NAME environment variable',
      )
    }
    return TEST_USER_DISPLAY_NAME
  }
  
  supportsEmailInspection() {
    return TEST_USER_AUTH_STRATEGY === 'fake-oidc'
  }
  
  /**
   * The option to select a language is shown in the header bar as a dropdown. This helper method selects the given language from the dropdown.
   */
  async selectApplicantLanguage(language: string) {
    await this.page.click('#select-language')
    await this.page.selectOption('#select-language', {label: language})
  
    await waitForPageJsLoad(this.page)
  }
  
  async dropTables () {
    await this.page.goto('/dev/seed')
    await this.page.click('#clear')
  }
  
  async seedQuestions () {
    // await this.request.post('/dev/seed/clear')
    // await this.page.goto("/programs")
    await this.page.goto('/dev/seed')
    await this.page.click('#sample-questions')
  }
  
  async seedPrograms () {
    await this.page.goto('/dev/seed')
    await this.page.click('#sample-programs')
  }
  
  async disableFeatureFlag (flag: string) {
    // const response = await this.request.get(`/dev/feature/${flag}/disable`)
    // expect(response.ok()).toBeTruthy()
    await this.page.goto(`/dev/feature/${flag}/disable`)
  }
  
  async enableFeatureFlag (flag: string) {
    // await this.request.get(`/dev/feature/${flag}/enable`)
    await this.page.goto(`/dev/feature/${flag}/enable`)
  }
  
  async closeWarningMessage () {
    // The warning message may be in the way of this link
    const element = await this.page.$('#warning-message-dismiss')
  
    if (element !== null) {
      await element
        .click()
        .catch(() =>
          console.log(
            "Didn't find a warning toast message to dismiss, which is fine.",
          ),
        )
    }
  }
  
  async validateToastMessage (value: string) {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain(value)
  }
}
