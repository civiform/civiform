import axe = require('axe-core')
import {toMatchImageSnapshot} from 'jest-image-snapshot'

interface CustomMatchers<R = unknown> {
  toHaveNoA11yViolations(): R
}

declare global {
  namespace jest {
    interface Expect extends CustomMatchers {}
    interface Matchers<R> extends CustomMatchers<R> {}
    interface InverseAsymmetricMatchers extends CustomMatchers {}
  }
}

// Custom matcher that outputs accessibility violations using axe.
// See https://jestjs.io/docs/expect#expectextendmatchers and
// https://jestjs.io/docs/configuration#setupfilesafterenv-array for more info on custom matchers in jest.
expect.extend({
  toHaveNoA11yViolations(results: axe.AxeResults) {
    const numViolations = results.violations.length
    if (numViolations == 0) {
      return {
        message: () => 'Expected axe accessibility violations, found none',
        pass: true,
      }
    } else {
      return {
        message: () =>
          `Expected no axe accessibility violations, found ${numViolations} violations:\n ${JSON.stringify(
            results.violations,
            null,
            2,
          )}`,
        pass: false,
      }
    }
  },
})
expect.extend({toMatchImageSnapshot})
