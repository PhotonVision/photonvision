import { test as base } from "@playwright/test";
import axios from "axios";

export const test = base.extend({
  // Override the page fixture to add beforeEach behavior
  page: async ({ page }, use) => {
    // This runs before EVERY test automatically
    console.log("Running before test: Resetting backend state...");
    axios.defaults.baseURL = "http://localhost:5800/api/test";
    await axios.post("/resetBackend");
    

    // Use the page in the test
    await use(page);

    // This runs after each test (cleanup)
    console.log("Running after test...");
    // Add cleanup code here if needed
  }
});
