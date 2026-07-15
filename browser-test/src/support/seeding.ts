import {test, expect, APIRequestContext} from '@playwright/test'
import {slugify} from './admin_programs'

/**
 * Admin names of the canned sample questions created by
 * Seeding.seedQuestions(), one per question type. Must stay in sync with
 * server/app/controllers/dev/seeding/SampleQuestionDefinitions.java.
 */
export const SAMPLE_QUESTIONS = {
  address: 'Sample Address Question',
  checkbox: 'Sample Checkbox Question',
  currency: 'Sample Currency Question',
  date: 'Sample Date Question',
  datePredicate: 'Sample Predicate Date Question',
  dropdown: 'Sample Dropdown Question',
  email: 'Sample Email Question',
  enumerator: 'Sample Enumerator Question',
  fileUpload: 'Sample File Upload Question',
  id: 'Sample ID Question',
  name: 'Sample Name Question',
  number: 'Sample Number Question',
  phone: 'Sample Phone Question',
  radioButton: 'Sample Radio Button Question',
  staticContent: 'Sample Static Content Question',
  text: 'Sample Text Question',
} as const

/**
 * Display names of the canned sample programs created by
 * Seeding.seedProgramsAndCategories(), which also creates all of the
 * SAMPLE_QUESTIONS. Must stay in sync with
 * server/app/controllers/dev/seeding/DevDatabaseSeedTask.java.
 */
export const SAMPLE_PROGRAMS = {
  /** One screen with an optional name question. */
  minimal: 'Minimal Sample Program',
  /** One of every question type across multiple screens. */
  comprehensive: 'Comprehensive Sample Program',
} as const

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
