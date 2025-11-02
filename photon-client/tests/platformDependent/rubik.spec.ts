import { expect } from "@playwright/test";
import { test } from "../fixtures";
import axios from "axios";

test.beforeEach(async ({ page }) => {
  page.goto("/#/settings");
  await axios.post("/override/platform", { platform: "rubik" });
});

test("testSettingsPage", async ({ page }) => {
  await expect(page.getByRole("main")).toContainText("Linux AARCH 64-bit with QCS6490");
  await expect(page.getByText("Object Detection")).toBeVisible();
});
