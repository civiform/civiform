import { startSession, loginAsAdmin, AdminQuestions } from './support'

describe('create dropdown question with options', () => {
  it('add remove buttons work correctly', async () => {
    const { page } = await startSession()

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    await page.click('#create-question-button')

    await page.click('#create-dropdown-question')

    // Add three options
    await page.click('#add-new-option')
    await page.fill('text=Question option', 'chocolate')
    await page.click('#add-new-option')
    await page.fill('text=Question option', 'vanilla')
    await page.click('#add-new-option')
    await page.fill('text=Question option', 'strawberry')
    // Remove first option
    await page.click('text=Remove')

    // Assert there are only two options now
    const questionSettingsDiv = await page.innerHTML('#question-settings')

    expect(questionSettingsDiv.match('<input/g').length).toBe(2)
  })
})