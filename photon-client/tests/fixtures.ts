import { test as base } from "@playwright/test";
import axios from "axios";
import fs from "fs";
import path from "path";
import os from "os";

const TRACKER_FILE = path.join(os.tmpdir(), "playwright-tracker.json");

function getTracker(): Record<string, string[]> {
  try {
    return JSON.parse(fs.readFileSync(TRACKER_FILE, "utf-8"));
  } catch {
    return {};
  }
}

function markFileAsSetup(browserName: string, filePath: string): boolean {
  const tracker = getTracker();
  if (!tracker[browserName]) tracker[browserName] = [];

  if (tracker[browserName].includes(filePath)) {
    return false; // Already setup
  }

  tracker[browserName].push(filePath);
  fs.writeFileSync(TRACKER_FILE, JSON.stringify(tracker));
  return true; // Newly setup
}

type TestFixtures = {
  tracker: void;
};

export const test = base.extend<TestFixtures>({
  page: async ({ page }, use) => {
    axios.defaults.baseURL = "http://localhost:5800/api/test";
    await use(page);
  },
  tracker: [
    async ({ browserName }, use, testInfo) => {
      const filePath = testInfo.file;

      if (markFileAsSetup(browserName, filePath)) {
        console.log(`Running setup for ${filePath} in ${browserName}`);
        await axios.post("http://localhost:5800/api/test/resetBackend");
        await axios.post("http://localhost:5800/api/test/activateTestMode");
      }

      await use();
    },
    { auto: true }
  ]
});
