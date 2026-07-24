import { expect } from "@playwright/test";
import { test } from "./fixtures.ts";

test("Quirks should be able to be changed", async ({ page }) => {
  await page.goto("http://localhost:5800/#/cameras");

  await page.locator("div.d-flex", { has: page.locator("span", { hasText: "Arducam Model" }) }).locator("div.v-field").click();

  await page.locator(".v-overlay .v-list .v-list-item", { hasText: "OV9281" }).click();

  await page.locator("button", { has: page.locator("span", { hasText: "Save Changes" }) }).click();

  await expect(page.locator(".v-overlay p").last()).toHaveText("Camera settings updated successfully");

  await page.locator("div.d-flex", { has: page.locator("span", { hasText: "Arducam Model" }) }).locator("div.v-field").click();

  await page.locator(".v-overlay .v-list .v-list-item", { hasText: "None" }).click();

  await page.locator("button", { has: page.locator("span", { hasText: "Save Changes" }) }).click();

  await expect(page.locator(".v-overlay p").last()).toHaveText("Camera settings updated successfully");
});
