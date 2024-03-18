import {test as base} from '@playwright/test'
import {
  AdminApiKeys,
  AdminPrograms,
  AdminQuestions,
  AdminProgramStatuses,
  ApplicantQuestions,
  AdminPredicates,
  AdminTranslations,
  AdminProgramImage,
  ApplicantFileQuestion,
  TIDashboard,
  AdminTIGroups,
  waitForPageJsLoad,
  NotFoundPage,
} from '../support'
import {Helpers} from '../support/helpers'

type CustomFixture = {
  adminApiKeys: AdminApiKeys
  adminPrograms: AdminPrograms
  adminQuestions: AdminQuestions
  adminProgramStatuses: AdminProgramStatuses
  applicantQuestions: ApplicantQuestions
  adminPredicates: AdminPredicates
  adminTranslations: AdminTranslations
  adminProgramImage: AdminProgramImage
  applicantFileQuestion: ApplicantFileQuestion
  tiDashboard: TIDashboard
  adminTiGroups: AdminTIGroups
  notFoundPage: NotFoundPage
  helpers: Helpers
}

export const test = base.extend<CustomFixture>({
  adminApiKeys: async ({page, request}, use) => {
    await use(new AdminApiKeys(page, request))
  },

  adminPrograms: async ({page}, use) => {
    await use(new AdminPrograms(page))
  },

  adminQuestions: async ({page}, use) => {
    await use(new AdminQuestions(page))
  },

  adminProgramStatuses: async ({page}, use) => {
    await use(new AdminProgramStatuses(page))
  },

  applicantQuestions: async ({page}, use) => {
    await use(new ApplicantQuestions(page))
  },

  adminPredicates: async ({page}, use) => {
    await use(new AdminPredicates(page))
  },

  adminTranslations: async ({page}, use) => {
    await use(new AdminTranslations(page))
  },

  adminProgramImage: async ({page}, use) => {
    await use(new AdminProgramImage(page))
  },

  applicantFileQuestion: async ({page}, use) => {
    await use(new ApplicantFileQuestion(page))
  },

  tiDashboard: async ({page}, use) => {
    await use(new TIDashboard(page))
  },

  adminTiGroups: async ({page}, use) => {
    await use(new AdminTIGroups(page))
  },

  notFoundPage: async ({page}, use) => {
    await use(new NotFoundPage(page))
  },

  helpers: async ({page, request}, use) => {
    await use(new Helpers(page, request))
  },

  page: async ({page, request}, use) => {
    await request.post('/dev/seed/clear')
    await page.goto('/programs')
    await waitForPageJsLoad(page)
    await page.locator('#warning-message-dismiss').click()
    await use(page)
  },
})

export {expect} from '@playwright/test'
