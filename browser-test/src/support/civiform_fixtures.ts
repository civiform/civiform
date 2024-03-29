import {test as base} from '@playwright/test'
import {
  AdminPrograms,
  AdminQuestions,
  AdminProgramMigration,
  AdminProgramStatuses,
  ApplicantQuestions,
  AdminPredicates,
  AdminTranslations,
  AdminProgramImage,
  ApplicantFileQuestion,
  TIDashboard,
  AdminTIGroups,
  waitForPageJsLoad,
  AdminSettings,
} from '.'
import {AdminApiKeys} from './admin_api_keys'

type CiviformFixtures = {
  adminApiKeys: AdminApiKeys
  adminPrograms: AdminPrograms
  adminQuestions: AdminQuestions
  adminProgramMigration: AdminProgramMigration
  adminProgramStatuses: AdminProgramStatuses
  applicantQuestions: ApplicantQuestions
  adminPredicates: AdminPredicates
  adminTranslations: AdminTranslations
  adminProgramImage: AdminProgramImage
  adminSettings: AdminSettings
  applicantFileQuestion: ApplicantFileQuestion
  tiDashboard: TIDashboard
  adminTiGroups: AdminTIGroups
}

export const test = base.extend<CiviformFixtures>({
  adminApiKeys: async ({page, request}, use) => {
    await use(new AdminApiKeys(page, request))
  },

  adminPrograms: async ({page}, use) => {
    await use(new AdminPrograms(page))
  },

  adminQuestions: async ({page}, use) => {
    await use(new AdminQuestions(page))
  },

  adminProgramMigration: async ({page}, use) => {
    await use(new AdminProgramMigration(page))
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

  adminSettings: async ({page}, use) => {
    await use(new AdminSettings(page))
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
