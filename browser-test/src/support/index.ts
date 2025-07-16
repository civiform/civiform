/*

This barrel file is solely for backwards compatibility with existing tests.

Do NOT add functions to this file.

*/

export {AdminQuestions} from './admin_questions'
export {AdminPredicates} from './admin_predicates'
export {AdminPrograms} from './admin_programs'
export {AdminProgramStatuses} from './admin_program_statuses'
export {AdminSettings} from './admin_settings'
export {AdminTranslations} from './admin_translations'
export {AdminProgramImage} from './admin_program_image'
export {AdminTIGroups} from './admin_ti_groups'
export {ApplicantFileQuestion} from './applicant_file_question'
export {ApplicantQuestions} from './applicant_questions'
export {ClientInformation, TIDashboard} from './ti_dashboard'
export {clickAndWaitForModal, dismissModal, waitForPageJsLoad} from './wait'
export {validateScreenshot} from './screenshots'
export {extractEmailsForRecipient, supportsEmailInspection} from './email'
export {
  loginAsAdmin,
  loginAsCiviformAndProgramAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  loginAsTrustedIntermediary,
  logout,
  testUserDisplayName,
} from './auth'
export {
  isLocalDevEnvironment,
  dismissToast,
  selectApplicantLanguage,
  disableFeatureFlag,
  enableFeatureFlag,
  closeWarningMessage,
  validateAccessibility,
  normalizeElements,
  validateToastHidden,
  validateToastMessage,
  throttle,
} from './helpers'

// Do NOT add functions to this file.


export const loginAsAdmin = async (page: Page) => {
  await test.step('Login as Civiform Admin', async () => {
    switch (TEST_USER_AUTH_STRATEGY) {
      case AuthStrategy.KEYCLOAK:
        await loginAsTestUserKeyCloak(page, 'civiformadmin1', 'password', 'a:has-text("Admin login")')
        break;

      default:
        await page.click('#debug-content-modal-button')
        await page.click('#admin')
        await waitForPageJsLoad(page)
    }
  })
}

export const loginAsProgramAdmin = async (page: Page) => {
  await test.step('Login as Program Admin', async () => {
    switch (TEST_USER_AUTH_STRATEGY) {
      case AuthStrategy.KEYCLOAK:
        await loginAsTestUserKeyCloak(page, 'programadmin1', 'password', 'a:has-text("Admin login")')
        break;

      default:
        await page.click('#debug-content-modal-button')
        await page.click('#program-admin')
        await waitForPageJsLoad(page)
    }    
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
      case AuthStrategy.KEYCLOAK:
        await loginAsTestUserKeyCloak(page, 'applicant1', 'password', loginButton)
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

async function loginAsTestUserKeyCloak(
  page: Page,
  username: string,
  password: string,
  loginButton: string
) {
  await page.click(loginButton)
  // Wait for the IDCS login page to make sure we've followed all redirects.
  // If running this against a site with a real IDCS (i.e. staging) and this
  // test fails with a timeout try re-running the tests. Sometimes there are
  // just transient network hiccups that will pass on a second run.
  // In short: If using a real IDCS retry test if this has a timeout failure.
  await page.waitForURL('**/auth*')
  await page.fill('input[name=username]', username)
  await page.fill('input[name=password]', password)
  await page.click('button:has-text("Sign In"):not([disabled])')
  // await page.waitForNavigation({ waitUntil: 'networkidle' })
}

