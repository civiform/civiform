import { startSession, loginAsAdmin, loginAsProgramAdmin, loginAsGuest, loginAsTestUser, logout, endSession, NotFoundPage } from './support'

describe('error pages', () => {
  it('try invalid sub-url under all stages of logged in and not logged in', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);

    const notFound = new NotFoundPage(page);

    await notFound.gotoInvalidPage(page);

    await notFound.checkPageHeaderEnUS();

    //await page.pause();

    await endSession(browser);
  })
})
