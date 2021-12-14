import { startSession, loginAsAdmin, loginAsProgramAdmin, loginAsGuest, loginAsTestUser, logout, endSession, NotFoundPage } from './support'

describe('error pages', () => {
  it('try an actual non-existent page while logged out', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);

    const notFound = new NotFoundPage(page);

    await notFound.gotoNonExistentPage(page);

    await notFound.checkPageHeaderEnUS();

    //await page.pause();

    await endSession(browser);
  })

  it('try mock not found page', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);

    const notFound = new NotFoundPage(page);

    await notFound.gotoMockNotFoundPage(page);

    await notFound.checkPageHeaderEnUS();

    //await page.pause();

    await endSession(browser);
  })

})
