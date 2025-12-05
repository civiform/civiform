import {Page} from 'playwright'
import {test, expect} from './support/civiform_fixtures'
import {loginAsAdmin} from './support'
import {loginAsProgramAdmin} from './support'
import {loginAsTrustedIntermediary} from './support'
import {logout} from './support'

async function expectAdminDashboard(page: Page) {
  await expect(
    page.getByRole('heading', {name: 'Program dashboard'}),
  ).toBeAttached()
  await expect(
    page.getByRole('heading', {name: 'Create, edit and publish programs'}),
  ).toBeAttached()
}

async function expectProgramAdminDashboard(page: Page) {
  await expect(
    page.getByRole('heading', {name: 'Your programs', exact: true}),
  ).toBeAttached()
}

async function expectTiDashboard(page: Page) {
  await expect(
    page.getByRole('heading', {name: 'All clients', exact: true}),
  ).toBeAttached()
}

test.describe('applicant security', {tag: ['@parallel-candidate']}, () => {
  test('applicant cannot access admin pages', async ({request}) => {
    const response = await request.get('/admin/programs')
    await expect(response).toBeOK()
    // Redirected to a non-admin page
    expect(response.url()).not.toContain('/admin')
  })

  test('redirects to program index page when not logged in (guest)', async ({
    page,
  }) => {
    await page.goto('/')
    await expect(
      page.getByRole('heading', {
        name: 'Apply for government programs online',
      }),
    ).toBeAttached()
  })
})

test.describe('non applicant security', {tag: ['@parallel-candidate']}, () => {
  const programName = 'Test program 1'

  test.beforeEach('Setup program', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programName)

    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.publishProgram(programName)
    await logout(page)
  })

  test('/ redirects to CiviForm admin dashboard when logged in as CiviForm admin', async ({
    page,
  }) => {
    await loginAsAdmin(page)
    await page.goto('/')

    await expectAdminDashboard(page)
  })

  test('/programs redirects to CiviForm admin dashboard when logged in as CiviForm admin', async ({
    page,
  }) => {
    await loginAsAdmin(page)
    await page.goto('/programs')

    await expectAdminDashboard(page)
  })

  test('program deeplink redirects to CiviForm admin dashboard when logged in as CiviForm admin', async ({
    page,
  }) => {
    await loginAsAdmin(page)
    await page.goto('/programs/' + programName)

    await expectAdminDashboard(page)
  })

  test('/ redirects to program admin dashboard when logged in as Program admin', async ({
    page,
  }) => {
    await loginAsProgramAdmin(page)
    await page.goto('/')

    await expectProgramAdminDashboard(page)
  })

  test('/programs redirects to program admin dashboard when logged in as Program admin', async ({
    page,
  }) => {
    await loginAsProgramAdmin(page)
    await page.goto('/programs')

    await expectProgramAdminDashboard(page)
  })

  test('program deeplink redirects to program admin dashboard when logged in as Program admin', async ({
    page,
  }) => {
    await loginAsProgramAdmin(page)
    await page.goto('/programs/' + programName)

    await expectProgramAdminDashboard(page)
  })

  test('/ redirects to TI dashboard when logged in as TI', async ({page}) => {
    await loginAsTrustedIntermediary(page)
    await page.goto('/')

    await expectTiDashboard(page)
  })

  test('/programs redirects to TI dashboard when logged in as TI', async ({
    page,
  }) => {
    await loginAsTrustedIntermediary(page)
    await page.goto('/programs')

    await expectTiDashboard(page)
  })

  test('program deeplink redirects to TI dashboard when logged in as TI', async ({
    page,
  }) => {
    await loginAsTrustedIntermediary(page)
    await page.goto('/programs/' + programName)

    await expectTiDashboard(page)
  })
})
