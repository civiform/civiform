import {test, expect, APIRequestContext} from '@playwright/test'

export class Seeding {
  public request!: APIRequestContext

  constructor(request: APIRequestContext) {
    this.request = request
  }

  async seedQuestions() {
    await test.step('Seed questions', async () => {
      const response = await this.request.post('/dev/seedQuestionsHeadless')
      await expect(response).toBeOK()
    })
  }

  async clearDatabase() {
    await test.step('Clear database', async () => {
      const response = await this.request.post('/dev/seed/clearHeadless')
      await expect(response).toBeOK()
    })
  }
}
