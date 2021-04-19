import { startSession, BASE_URL, getUserId, gotoRootUrl, gotoEndpoint, loginWithSimulatedIdcs, loginAsAdmin, loginAsGuest, logout, endSession } from './support'
import { homePage_whenNotLoggedIn_redirectsToLoginForm, homePage_whenLoggedInAsAdmin_redirectsToAdminProgramList, noCredLogin, basicOidcLogin, mergeLogins, adminTestLogin } from './support/security_test_methods'

const { GenericContainer } = require("testcontainers");
var assert = require('assert');

describe('security browser testing', () => {
  beforeAll(async () => {
    let oidcProvider = await new GenericContainer("public.ecr.aws/t1q6b4h2/oidc-provider:latest")
      .withExposedPorts(3380)
  })

  it('Test with fake oidc', async () => {
    const { browser, page } = await startSession();

    await homePage_whenNotLoggedIn_redirectsToLoginForm(page);
    await homePage_whenLoggedInAsAdmin_redirectsToAdminProgramList(page);
    await noCredLogin(page);
    await basicOidcLogin(page);
    await mergeLogins(page);
    await adminTestLogin(page);

    await endSession(browser);
  })
})
