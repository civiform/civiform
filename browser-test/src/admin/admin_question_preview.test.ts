import {test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('Admin question preview', () => {
  const questionName = 'test-question'

  test('Preview address question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addAddressQuestion({
      questionName: questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'address-question')
  })

  test('Preview checkbox question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addCheckboxQuestion({
      questionName: questionName,
      options: [
        {adminName: 'a', text: 'a'},
        {adminName: 'b', text: 'b'},
        {adminName: 'c', text: 'c'},
      ],
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'checkbox-question')
  })

  test('Preview currency question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addCurrencyQuestion({
      questionName: questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'currency-question')
  })

  test('Preview date question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addDateQuestion({
      questionName: questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'date-question')
  })

  test('Preview dropdown question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addDropdownQuestion({
      questionName: questionName,
      options: [
        {adminName: 'a', text: 'a'},
        {adminName: 'b', text: 'b'},
        {adminName: 'c', text: 'c'},
      ],
        })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'dropdown-question')
  })

  test('Preview email question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addEmailQuestion({
      questionName: questionName
        })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'email-question')
  })

  // TODO ssandbekkhaug do some extra manual testing
  test('Preview enumerator question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addEnumeratorQuestion({
      questionName:questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'enumerator-question')
  })

  // TODO(#7849): Support file upload question

  test('Preview ID question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addIdQuestion({
      questionName:questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'id-question')
  })

  test('Preview name question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addNameQuestion({
      questionName:questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'name-question')
  })

  test('Preview number question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addNumberQuestion({
      questionName:questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'number-question')
  })

  test('Preview radio button question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addRadioButtonQuestion({
      questionName:questionName,
      options: [
        {adminName: 'a', text: 'a'},
        {adminName: 'b', text: 'b'},
        {adminName: 'c', text: 'c'},
      ],
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'number-question')
  })

  test('Preview static text', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addStaticQuestion({
      questionName:questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'static-question')
  })

  test('Preview phone number question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addPhoneQuestion({
      questionName:questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'phone-question')
  })

  test('Preview text question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: questionName
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    await validateScreenshot(page, 'text-question')
  })
})
