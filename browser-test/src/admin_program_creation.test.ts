import { startSession, loginAsAdmin, AdminQuestions } from './support'

describe('create dropdown question with options', () => {
  it('add remove buttons work correctly', async () => {
    const { page } = await startSession()

    await loginAsAdmin(page)

    await page.click('text=Questions')
    await page.click('#create-question-button')
    await page.click('#create-dropdown-question')

    // Add three options
    await page.click('#add-new-option')
    await page.fill('input:above(#add-new-option)', 'chocolate')
    await page.click('#add-new-option')
    await page.fill('input:above(#add-new-option)', 'vanilla')
    await page.click('#add-new-option')
    await page.fill('input:above(#add-new-option)', 'strawberry')

    // Assert there are three options present
    var questionSettingsDiv = await page.innerHTML('#question-settings')
    expect(questionSettingsDiv.match(/<input/g).length).toBe(3)

    // Remove first option - use :visible to not select the hidden template
    await page.click('button:text("Remove"):visible')

    // Assert there are only two options now
    questionSettingsDiv = await page.innerHTML('#question-settings')
    expect(questionSettingsDiv.match(/<input/g).length).toBe(2)

    // TODO(https://github.com/seattle-uat/civiform/issues/631):
    // Assert options are written correctly and appear once form is submitted
  })
})

