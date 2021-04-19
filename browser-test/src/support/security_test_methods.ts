import { startSession, BASE_URL, getUserId, gotoRootUrl, gotoEndpoint, loginWithSimulatedIdcs, loginAsAdmin, loginAsGuest, logout, endSession } from './'
import { assertEndpointEquals, assertPageIncludes } from './'

import { Page } from 'playwright'

var assert = require('assert');

export const homePage_whenNotLoggedIn_redirectsToLoginForm = async (page: Page) => {
    await gotoRootUrl(page);
    assertEndpointEquals(page, '/loginForm');
}

export const homePage_whenLoggedInAsAdmin_redirectsToAdminProgramList = async (page: Page) => {
    await loginAsAdmin(page);
    await gotoRootUrl(page);

    assertEndpointEquals(page, '/admin/programs');

    await logout(page);
    await gotoRootUrl(page);
}

export const homePage_whenLoggedInAsApplicant_redirectsToApplicantProgramList = async (page: Page) => {
    await loginAsGuest(page);

    let user_id = await getUserId(page);

    await gotoRootUrl(page);

    assertEndpointEquals(page, '/applicants/'.concat(user_id).concat('/programs'));
    await logout(page);
}

export const noCredLogin = async (page: Page) => {
    await loginAsGuest(page);

    await gotoEndpoint(page, '/securePlayIndex');
    await assertPageIncludes(page, 'You are logged in.');

    await gotoEndpoint(page, '/users/me');
    await assertPageIncludes(page, 'GuestClient');
    await assertPageIncludes(page, 'ROLE_APPLICANT');

    await gotoEndpoint(page, '/admin/programs');
    await assertPageIncludes(page, '403');

    await logout(page);
}

export const basicOidcLogin = async (page: Page) => {
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
}

export const mergeLogins = async (page: Page) => {
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
}

export const adminTestLogin = async (page: Page) => {
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
}
