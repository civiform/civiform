import {test as base} from '@playwright/test'
import {
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
  AdminSettings,
} from '.'
import {AdminApiKeys} from './admin_api_keys'
import {AdminProgramMigration} from './admin_program_migration'
import {ApplicantProgramList} from './applicant_program_list'
import {Seeding} from './seeding'

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
  applicantProgramList: ApplicantProgramList
  tiDashboard: TIDashboard
  adminTiGroups: AdminTIGroups
  seeding: Seeding
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

  applicantProgramList: async ({page}, use) => {
    await use(new ApplicantProgramList(page))
  },

  tiDashboard: async ({page}, use) => {
    await use(new TIDashboard(page))
  },

  adminTiGroups: async ({page}, use) => {
    await use(new AdminTIGroups(page))
  },

  seeding: async ({request}, use) => {
    await use(new Seeding(request))
  },

  page: async ({page, request}, use) => {
    page.on('console', (msg) => {
      if (msg.text().includes('Content Security Policy')) {
        throw new Error(msg.text())
      }
    })

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
