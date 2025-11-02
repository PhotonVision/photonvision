import { test as base } from "@playwright/test";
import axios from "axios";

export const test = base.extend({
  page: async ({ page }, use) => {
    // Use the page in the test (no per-test backend reset here)
    axios.defaults.baseURL = "http://localhost:5800/api/test";
    await use(page);
  }
});

test.beforeAll(async () => {
  console.log("Running before all tests: Resetting backend state...");
  await axios.post("http://localhost:5800/api/test/resetBackend");
  await axios.post("http://localhost:5800/api/test/activateTestMode");
});
