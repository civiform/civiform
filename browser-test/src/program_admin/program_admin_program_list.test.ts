import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  logout,
  validateScreenshot,
} from '../support'
import {ProgramVisibility} from '../support/admin_programs'

test.describe('Program admin program list', {tag: ['@northstar']}, () => {
  test('shows all the programs that are active', async ({
    page,
    adminPrograms,
  }) => {
    await test.step('log in as a CiviForm admin and publish multiple programs', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram('Program Name One')
      await adminPrograms.addProgram('Program Name Two')
      await adminPrograms.addProgram('Program Name Three')
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('log in as a program admin and view the program list, verify that all active programs are shown', async () => {
      await loginAsProgramAdmin(page)
      await expect(page.getByText('Program Name One')).toHaveCount(1)
      await expect(page.getByText('Program Name Two')).toHaveCount(1)
      await expect(page.getByText('Program Name Three')).toHaveCount(1)
      await validateScreenshot(page, 'program-admin-program-list')
    })
  })

  test('shows all the programs that are active, including the program with disabled visibility', async ({
    page,
    adminPrograms,
  }) => {
    await test.step('log in as a CiviForm admin and publish multiple programs', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(
        'Disabled Program Name',
        'Program Description',
        'Short Program Description',
        'https://usa.gov',
        ProgramVisibility.DISABLED,
      )
      await adminPrograms.addProgram('Program Name Two')
      await adminPrograms.addProgram('Program Name Three')
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('log in as a program admin and view the program list, verify that the disabled program is shown', async () => {
      await loginAsProgramAdmin(page)
      await expect(page.getByText('Disabled Program Name')).toHaveCount(1)
      await expect(page.getByText('Program Name Two')).toHaveCount(1)
      await expect(page.getByText('Program Name Three')).toHaveCount(1)

      await validateScreenshot(
        page,
        'program-admin-program-list-visible-disabled-program',
      )
    })
  })
})

test.describe('Translation tag showing as expected', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'translation_management_improvement_enabled')
  })

  const programName = 'Program for translation tags'

  test('Tag translation incomplete and complete shows up as expected', async ({
    page,
    adminPrograms,
    adminTranslations,
    adminProgramStatuses,
  }) => {
    await test.step('Tag translation incomplete is visible', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoAdminProgramsPage()
      await expect(page.getByText('Translation Incomplete')).toBeVisible()
      await expect(page.getByText('Translation Complete')).toBeHidden()
    })

    await test.step('Translate all fields available', async () => {
      await adminPrograms.gotoDraftProgramManageTranslationsPage(programName)
      const languages = [
        'Amharic',
        'Arabic',
        'Traditional Chinese',
        'French',
        'Japanese',
        'Korean',
        'Lao',
        'Russian',
        'Somali',
        'Spanish',
        'Tagalog',
        'Vietnamese',
      ]

      for (const language of languages) {
        await adminTranslations.selectLanguage(language)
        await adminTranslations.editProgramTranslations({
          name: `${language} name`,
          description: `${language} description`,
          blockName: `${language} block name`,
          blockDescription: `${language} block description`,
          statuses: [],
        })
      }
    })

    await test.step('Tag translation complete is visible', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await expect(page.getByText('Translation Complete')).toBeVisible()
      await expect(page.getByText('Translation Incomplete')).toBeHidden()
    })

    await test.step('Tag translation incomplete shows when a new field to the proram needs to be translated', async () => {
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus('testStatus')
      await adminPrograms.gotoAdminProgramsPage()
      await expect(page.getByText('Translation Incomplete')).toBeVisible()
      await expect(page.getByText('Translation Complete')).toBeHidden()
    })
  })
})
