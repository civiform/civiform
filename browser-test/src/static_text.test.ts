import {Page} from 'playwright'
// import axe = require('axe-core')
import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  startSession,
  resetSession,
} from './support'

// //  https://jestjs.io/docs/expect
// interface CustomMatchers<R = unknown> {
//   toHaveNoViolations(): R
// }

// declare global {
//   namespace jest {
//     interface Expect extends CustomMatchers {}
//     interface Matchers<R> extends CustomMatchers<R> {}
//     interface InverseAsymmetricMatchers extends CustomMatchers {}
//   }
// }

// // Print out all accessibility violations, and link to how to fix?
// expect.extend({
//   toHaveNoViolations(results: axe.AxeResults) {
//     const numViolations = results.violations.length
//     const pass = numViolations == 0
//     if (pass) {
//       return {
//         message: () => `Expected axe accessibility violations, found none`,
//         pass: true,
//       }
//     } else {
//       return {
//         message: () =>
//           `Expected no axe accessibility violations, found ${numViolations} violations:\n ${JSON.stringify(
//             results.violations,
//           )}`,
//         pass: false,
//       }
//     }
//   },
// })

describe('Static text question for applicant flow', () => {
  const staticText = 'Hello, I am some static text!'
  const programName = 'test program for static text'
  let pageObject: Page
  let applicantQuestions: ApplicantQuestions

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    // As admin, create program with static text question.
    await loginAsAdmin(pageObject)
    const adminQuestions = new AdminQuestions(pageObject)
    const adminPrograms = new AdminPrograms(pageObject)
    applicantQuestions = new ApplicantQuestions(pageObject)

    await adminQuestions.addStaticQuestion({
      questionName: 'static-text-q',
      questionText: staticText,
    })
    // Must add an answerable question for text to show.
    await adminQuestions.addEmailQuestion({questionName: 'partner-email-q'})
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['static-text-q', 'partner-email-q'],
      programName,
    )

    await logout(pageObject)
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  it('displays static text', async () => {
    await loginAsGuest(pageObject)
    await selectApplicantLanguage(pageObject, 'English')

    await applicantQuestions.applyProgram(programName)

    const staticId = '.cf-question-static'
    expect(await pageObject.innerText(staticId)).toContain(staticText)
  })

  it('has no accessiblity violations', async () => {
    await loginAsGuest(pageObject)
    await selectApplicantLanguage(pageObject, 'English')

    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.validateAccessibility()

    // Wrap this in a function?

    // // Inject axe and run.
    // await pageObject.addScriptTag({path: 'node_modules/axe-core/axe.min.js'})
    // const results = await pageObject.evaluate(() => {
    //   return axe.run()
    // })

    // // expect.extend({
    // //   jeannie(received: number) {
    // //     if (received == 2) {
    // //       return {
    // //         message: () =>
    // //           `Expected axe accessibility violdations, found none`,
    // //         pass: true,
    // //       };
    // //     } else {
    // //       return {
    // //         message: () =>
    // //           `Expected no axe accessibility violdations, found ${received} violations`,
    // //         pass: false,
    // //       };
    // //     }
    // //   },
    // // });
    // // expect(0).jeannie();
    // console.log(JSON.stringify(results.violations, null, 2))

    // expect(results).toHaveNoViolations()
  })
})
