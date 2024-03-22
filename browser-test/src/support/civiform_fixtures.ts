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
} from '.'

type CiviformFixtures = {
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
}

export const test = base.extend<CiviformFixtures>({
  adminApiKeys: async ({page}, use) => {
    await use(new AdminApiKeys(page))
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

  page: async ({page, request}, use) => {
    // BeforeEach
    await test.step('Clear database', async () => {
      await request.post('/dev/seed/clear')
    })

    await test.step('Go to home page before test starts', async () => {
      await page.goto('/programs')
      await waitForPageJsLoad(page)
      await page.locator('#warning-message-dismiss').click()
    })

    // Run the Test
    await use(page)

    // AfterEach
    // - none -
  },
})

export {expect} from '@playwright/test'
