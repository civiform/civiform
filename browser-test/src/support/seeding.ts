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
}
