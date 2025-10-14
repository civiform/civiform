import {test, expect, APIRequestContext} from '@playwright/test'
import {slugify} from './admin_programs'

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

  async seedApplications(programName: string, count: number) {
    await test.step(`Seed ${count} applications for program ${programName}`, async () => {
      const response = await this.request.post(
        '/dev/seedApplicationsHeadless',
        {
          form: {
            programSlug: slugify(programName),
            count: count.toString(),
          },
        },
      )
      await expect(response).toBeOK()
    })
  }
}
