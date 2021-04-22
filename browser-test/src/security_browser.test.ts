import { assertEndpointEquals, assertPageIncludes } from './support' 
import { startSession, getUserId, gotoRootUrl, gotoEndpoint } from './support'
import { loginWithSimulatedIdcs, loginAsAdmin, loginAsGuest } from './support'
import { logout, endSession } from './support'
import { BASE_URL } from './support'

const { GenericContainer } = require("testcontainers");
var assert = require('assert');

describe('security browser testing', () => {
  beforeAll(async () => {
    let oidcProvider = await new GenericContainer("public.ecr.aws/t1q6b4h2/oidc-provider:latest")
      .withExposedPorts(3380)
  })

  it('homePage_whenNotLoggedIn_redirectsToLoginForm', async () => {
    const { browser, page } = await startSession();
    await gotoRootUrl(page);
    assertEndpointEquals(page, '/loginForm');
  })

  it('homePage_whenLoggedInAsAdmin_redirectsToAdminProgramList', async () => {
    const { browser, page } = await startSession();
    await loginAsAdmin(page);
    await gotoRootUrl(page);

    assertEndpointEquals(page, '/admin/programs');

    await logout(page);
    await gotoRootUrl(page);
  })

  it('homePage_whenLoggedInAsApplicant_redirectsToApplicantProgramList', async () => {
    const { browser, page } = await startSession();
    await loginAsGuest(page);

    let user_id = await getUserId(page);

    await gotoRootUrl(page);

    assertEndpointEquals(page, '/applicants/'.concat(user_id).concat('/programs'));
    await logout(page);
  })

  it('noCredLogin', async () => {
    const { browser, page } = await startSession();
    await loginAsGuest(page);

    await gotoEndpoint(page, '/securePlayIndex');
    await assertPageIncludes(page, 'You are logged in.');

    await gotoEndpoint(page, '/users/me');
    await assertPageIncludes(page, 'GuestClient');
    await assertPageIncludes(page, 'ROLE_APPLICANT');

    await gotoEndpoint(page, '/admin/programs');
    await assertPageIncludes(page, '403');

    await logout(page);
  })

  it('basicOidcLogin', async () => {
    const { browser, page } = await startSession();
    console.log(await page.url());
    await loginWithSimulatedIdcs(page);
    await gotoEndpoint(page, '/securePlayIndex');

    // -- FAILS
    // -- Reviewer Please double check 
    await assertPageIncludes(page, 'You are logged in.');

    await gotoEndpoint(page, '/users/me');
    let pg_source = await page.content();

    // -- FAILS
    // -- Reviewer Please double check 
    await assertPageIncludes(page, 'OidcClient');
    await assertPageIncludes(page, 'username@example.com');

    await logout(page);
  })

  it('mergeLogins', async () => {
    const { browser, page } = await startSession();
    await loginAsGuest(page);

    await gotoEndpoint(page, '/users/me');
    let pg_source = await page.content();
    assert(pg_source.includes('GuestClient'));

    let user_id = await getUserId(page);

    //await gotoRootUrl(page);

    // Had to do this because login attempt after gotoRootUrl 
    // resulted in timeout error
    await gotoEndpoint(page, '/loginForm');
    await loginWithSimulatedIdcs(page);

    await gotoEndpoint(page, '/users/me');

    // Fails after no-fail login attempt
    await assertPageIncludes(page, 'OidcClient');

    // FAILS on getting user id
    let user_id2 = await getUserId(page);
    assert.equal(user_id, user_id2);

    await logout(page);
    await gotoEndpoint(page, '/loginForm');
    await loginWithSimulatedIdcs(page);

    // Fails after no-fail login attempt
    //assert(pg_source.includes('OidcClient'));

    let user_id3 = await getUserId(page);
    assert.equal(user_id, user_id3);

    await logout(page);
  })

  it('adminTestLogin', async () => {
    const { browser, page } = await startSession();
    await loginAsAdmin(page);
    await gotoEndpoint(page, '/securePlayIndex');
    let pg_source = await page.content();
    assert(pg_source.includes('You are logged in.'));

    await gotoEndpoint(page, '/users/me');
    pg_source = await page.content();
    assert(pg_source.includes('FakeAdminClient'));
    assert(pg_source.includes('ROLE_UAT_ADMIN'));

    await gotoEndpoint(page, '/admin/programs');
    pg_source = await page.content();
    assert(pg_source.includes('Programs'));
    assert(pg_source.includes('Create new program'));
  })
})
