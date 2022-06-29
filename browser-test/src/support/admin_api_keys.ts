import axios from 'axios'
import {Page} from 'playwright'
import {readFileSync} from 'fs'
import {waitForPageJsLoad} from './wait'
import {BASE_URL} from './config'

type CreateApiKeyParamsType = {
  name: string
  expiration: string
  subnet: string
  programSlugs: Array<string>
}

export class AdminApiKeys {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  // Create a new ApiKey, returning the credentials string
  async createApiKey({
    name,
    expiration,
    subnet,
    programSlugs,
  }: CreateApiKeyParamsType): Promise<string> {
    await this.gotoNewApiKeyPage()
    await this.page.fill('#keyName', name)
    await this.page.fill('#expiration', expiration)
    await this.page.fill('#subnet', subnet)

    for (const slug of programSlugs) {
      await this.page.check(`#${slug}`)
    }

    await this.page.click('#apikey-submit-button')
    await waitForPageJsLoad(this.page)

    await this.expectApiKeyCredentialsPage(name)
    return await this.page.innerText('#api-key-credentials')
  }

  async callCheckAuth(credentials: string): Promise<{status: number}> {
    return await axios.get(BASE_URL + '/api/v1/checkAuth', {
      headers: {Authorization: 'Basic ' + credentials},
    })
  }

  async expectApiKeyCredentialsPage(name: string) {
    expect(await this.page.innerText('h1')).toEqual(`Created API key: ${name}`)
  }

  async gotoNewApiKeyPage() {
    await this.gotoApiKeyIndexPage()
    await this.page.click('#new-api-key-button')
    await waitForPageJsLoad(this.page)
    await this.expectNewApiKeyPage()
  }

  async expectNewApiKeyPage() {
    expect(await this.page.innerText('h1')).toEqual('Create a new API key')
  }

  async expectKeyCallCount(
    keyNameSlugified: string,
    expectedCallCount: number,
    timeoutMillis = 3000,
  ) {
    const startTime = Date.now()
    const maxWaitTime = startTime + timeoutMillis
    const expectedCallCountText = `Call count: ${expectedCallCount}`
    let callCountText = ''

    while (true) {
      await this.gotoApiKeyIndexPage()

      try {
        callCountText = await this.page.innerText(
          `#${keyNameSlugified}-call-count`,
          {timeout: 100},
        )
      } catch (e) {
        console.log(`failed to find #${keyNameSlugified}-call-count`)
      }

      if (callCountText != expectedCallCountText || Date.now() > maxWaitTime) {
        break
      }
    }

    expect(callCountText).toContain(expectedCallCountText)
  }

  async expectLastCallIpAddressToBeSet(
    keyNameSlugified: string,
    timeoutMillis = 3000,
  ) {
    const startTime = Date.now()
    const maxWaitTime = startTime + timeoutMillis
    let lastCallIpText = ''

    while (true) {
      await this.gotoApiKeyIndexPage()

      try {
        lastCallIpText = await this.page.innerText(
          `#${keyNameSlugified}-last-call-ip`,
          {timeout: 100},
        )
      } catch (e) {
        console.log(`failed to find #${keyNameSlugified}-last-call-ip`)
      }

      if (!lastCallIpText.includes('N/A') || Date.now() > maxWaitTime) {
        break
      }
    }

    expect(lastCallIpText).toContain('Last used by')
    expect(lastCallIpText).not.toContain('N/A')
  }

  async retireApiKey(keyNameSlugified: string) {
    await this.gotoApiKeyIndexPage()
    this.page.on('dialog', (dialog) => dialog.accept())
    await this.page.click(`#retire-${keyNameSlugified} button`)
  }

  async expectApiKeyIsActive(keyName: string) {
    await this.gotoApiKeyIndexPage()
    expect(await this.page.innerText('.cf-api-key-name')).toContain(
      `${keyName} active`,
    )
  }

  async expectApiKeyIsRetired(keyName: string) {
    await this.gotoApiKeyIndexPage()
    expect(await this.page.innerText('.cf-api-key-name')).toContain(
      `${keyName} retired`,
    )
  }

  async gotoApiKeyIndexPage() {
    await this.page.click('nav :text("API keys")')
    await waitForPageJsLoad(this.page)
    await this.expectApiKeysIndexPage()
  }

  async expectApiKeysIndexPage() {
    expect(await this.page.innerText('h1')).toEqual('API Keys')
  }
}
