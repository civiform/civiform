import {test, expect} from '../support/civiform_fixtures'
import {
  isHermeticTestEnvironment,
  loginAsAdmin,
  loginAsCiviformAndProgramAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  waitForPageJsLoad,
  validateScreenshot,
} from '../support'

test.describe('Program admin review of submitted applications', () => {
  test('all major steps with multiple file upload flag', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    test.slow()

    const programName = 'A shiny new program'

    await loginAsAdmin(page)

    await test.step('Create new questions', async () => {
      await adminQuestions.addDateQuestion({questionName: 'date-q'})
      await adminQuestions.addEmailQuestion({questionName: 'email-q'})
      await adminQuestions.addDropdownQuestion({
        questionName: 'ice-cream-q',
        options: [
          {adminName: 'chocolate_admin', text: 'chocolate'},
          {adminName: 'banana_admin', text: 'banana'},
          {adminName: 'black_raspberry_admin', text: 'black raspberry'},
        ],
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'favorite-trees-q',
        options: [
          {adminName: 'oak_admin', text: 'oak'},
          {adminName: 'maple_admin', text: 'maple'},
          {adminName: 'pine_admin', text: 'pine'},
          {adminName: 'cherry_admin', text: 'cherry'},
        ],
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'favorite-rats-q',
        options: [
          {adminName: 'sewage_admin', text: 'sewage'},
          {adminName: 'laboratory_admin', text: 'laboratory'},
          {adminName: 'bubonic_admin', text: 'bubonic'},
          {adminName: 'giant_admin', text: 'giant'},
        ],
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'scared-of-q',
        options: [
          {adminName: 'dog_admin', text: 'dogs'},
          {adminName: 'bee_admin', text: 'bees'},
          {adminName: 'spider_admin', text: 'spiders'},
          {adminName: 'dark_admin', text: 'the dark'},
          {adminName: 'clown_admin', text: 'clowns'},
        ],
      })
      await adminQuestions.addCurrencyQuestion({
        questionName: 'monthly-income-q',
      })
      await adminQuestions.addAddressQuestion({questionName: 'address-q'})
      await adminQuestions.addFileUploadQuestion({
        questionName: 'fileupload-q',
      })
      await adminQuestions.addNameQuestion({questionName: 'name-q'})
      await adminQuestions.addNumberQuestion({questionName: 'number-q'})
      await adminQuestions.addTextQuestion({questionName: 'text-q'})
      await adminQuestions.addRadioButtonQuestion({
        questionName: 'radio-q',
        options: [
          {adminName: 'one_admin', text: 'one'},
          {adminName: 'two_admin', text: 'two'},
          {adminName: 'three_admin', text: 'three'},
        ],
      })
      await adminQuestions.addStaticQuestion({questionName: 'first-static-q'})
      await adminQuestions.addStaticQuestion({
        questionName: 'second-static-q',
      })
    })

    await test.step('Create program', async () => {
      await adminPrograms.addProgram(programName)

      await adminPrograms.editProgramBlock(programName, 'block description', [
        'date-q',
        'address-q',
        'name-q',
        'radio-q',
        'email-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'another description', [
        'ice-cream-q',
        'favorite-trees-q',
        'number-q',
        'text-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'third description', [
        'fileupload-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'fourth description', [
        'scared-of-q',
        'favorite-rats-q',
        'first-static-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'fifth description', [
        'second-static-q',
        'monthly-income-q',
      ])

      // Intentionally add an empty block to ensure that empty blocks do not
      // prevent applicants from being able to submit applications.
      await adminPrograms.addProgramBlock(programName, 'empty block')
    })

    await test.step('Publish program', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.expectDraftProgram(programName)

      await adminPrograms.publishProgram(programName)
      await adminPrograms.expectActiveProgram(programName)
    })

    await test.step('Assert program', async () => {
      await adminQuestions.expectActiveQuestionExist('ice-cream-q')
      await adminQuestions.expectActiveQuestionExist('favorite-trees-q')
      await adminQuestions.expectActiveQuestionExist('favorite-rats-q')
      await adminQuestions.expectActiveQuestionExist('scared-of-q')
      await adminQuestions.expectActiveQuestionExist('address-q')
      await adminQuestions.expectActiveQuestionExist('name-q')
      await adminQuestions.expectActiveQuestionExist('date-q')
      await adminQuestions.expectActiveQuestionExist('number-q')
      await adminQuestions.expectActiveQuestionExist('text-q')
      await adminQuestions.expectActiveQuestionExist('radio-q')
      await adminQuestions.expectActiveQuestionExist('email-q')
      await adminQuestions.expectActiveQuestionExist('first-static-q')
      await adminQuestions.expectActiveQuestionExist('second-static-q')
      await adminQuestions.expectActiveQuestionExist('monthly-income-q')
    })

    await adminQuestions.goToViewQuestionPage('date-q')

    await test.step('Log in as test user', async () => {
      await logout(page)
      await loginAsTestUser(page)
    })

    await applicantQuestions.validateHeader('en-US')

    await test.step('Fill out first application block', async () => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '')
      await applicantQuestions.answerNameQuestion('', '', '')
      await applicantQuestions.answerRadioButtonQuestion('two')
      await applicantQuestions.answerDateQuestion('2021-05-10')
      await applicantQuestions.answerEmailQuestion('test1@gmail.com')
      await applicantQuestions.clickNext()

      // Application doesn't progress because of name and address question errors.
      // Verify that address error messages are visible.
      await expect(
        page.locator('.cf-address-street-1-error:visible'),
      ).toHaveText('Error: Please enter valid street name and number.')
      await expect(page.locator('.cf-address-city-error:visible')).toHaveText(
        'Error: Please enter city.',
      )
      await expect(page.locator('.cf-address-state-error:visible')).toHaveText(
        'Error: Please enter state.',
      )
      await expect(page.locator('.cf-address-zip-error:visible')).toHaveText(
        'Error: Please enter valid 5-digit ZIP code.',
      )
      await expect(page.locator('.cf-name-first-error:visible')).toHaveText(
        'Error: Please enter your first name.',
      )
      await expect(page.locator('.cf-name-last-error:visible')).toHaveText(
        'Error: Please enter your last name.',
      )

      // Fix the address and name questions and submit.
      await applicantQuestions.answerNameQuestion('Queen', 'Hearts', 'of')
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
      )

      await applicantQuestions.clickNext()
    })

    await test.step('Fill out second application block', async () => {
      await applicantQuestions.answerDropdownQuestion('banana')
      await applicantQuestions.answerCheckboxQuestion(['cherry', 'pine'])
      await applicantQuestions.answerNumberQuestion('42')
      await applicantQuestions.answerTextQuestion('some text')
      await applicantQuestions.clickNext()
    })

    await test.step('Fill out third application block', async () => {
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload.png',
      )
      await applicantQuestions.answerFileUploadQuestionFromAssets(
        'file-upload-second.png',
      )
      await applicantQuestions.clickNext()
    })

    await test.step('Fill out fourth application block', async () => {
      await applicantQuestions.answerCheckboxQuestion(['clowns'])
      await applicantQuestions.answerCheckboxQuestion(['sewage'])
      await applicantQuestions.seeStaticQuestion('static question text')
      await applicantQuestions.clickNext()
    })

    await test.step('Fill out fifth application block', async () => {
      await applicantQuestions.seeStaticQuestion('static question text')
      await applicantQuestions.answerCurrencyQuestion('1234.56')
      await applicantQuestions.clickNext()
    })

    await test.step('Submit application', async () => {
      await applicantQuestions.submitFromReviewPage()
    })

    await test.step('Log in as program admin', async () => {
      await logout(page)
      await loginAsProgramAdmin(page)
    })

    await test.step('View the submitted application', async () => {
      await adminPrograms.viewApplications(programName)
      await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    })

    await test.step('Review screen 1', async () => {
      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'address-q',
        '1234 St',
      )
      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'name-q',
        'Queen',
      )

      await adminPrograms.expectApplicationAnswers('Screen 1', 'radio-q', '2')
      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'date-q',
        '05/10/2021',
      )
      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'email-q',
        'test1@gmail.com',
      )
    })

    await test.step('Review screen 2', async () => {
      await adminPrograms.expectApplicationAnswers(
        'Screen 2',
        'ice-cream-q',
        '2',
      )
      await adminPrograms.expectApplicationAnswers(
        'Screen 2',
        'favorite-trees-q',
        'pine; cherry',
      )

      await adminPrograms.expectApplicationAnswers('Screen 2', 'number-q', '42')
      await adminPrograms.expectApplicationAnswers(
        'Screen 2',
        'text-q',
        'some text',
      )
    })

    await test.step('Review screen 3', async () => {
      await adminPrograms.expectApplicationAnswers(
        'Screen 3',
        'fileupload-q',
        'file-upload.png',
      )
      await adminPrograms.expectApplicationAnswers(
        'Screen 3',
        'fileupload-q',
        'file-upload-second.png',
      )
      await page.getByRole('link', {name: 'Back'}).click()
    })

    await test.step('Log in as civiform admin', async () => {
      await logout(page)
      await loginAsAdmin(page)
    })

    await test.step('Create new version and re-publish program', async () => {
      await adminQuestions.createNewVersion('favorite-trees-q')
      await adminQuestions.gotoQuestionEditPage('favorite-trees-q')

      await page.getByLabel('delete').nth(1).click()
      await page.getByRole('button', {name: 'Update'}).click()

      await adminPrograms.publishProgram(programName)
    })

    await test.step('Log in as program admin', async () => {
      await logout(page)
      await loginAsProgramAdmin(page)
    })

    await test.step('Review updated screen 2', async () => {
      await adminPrograms.viewApplications(programName)
      await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
      await adminPrograms.expectApplicationAnswers(
        'Screen 2',
        'favorite-trees-q',
        'pine; cherry',
      )

      await page.getByRole('link', {name: 'Back'}).click()
    })

    await test.step('Click CiviForm logo and navigate to the programs admins home page', async () => {
      // TODO: We need to add a better/accessible way of targeting this
      await page.click('text=CF')
      await waitForPageJsLoad(page)
    })

    await validateScreenshot(page, 'applications-page')

    await test.step('Go to reporting page', async () => {
      await page.getByRole('link', {name: 'Reporting'}).click()

      // The reporting page is not deterministic outside the hermetic testing environment
      // so don't validate the screenshot for it when running staging probers.
      if (isHermeticTestEnvironment()) {
        await validateScreenshot(page, 'reporting-page')
      }
    })

    await test.step('Go to reporting program details', async () => {
      await page.getByRole('link', {name: programName}).click()

      if (isHermeticTestEnvironment()) {
        await validateScreenshot(page, 'program-specific-reporting-page')
      }
    })

    await test.step('Log in as Civiform Admin', async () => {
      // Validate the views for CF and program admins.
      await logout(page)
      await loginAsCiviformAndProgramAdmin(page)
    })

    await test.step('Go to applications list page', async () => {
      await page.click(
        adminPrograms.withinProgramCardSelector(
          programName,
          'Active',
          '.cf-with-dropdown',
        ),
      )
      await page.click(
        adminPrograms.withinProgramCardSelector(
          programName,
          'ACTIVE',
          'button :text("Applications")',
        ),
      )
      await waitForPageJsLoad(page)
      await validateScreenshot(page, 'cf-admin-applications-page')
    })

    if (isHermeticTestEnvironment()) {
      await test.step('Go to reporting page', async () => {
        await page.getByRole('link', {name: 'Reporting'}).click()
        await waitForPageJsLoad(page)
        await validateScreenshot(page, 'cf-admin-reporting-page')
      })
    }
  })

  test('all major steps', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    test.slow()

    const programName = 'A shiny new program'

    await loginAsAdmin(page)

    await test.step('Create new questions', async () => {
      await adminQuestions.addDateQuestion({questionName: 'date-q'})
      await adminQuestions.addEmailQuestion({questionName: 'email-q'})
      await adminQuestions.addDropdownQuestion({
        questionName: 'ice-cream-q',
        options: [
          {adminName: 'chocolate_admin', text: 'chocolate'},
          {adminName: 'banana_admin', text: 'banana'},
          {adminName: 'black_raspberry_admin', text: 'black raspberry'},
        ],
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'favorite-trees-q',
        options: [
          {adminName: 'oak_admin', text: 'oak'},
          {adminName: 'maple_admin', text: 'maple'},
          {adminName: 'pine_admin', text: 'pine'},
          {adminName: 'cherry_admin', text: 'cherry'},
        ],
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'favorite-rats-q',
        options: [
          {adminName: 'sewage_admin', text: 'sewage'},
          {adminName: 'laboratory_admin', text: 'laboratory'},
          {adminName: 'bubonic_admin', text: 'bubonic'},
          {adminName: 'giant_admin', text: 'giant'},
        ],
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'scared-of-q',
        options: [
          {adminName: 'dog_admin', text: 'dogs'},
          {adminName: 'bee_admin', text: 'bees'},
          {adminName: 'spider_admin', text: 'spiders'},
          {adminName: 'dark_admin', text: 'the dark'},
          {adminName: 'clown_admin', text: 'clowns'},
        ],
      })
      await adminQuestions.addCurrencyQuestion({
        questionName: 'monthly-income-q',
      })
      await adminQuestions.addAddressQuestion({questionName: 'address-q'})
      await adminQuestions.addFileUploadQuestion({
        questionName: 'fileupload-q',
      })
      await adminQuestions.addNameQuestion({questionName: 'name-q'})
      await adminQuestions.addNumberQuestion({questionName: 'number-q'})
      await adminQuestions.addTextQuestion({questionName: 'text-q'})
      await adminQuestions.addRadioButtonQuestion({
        questionName: 'radio-q',
        options: [
          {adminName: 'one_admin', text: 'one'},
          {adminName: 'two_admin', text: 'two'},
          {adminName: 'three_admin', text: 'three'},
        ],
      })
      await adminQuestions.addStaticQuestion({questionName: 'first-static-q'})
      await adminQuestions.addStaticQuestion({
        questionName: 'second-static-q',
      })
    })

    await test.step('Create program', async () => {
      await adminPrograms.addProgram(programName)

      await adminPrograms.editProgramBlock(programName, 'block description', [
        'date-q',
        'address-q',
        'name-q',
        'radio-q',
        'email-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'another description', [
        'ice-cream-q',
        'favorite-trees-q',
        'number-q',
        'text-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'third description', [
        'fileupload-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'fourth description', [
        'scared-of-q',
        'favorite-rats-q',
        'first-static-q',
      ])
      await adminPrograms.addProgramBlock(programName, 'fifth description', [
        'second-static-q',
        'monthly-income-q',
      ])

      // Intentionally add an empty block to ensure that empty blocks do not
      // prevent applicants from being able to submit applications.
      await adminPrograms.addProgramBlock(programName, 'empty block')
    })

    await test.step('Publish program', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.expectDraftProgram(programName)

      await adminPrograms.publishProgram(programName)
      await adminPrograms.expectActiveProgram(programName)
    })

    await test.step('Assert program', async () => {
      await adminQuestions.expectActiveQuestionExist('ice-cream-q')
      await adminQuestions.expectActiveQuestionExist('favorite-trees-q')
      await adminQuestions.expectActiveQuestionExist('favorite-rats-q')
      await adminQuestions.expectActiveQuestionExist('scared-of-q')
      await adminQuestions.expectActiveQuestionExist('address-q')
      await adminQuestions.expectActiveQuestionExist('name-q')
      await adminQuestions.expectActiveQuestionExist('date-q')
      await adminQuestions.expectActiveQuestionExist('number-q')
      await adminQuestions.expectActiveQuestionExist('text-q')
      await adminQuestions.expectActiveQuestionExist('radio-q')
      await adminQuestions.expectActiveQuestionExist('email-q')
      await adminQuestions.expectActiveQuestionExist('first-static-q')
      await adminQuestions.expectActiveQuestionExist('second-static-q')
      await adminQuestions.expectActiveQuestionExist('monthly-income-q')
    })

    await adminQuestions.goToViewQuestionPage('date-q')

    await test.step('Log in as test user', async () => {
      await logout(page)
      await loginAsTestUser(page)
    })

    await applicantQuestions.validateHeader('en-US')

    await test.step('Fill out first application block', async () => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '')
      await applicantQuestions.answerNameQuestion('', '', '')
      await applicantQuestions.answerRadioButtonQuestion('two')
      await applicantQuestions.answerDateQuestion('2021-05-10')
      await applicantQuestions.answerEmailQuestion('test1@gmail.com')
      await applicantQuestions.clickNext()

      // Application doesn't progress because of name and address question errors.
      // Verify that address error messages are visible.
      await expect(
        page.locator('.cf-address-street-1-error:visible'),
      ).toHaveText('Error: Please enter valid street name and number.')
      await expect(page.locator('.cf-address-city-error:visible')).toHaveText(
        'Error: Please enter city.',
      )
      await expect(page.locator('.cf-address-state-error:visible')).toHaveText(
        'Error: Please enter state.',
      )
      await expect(page.locator('.cf-address-zip-error:visible')).toHaveText(
        'Error: Please enter valid 5-digit ZIP code.',
      )
      await expect(page.locator('.cf-name-first-error:visible')).toHaveText(
        'Error: Please enter your first name.',
      )
      await expect(page.locator('.cf-name-last-error:visible')).toHaveText(
        'Error: Please enter your last name.',
      )

      // Fix the address and name questions and submit.
      await applicantQuestions.answerNameQuestion('Queen', 'Hearts', 'of')
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
      )

      await applicantQuestions.clickNext()
    })

    await test.step('Fill out second application block', async () => {
      await applicantQuestions.answerDropdownQuestion('banana')
      await applicantQuestions.answerCheckboxQuestion(['cherry', 'pine'])
      await applicantQuestions.answerNumberQuestion('42')
      await applicantQuestions.answerTextQuestion('some text')
      await applicantQuestions.clickNext()
    })

    await test.step('Fill out third application block', async () => {
      await applicantQuestions.answerFileUploadQuestion('file key')
      await applicantQuestions.clickNext()
    })

    await test.step('Fill out fourth application block', async () => {
      await applicantQuestions.answerCheckboxQuestion(['clowns'])
      await applicantQuestions.answerCheckboxQuestion(['sewage'])
      await applicantQuestions.seeStaticQuestion('static question text')
      await applicantQuestions.clickNext()
    })

    await test.step('Fill out fifth application block', async () => {
      await applicantQuestions.seeStaticQuestion('static question text')
      await applicantQuestions.answerCurrencyQuestion('1234.56')
      await applicantQuestions.clickNext()
    })

    await test.step('Submit application', async () => {
      await applicantQuestions.submitFromReviewPage()
    })

    await test.step('Log in as program admin', async () => {
      await logout(page)
      await loginAsProgramAdmin(page)
    })

    await test.step('View the submitted application', async () => {
      await adminPrograms.viewApplications(programName)
      await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    })

    await test.step('Review screen 1', async () => {
      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'address-q',
        '1234 St',
      )
      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'name-q',
        'Queen',
      )

      await adminPrograms.expectApplicationAnswers('Screen 1', 'radio-q', '2')
      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'date-q',
        '05/10/2021',
      )
      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'email-q',
        'test1@gmail.com',
      )
    })

    await test.step('Review screen 2', async () => {
      await adminPrograms.expectApplicationAnswers(
        'Screen 2',
        'ice-cream-q',
        '2',
      )
      await adminPrograms.expectApplicationAnswers(
        'Screen 2',
        'favorite-trees-q',
        'pine; cherry',
      )

      await adminPrograms.expectApplicationAnswers('Screen 2', 'number-q', '42')
      await adminPrograms.expectApplicationAnswers(
        'Screen 2',
        'text-q',
        'some text',
      )
    })

    await test.step('Review screen 3', async () => {
      await adminPrograms.expectApplicationAnswerLinks(
        'Screen 3',
        'fileupload-q',
      )
      await page.getByRole('link', {name: 'Back'}).click()
    })

    await test.step('Log in as civiform admin', async () => {
      await logout(page)
      await loginAsAdmin(page)
    })

    await test.step('Create new version and re-publish program', async () => {
      await adminQuestions.createNewVersion('favorite-trees-q')
      await adminQuestions.gotoQuestionEditPage('favorite-trees-q')

      await page.getByLabel('delete').nth(1).click()
      await page.getByRole('button', {name: 'Update'}).click()

      await adminPrograms.publishProgram(programName)
    })

    await test.step('Log in as program admin', async () => {
      await logout(page)
      await loginAsProgramAdmin(page)
    })

    await test.step('Review updated screen 2', async () => {
      await adminPrograms.viewApplications(programName)
      await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
      await adminPrograms.expectApplicationAnswers(
        'Screen 2',
        'favorite-trees-q',
        'pine; cherry',
      )
      await page.getByRole('link', {name: 'Back'}).click()
    })

    await test.step('Click CiviForm logo and navigate to the programs admins home page', async () => {
      // TODO: We need to add a better/accessible way of targeting this
      await page.click('text=CF')
      await waitForPageJsLoad(page)
    })

    await validateScreenshot(page, 'applications-page')

    await test.step('Go to reporting page', async () => {
      await page.getByRole('link', {name: 'Reporting'}).click()

      // The reporting page is not deterministic outside the hermetic testing environment
      // so don't validate the screenshot for it when running staging probers.
      if (isHermeticTestEnvironment()) {
        await validateScreenshot(page, 'reporting-page')
      }
    })

    await test.step('Go to reporting program details', async () => {
      await page.getByRole('link', {name: programName}).click()

      if (isHermeticTestEnvironment()) {
        await validateScreenshot(page, 'program-specific-reporting-page')
      }
    })

    await test.step('Log in as Civiform Admin', async () => {
      // Validate the views for CF and program admins.
      await logout(page)
      await loginAsCiviformAndProgramAdmin(page)
    })

    await test.step('Go to applications list page', async () => {
      await page.click(
        adminPrograms.withinProgramCardSelector(
          programName,
          'Active',
          '.cf-with-dropdown',
        ),
      )
      await page.click(
        adminPrograms.withinProgramCardSelector(
          programName,
          'ACTIVE',
          'button :text("Applications")',
        ),
      )
      await waitForPageJsLoad(page)
      await validateScreenshot(page, 'cf-admin-applications-page')
    })

    if (isHermeticTestEnvironment()) {
      await test.step('Go to reporting page', async () => {
        await page.getByRole('link', {name: 'Reporting'}).click()
        await waitForPageJsLoad(page)
        await validateScreenshot(page, 'cf-admin-reporting-page')
      })
    }
  })

  test('program applications listed most recent first', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    const programName = 'Fruit program'
    const answers = ['apple', 'banana', 'cherry', 'durian']

    await test.step('Create a simple one question program application', async () => {
      await loginAsAdmin(page)

      await adminQuestions.addTextQuestion({questionName: 'fruit-text-q'})
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['fruit-text-q'],
        programName,
      )

      await logout(page)
    })

    await test.step('Submit applications from different users', async () => {
      for (const answer of answers) {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerTextQuestion(answer)
        await applicantQuestions.clickNext()
        await applicantQuestions.submitFromReviewPage()

        await logout(page)
      }
    })

    await test.step('Expect applications to be presented in reverse chronological order to program admin', async () => {
      await loginAsProgramAdmin(page)

      await adminPrograms.viewApplications(programName)

      for (let i = 0; i < answers.length; i++) {
        await page.click(
          `:nth-match(.cf-admin-application-row, ${i + 1}) a:text("Guest")`,
        )
        await waitForPageJsLoad(page)
        await adminPrograms.expectApplicationAnswers(
          'Screen 1',
          'fruit-text-q',
          answers[answers.length - i - 1],
        )
        await page.getByRole('link', {name: 'Back'}).click()
      }
    })
  })

  test('program application filters cleared', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    const noApplyFilters = false
    const applyFilters = true
    const programName = 'Test program'

    await test.step('Add and publish program as Civiform Admin', async () => {
      await loginAsAdmin(page)

      await adminQuestions.addNameQuestion({
        questionName: 'Sample Name Question',
        universal: true,
        primaryApplicantInfo: true,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['Sample Name Question'],
        programName,
      )

      await logout(page)
    })

    await test.step('Submit an application as Test User', async () => {
      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(programName)

      // Applicant fills out first application block.
      await applicantQuestions.answerNameQuestion('sarah', 'smith')
      await applicantQuestions.clickNext()

      // Applicant submits answers from review page.
      await applicantQuestions.submitFromReviewPage()

      await logout(page)
    })

    await test.step('View application as Program Admin', async () => {
      await loginAsProgramAdmin(page)

      await adminPrograms.viewApplications(programName)
      const csvContent = await adminPrograms.getCsv(noApplyFilters)
      expect(csvContent).toContain('sarah,,smith')

      await logout(page)
    })

    await test.step('Apply to the program as as a Guest User', async () => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Gus', 'Guest')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()
      await applicantQuestions.returnToProgramsFromSubmissionPage()
    })

    await test.step('View application as Program Admin again', async () => {
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programName)
      const postEditCsvContent = await adminPrograms.getCsv(noApplyFilters)
      expect(postEditCsvContent).toContain('sarah,,smith')
      expect(postEditCsvContent).toContain('Gus,,Guest')
    })

    await test.step('Finds a partial text match on applicant name, case insensitive', async () => {
      await adminPrograms.filterProgramApplications({searchFragment: 'SARA'})
      const filteredCsvContent = await adminPrograms.getCsv(applyFilters)
      expect(filteredCsvContent).toContain('sarah,,smith')
      expect(filteredCsvContent).not.toContain('Gus,,Guest')
      await validateScreenshot(page, 'applications-filtered')
    })

    await test.step('Clear filters', async () => {
      await adminPrograms.clearFilterProgramApplications()
      const unfilteredCsvContent = await adminPrograms.getCsv(applyFilters)
      expect(unfilteredCsvContent).toContain('sarah,,smith')
      expect(unfilteredCsvContent).toContain('Gus,,Guest')
      await validateScreenshot(page, 'applications-unfiltered')
    })

    await test.step('Applies filters to download even when filter is not clicked', async () => {
      await adminPrograms.filterProgramApplications({
        searchFragment: 'SARA',
        clickFilterButton: false,
      })
      const filteredCsvContent = await adminPrograms.getCsv(applyFilters)
      expect(filteredCsvContent).toContain('sarah,,smith')
      expect(filteredCsvContent).not.toContain('Gus,,Guest')
      await validateScreenshot(page, 'applications-filtered-after-download')
    })
  })

  test('Application search using Personal Applicant Info works', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    const applyFilters = true
    const programName = 'Test program'

    await test.step('Login as an admin and create a program with three PAI questions', async () => {
      await loginAsAdmin(page)

      await adminQuestions.addNameQuestion({
        questionName: 'Name',
        universal: true,
        primaryApplicantInfo: true,
      })

      await adminQuestions.addEmailQuestion({
        questionName: 'Email',
        universal: true,
        primaryApplicantInfo: true,
      })

      await adminQuestions.addPhoneQuestion({
        questionName: 'Phone',
        universal: true,
        primaryApplicantInfo: true,
      })

      await adminPrograms.addAndPublishProgramWithQuestions(
        ['Name', 'Email', 'Phone'],
        programName,
      )

      await logout(page)
    })

    await test.step('Apply to the program as three different applicants so we have three applications to search', async () => {
      await applicantQuestions.completeApplicationWithPaiQuestions(
        programName,
        'oneFirst',
        'oneMiddle',
        'oneLast',
        'one@email.com',
        '4152321234',
      )
      await applicantQuestions.completeApplicationWithPaiQuestions(
        programName,
        'twoFirst',
        'twoMiddle',
        'twoLast',
        'two@email.com',
        '4153231234',
      )
      await applicantQuestions.completeApplicationWithPaiQuestions(
        programName,
        'threeFirst',
        'threeMiddle',
        'threeLast',
        'three@email.com',
        '5102321234',
      )
    })

    await test.step('Login as a Program Admin to search the applications', async () => {
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programName)
      await validateScreenshot(page, 'applications-unfiltered-pai')
    })

    await test.step('Search by name and validate expected applications are returned', async () => {
      await adminPrograms.filterProgramApplications({searchFragment: 'one'})
      const csvContentNameSearch = await adminPrograms.getCsv(applyFilters)
      expect(csvContentNameSearch).toContain('oneFirst')
      expect(csvContentNameSearch).not.toContain('twoFirst')
      expect(csvContentNameSearch).not.toContain('threeFirst')
    })

    await test.step('Search by email and validate expected applications are returned', async () => {
      await adminPrograms.filterProgramApplications({searchFragment: 'email'})
      const csvContentEmailSearch = await adminPrograms.getCsv(applyFilters)
      expect(csvContentEmailSearch).toContain('one@email.com')
      expect(csvContentEmailSearch).toContain('two@email.com')
      expect(csvContentEmailSearch).toContain('three@email.com')
    })

    await test.step('Search by phone and validate expected applications are returned', async () => {
      await adminPrograms.filterProgramApplications({searchFragment: '415'})
      const csvContentPhoneSearch = await adminPrograms.getCsv(applyFilters)
      expect(csvContentPhoneSearch).toContain('oneFirst')
      expect(csvContentPhoneSearch).toContain('twoFirst')
      expect(csvContentPhoneSearch).not.toContain('threeFirst')
    })

    // Creating a range of a couple days to test the filter. The localtime used in the UI vs the
    // UTC time used on the server is difficult to mock correctly without getting too hacky.
    // This will good enough to make sure the filters work, even if they don't check exact
    // bounds of just today.
    await test.step('Search by date and validate expected applications are returned', async () => {
      await adminPrograms.filterProgramApplications({
        fromDate: formattedToday(-1),
        untilDate: formattedToday(1),
      })
      const csvContentPhoneSearch = await adminPrograms.getCsv(applyFilters)
      expect(csvContentPhoneSearch).toContain('oneFirst')
      expect(csvContentPhoneSearch).toContain('twoFirst')
      expect(csvContentPhoneSearch).toContain('threeFirst')
    })
  })
})

/** Returns a date for now in UTC with the format of "yyyy-mm-dd". */
function formattedToday(addDays: number) {
  const now = new Date()
  now.setDate(now.getDate() + addDays)
  const isoString = now.toISOString() // UTC time zone. Example: "2024-05-28T16:34:25.863Z"
  return isoString.substring(0, 10)
}
