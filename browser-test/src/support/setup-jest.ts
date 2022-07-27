import axe = require('axe-core')

interface CustomMatchers<R = unknown> {
  toHaveNoViolations(): R
}

declare global {
  namespace jest {
    interface Expect extends CustomMatchers {}
    interface Matchers<R> extends CustomMatchers<R> {}
    interface InverseAsymmetricMatchers extends CustomMatchers {}
  }
}

// Custom matcher that outputs accessibility violations using axe.
expect.extend({
  toHaveNoViolations(results: axe.AxeResults) {
    const numViolations = results.violations.length
    const pass = numViolations == 0
    if (pass) {
      return {
        message: () => `Expected axe accessibility violations, found none`,
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
