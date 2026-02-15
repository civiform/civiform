/* eslint-disable @typescript-eslint/no-unsafe-argument */
/* eslint-disable @typescript-eslint/no-unsafe-assignment */
/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-unsafe-call */
/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-require-imports */
import fs = require('fs')
// import path = require('path')
// import sharp = require('sharp')
import type {Reporter} from '@playwright/test/reporter'

/**
 * A custom playwright reporter that runs after all reporters have ended their processing. It
 * handles moving files to the `diff_output` and `updated_snapshots` folders.
 */
class RunHeuristicsReporter implements Reporter {
  printsToStdio(): boolean {
    return true
  }

  /**
   * Called immediately before test runner exists. At this point all the reporters have received the reporter.onEnd()
   * signal, so all the reports should be built. You can run the code that uploads the reports in this hook.
   * See https://playwright.dev/docs/api/class-reporter#reporter-on-exit
   *
   * @override
   */
  // eslint-disable-next-line @typescript-eslint/require-await
  async onExit() {
    this.populateImages()
  }

  /**
   * Read the json report and kickstart the process of handling the output
   */
  populateImages() {
    const jsonObj: PlaywrightReport = JSON.parse(
      fs.readFileSync('./tmp/json-output/results.json', 'utf8'),
    )

    const runDurationTable: RunDurationTable = this.walkSuites(
      jsonObj.suites,
      {},
    )

    fs.mkdirSync('./tmp/run-heuristics', {recursive: true})

    fs.writeFileSync(
      './tmp/run-heuristics/run-heuristics-report.json',
      JSON.stringify(runDurationTable, null, 2),
      'utf-8',
    )
  }

  /**
   * Recurse through nested suites arrays to get to the specs
   * @param suites An array of elements from a suites property from the playwright json report output
   */
  walkSuites(
    suites: Suite[],
    resultHashTable: RunDurationTable,
  ): RunDurationTable {
    for (const suite of suites) {
      if (suite.suites !== undefined) {
        this.walkSuites(suite.suites, resultHashTable)
      }

      const key = 'src/' + suite.file
      if (resultHashTable[key] === undefined) {
        resultHashTable[key] = 0
      }

      resultHashTable[key] += this.processSpecs(suite.specs)
    }

    return resultHashTable
  }

  /**
   * Iterate over specs and for ones from failed tests place updated and diff images in desired folders
   * @param specs An array of elements from a specs property from the playwright json report output
   */
  processSpecs(specs: Spec[]): number {
    let runningDuration = 0

    for (const spec of specs) {
      for (const test of spec.tests) {
        for (const result of test.results) {
          runningDuration += result.duration
        }
      }
    }

    return runningDuration
  }

  // /**
  //  *
  //  */
  // partitionFilesBySizeInOrder(
  //   resultHashTable: ResultHashTable,
  //   numBuckets: number,
  // ): Bucket[] {
  //   const filesWithDurations = Object.entries(resultHashTable).map(
  //     ([file, duration]) => ({
  //       path: file,
  //       duration: duration,
  //     }),
  //   )

  //   // Sort by descending size
  //   filesWithDurations.sort((a, b) => b.duration - a.duration)

  //   // Initialize buckets
  //   const buckets: Bucket[] = Array.from({length: numBuckets}, () => ({
  //     duration: 0,
  //     files: [],
  //   }))

  //   // Distribute files
  //   for (const file of filesWithDurations) {
  //     // Find bucket with the smallest current sum
  //     let bestBucket = buckets[0]
  //     for (const bucket of buckets) {
  //       if (bucket.duration < bestBucket.duration) {
  //         bestBucket = bucket
  //       }
  //     }

  //     bestBucket.files.push(file.path)
  //     bestBucket.duration += file.duration
  //   }

  //   return buckets
  // }
}

/**
 * TypeScript interfaces for Playwright Test Report
 */

// Main report structure
interface PlaywrightReport {
  suites: Suite[]
}

interface Suite {
  file: string
  specs: Spec[]
  suites: Suite[]
}

interface Spec {
  ok: boolean
  tests: Test[]
}

interface Test {
  results: TestResult[]
}

interface TestResult {
  duration: number
}
// End Playwright json report structure

interface RunDurationTable {
  [file: string]: number
}

// interface Bucket {
//   duration: number
//   files: string[]
// }

export default RunHeuristicsReporter
