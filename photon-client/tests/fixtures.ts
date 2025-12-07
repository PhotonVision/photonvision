import { test as base } from "@playwright/test";
import axios from "axios";

// Track what's been setup already
const setupFiles = new Map<string, Set<string>>();

type TestFixtures = {
  fileSetup: void;
};

// Create the fixture
export const test = base.extend<TestFixtures>({
  page: async ({ page }, use) => {
    // Use the page in the test (no per-test backend reset here)
    axios.defaults.baseURL = "http://localhost:5800/api/test";
    await use(page);
  },
  fileSetup: [async ({ }, use, testInfo) => {

    const projectName = testInfo.project.name;
    const filePath = testInfo.file;
    
    // Initialize set for this project if it doesn't exist
    if (!setupFiles.has(projectName)) {
      setupFiles.set(projectName, new Set());
    }
    
    const projectFiles = setupFiles.get(projectName)!;
    
    // Only run setup once per file per project (browser)
    if (!projectFiles.has(filePath)) {
      projectFiles.add(filePath);


      console.log("Running before all tests: Resetting backend state...");
      await axios.post("http://localhost:5800/api/test/resetBackend");
      await axios.post("http://localhost:5800/api/test/activateTestMode");
    }

    // Pass control to the test
    await use();

    // Note: Cleanup runs after each test, not once per file
    // Use test.afterAll() in test files if you need per-file cleanup
  }, { auto: true }]
});

export { expect } from '@playwright/test';