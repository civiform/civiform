import { startSession, loginWithSimulatedIdcs, endSession } from './support'
const { GenericContainer } = require("testcontainers");

describe('security browser testing', () => {
  beforeAll(async () => {
    let oidcProvider = await new GenericContainer("public.ecr.aws/t1q6b4h2/oidc-provider:latest")
        .withExposedPorts(3380)
  })

  it('Test with fake oidc', async () => {
    const { browser, page } = await startSession();

    await loginWithSimulatedIdcs(page);

    let pg_source = await page.content();

    if (pg_source.includes("Enter any login")) {
      //console.log(pg_source);
      await page.click('css=[name=login]');

    }
    await endSession(browser);
  })
})
