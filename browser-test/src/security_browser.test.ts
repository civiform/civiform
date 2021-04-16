import { startSession, BASE_URL, gotoRootUrl, gotoEndpoint, loginWithSimulatedIdcs, loginAsAdmin, loginAsGuest, logout, endSession } from './support'
const { GenericContainer } = require("testcontainers");
var assert = require('assert');

describe('security browser testing', () => {
  beforeAll(async () => {
    let oidcProvider = await new GenericContainer("public.ecr.aws/t1q6b4h2/oidc-provider:latest")
        .withExposedPorts(3380)
  })

  it('Test with fake oidc', async () => {
    const { browser, page } = await startSession();

    // public void homePage_whenNotLoggedIn_redirectsToLoginForm() {
    await gotoRootUrl(page);
    let url = await page.url();
    let b_url = BASE_URL.concat('/loginForm');
    assert.equal(url, b_url);
    //await endSession(browser);

    // public void homePage_whenLoggedInAsAdmin_redirectsToAdminProgramList() {
    await loginAsAdmin(page);
    await gotoRootUrl(page);

    let pg_source = await page.content();

    url = await page.url();

    // REVIEWER PLEASE DOUBLE CHECK DESIRED URL VALUE
    b_url = BASE_URL.concat('/admin/programs');
    assert.equal(url, b_url);

    await logout(page);
    await gotoRootUrl(page);

    // public void homePage_whenLoggedInAsApplicant_redirectsToApplicantProgramList() {
    //await loginAsGuest(page);


    //await loginWithSimulatedIdcs(page);
  })
})
