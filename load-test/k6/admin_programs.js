// Use Case: An admin loading the question and program lists.

import { browser } from "k6/browser";
import { expect } from "https://jslib.k6.io/k6-testing/0.5.0/index.js";

export const options = {
  scenarios: {
    default: {
      executor: "shared-iterations",
      options: { browser: { type: "chromium" } },
      // Simultaneous users
      vus: 1,
      // Total runs across all users
      iterations: 20,
      // max duration of the test, defaults to 10m
      maxDuration: "30m",
    },
  },

  thresholds: {
    // These are not based on realistic expectations as we currently are addressing
    // latency issues with these handlers.
    "browser_http_req_duration{name:admin_questions}": ['avg < 75', 'p(95) < 100']
    "browser_http_req_duration{name:admin_programs}": ['avg < 200', 'p(95) < 200'],
  }
};

export default async function () {
  const page = await browser.newPage();

  // Connect urls with metric tags to use in the thresholds.
  page.on("metric", (metric) => {
    metric.tag({
      name: "admin_questions", matches: [{ url: /\/admin\/questions$/ }],
    });
    metric.tag({
      name: "admin_programs", matches: [{ url: /\/admin\/programs$/ }],
    });
  });

  try {
    await page.goto("http://civiform:9000/");
    await page.getByRole("button", { name: "DevTools", exact: true }).click();

    await Promise.all([
      page.waitForNavigation(),
      page.getByRole("link", { name: "Civiform Admin", exact: true }).click(),
    ]);

    await Promise.all([
      page.waitForNavigation(),
      page.getByRole("link", { name: "Questions", exact: true }).click(),
    ]);

    // Run both handlers a second time just to get a little more from the
    // cost of logging in without throwing off the utility of the scenario
    // run configs too much.
    await Promise.all([
      page.waitForNavigation(),
      page.getByRole("link", { name: "Programs", exact: true }).click(),
    ]);
	  
    await Promise.all([
      page.waitForNavigation(),
      page.getByRole("link", { name: "Questions", exact: true }).click(),
    ]);

    // Ensure the page loaded.
    await expect(
      page.getByRole("heading", { name: "All questions", exact: true }),
    ).toBeVisible();

  } finally {
    await page?.close();
  }
}
