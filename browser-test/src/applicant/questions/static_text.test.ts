import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {SAMPLE_QUESTIONS, Seeding} from '../../support/seeding'

const staticText = 'Hello, I am some static text!'
const markdownText =
  '\n[This is a link](https://www.example.com)\n' +
  'This is a list:\n' +
  '* Item 1\n' +
  '* Item 2\n' +
  '\n' +
  'There are some empty lines below this that should be preserved\n' +
  '\n' +
  '\n' +
  'This link should be autodetected: https://www.example.com\n' +
  '__Last line of content should be bold__'
const programName = 'Test program for static text'

test.describe('Static text question for applicant flow', () => {
  test.beforeEach(async ({page, adminQuestions, adminPrograms, seeding}) => {
    await setUpForSingleQuestion(
      programName,
      page,
      seeding,
      adminQuestions,
      adminPrograms,
    )
  })

  test('parses markdown', async ({page, applicantQuestions}) => {
    await applicantQuestions.applyProgram(programName)
    await validateScreenshot(
      page.getByTestId('staticQuestionRoot'),
      'markdown-text',
      {fullPage: false},
    )
    await verifyMarkdownHtml(page)
  })

  test('has no accessiblity violations', async ({page, applicantQuestions}) => {
    await applicantQuestions.applyProgram(programName)
    await validateAccessibility(page)
  })

  test.describe('with question image feature flag enabled', () => {
    const programWithImage = 'Program with Static Image'
    const imageQuestionName = 'static-img-q'
    const imageDescription = 'Sample tax form example'

    test.beforeEach(
      async ({page, adminQuestions, adminQuestionImage, adminPrograms}) => {
        await enableFeatureFlag(
          page,
          'ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED',
        )
        await enableFeatureFlag(page, 'IMAGES_IN_QUESTION_FEATURE_ENABLED')

        await loginAsAdmin(page)
        await adminQuestions.addStaticQuestion({
          questionName: imageQuestionName,
          questionText: 'Please review this image',
          markdownText: 'Some markdown info',
        })
        await adminQuestions.gotoQuestionEditPage(imageQuestionName)
        await adminQuestionImage.setImageDescription(imageDescription)
        await adminQuestionImage.uploadImageFile(
          'src/assets/program-summary-image-wide.png',
        )
        await adminQuestions.clickSubmitButtonAndNavigate('Update')
        await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
        await adminPrograms.addAndPublishProgramWithQuestions(
          [imageQuestionName, SAMPLE_QUESTIONS.email],
          programWithImage,
        )
        await logout(page)
      },
    )

    test('does not render image when question has no image', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await expect(page.getByTestId('question-image-container')).toHaveCount(0)
      await expect(
        page.getByTestId('staticQuestionRoot').getByRole('img'),
      ).toHaveCount(0)
    })

    test('renders inline image for applicant when question has an image', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Visit the program as an applicant and land on the screen containing the static question', async () => {
        await applicantQuestions.applyProgram(programWithImage)
      })

      await test.step('Verify the image', async () => {
        const imageContainer = page.getByTestId('question-image-container')
        await expect(imageContainer).toBeVisible()

        const img = imageContainer.getByRole('img', {name: imageDescription})
        await expect(img).toBeVisible()

        const imageLink = imageContainer.getByRole('link', {
          name: new RegExp(`${imageDescription}.*Enlarge image`, 'i'),
        })
        await expect(imageLink).toBeVisible()
        await expect(imageLink).not.toHaveAttribute('aria-label')
        await expect(imageLink).toContainText('Enlarge image')
        await expect(imageLink).toHaveAttribute('target', '_blank')
        await expect(imageContainer.getByRole('link')).toHaveCount(1)

        await validateAccessibility(page)
      })
    })

    test('renders translated alt text for applicant in another language', async ({
      page,
      adminQuestions,
      adminTranslations,
      applicantQuestions,
    }) => {
      const spanishAltText = 'Ejemplo de formulario de impuestos'

      await test.step('Log in as admin and add Spanish translation for the question image alt text', async () => {
        await loginAsAdmin(page)
        await adminQuestions.goToQuestionTranslationPage(imageQuestionName)
        await adminTranslations.selectLanguage('Spanish')
        await adminTranslations.editQuestionImageDescription(spanishAltText)
        await logout(page)
      })

      await test.step('Visit the program as an applicant in Spanish', async () => {
        await applicantQuestions.applyProgram(programWithImage)
        await selectApplicantLanguage(page, 'es-US')
      })

      await test.step('Verify the image HTML contains the translated alt text', async () => {
        const imageContainer = page.getByTestId('question-image-container')
        await expect(imageContainer).toBeVisible()

        const img = imageContainer.getByRole('img', {name: spanishAltText})
        await expect(img).toBeVisible()
        await expect(img).toHaveAttribute('alt', spanishAltText)

        expect(await imageContainer.innerHTML()).toContain(
          `alt="${spanishAltText}"`,
        )

        const imageLink = imageContainer.getByRole('link', {
          name: new RegExp(`${spanishAltText}.*`, 'i'),
        })
        await expect(imageLink).toBeVisible()
      })
    })

    test('does not render image when feature flag is disabled', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Disable the flag- IMAGES_IN_QUESTION_FEATURE_ENABLED', async () => {
        await disableFeatureFlag(page, 'IMAGES_IN_QUESTION_FEATURE_ENABLED')
      })

      await test.step('Visit the program as an applicant and land on the screen containing the static question', async () => {
        await applicantQuestions.applyProgram(programWithImage)
      })

      await test.step('Verify the image', async () => {
        await expect(page.getByTestId('question-image-container')).toHaveCount(
          0,
        )
        await expect(
          page.getByTestId('staticQuestionRoot').getByRole('img'),
        ).toHaveCount(0)
      })
    })
  })
})

async function setUpForSingleQuestion(
  programName: string,
  page: Page,
  seeding: Seeding,
  adminQuestions: AdminQuestions,
  adminPrograms: AdminPrograms,
) {
  await seeding.seedQuestions()
  // As admin, create program with static text question.
  await loginAsAdmin(page)
  await adminQuestions.addStaticQuestion({
    questionName: 'static-text-q',
    questionText: staticText,
    markdownText: markdownText,
  })
  // Must add an answerable question for text to show.
  await adminPrograms.addAndPublishProgramWithQuestions(
    ['static-text-q', SAMPLE_QUESTIONS.email],
    programName,
  )
  await logout(page)
}

async function verifyMarkdownHtml(page: Page) {
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<p>Hello, I am some static text!<br>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<a href="https://www.example.com" class="usa-link usa-link--external" target="_blank" aria-label="This is a link, opens in a new tab" rel="nofollow noopener noreferrer">This is a link</a>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<ul class="usa-list margin-r-4"><li>Item 1</li><li>Item 2<br>&nbsp;</li></ul>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<p>There are some empty lines below this that should be preserved<br>&nbsp;</p>\n<p>&nbsp;</p>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<p>This link should be autodetected: <a href="https://www.example.com" class="usa-link usa-link--external" target="_blank" aria-label="https://www.example.com, opens in a new tab" rel="nofollow noopener noreferrer">https://www.example.com</a>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<strong>Last line of content should be bold</strong>',
  )
}
