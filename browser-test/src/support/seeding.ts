import {test, expect, APIRequestContext} from '@playwright/test'

export class Seeding {
  public request!: APIRequestContext

  constructor(request: APIRequestContext) {
    this.request = request
  }

  async seedProgramsAndCategories() {
    await test.step('Seed programs and categories', async () => {
      const response = await this.request.post('/dev/seedProgramsHeadless')
      await expect(response).toBeOK()
    })
  }
}
