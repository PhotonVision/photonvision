import { expect } from "@playwright/test";
import { test } from "./fixtures.ts";

test("Camera Gain Slider won't go past max or min", async ({ page }) => {
  await page.goto("http://localhost:5800/#/dashboard");
  await page.locator("div").filter({ hasText: "Set up some cameras to get started!" }).nth(2).press("Escape");

  // Fill in Camera Gain text field with 1000
  await page.locator("#input-v-44").fill("1000");
  await page.locator("#input-v-44").press("Enter");
  await expect(page.locator("#input-v-44")).toHaveValue("100");

  // Try using buttons to go past the max
  await page.getByRole("button", { name: "appended action" }).nth(2).click();
  await expect(page.locator("#input-v-44")).toHaveValue("100");

  // Make sure the value is actually properly limited, not just visually
  await page.getByRole("button", { name: "prepended action" }).nth(2).click();
  await expect(page.locator("#input-v-44")).toHaveValue("99");

  await page.locator("#input-v-44").fill("-10");
  await page.locator("#input-v-44").press("Enter");
  await expect(page.locator("#input-v-44")).toHaveValue("0");

  await page.getByRole("button", { name: "prepended action" }).nth(2).click();
  await expect(page.locator("#input-v-44")).toHaveValue("0");

  // Make sure the value is actually properly limited, not just visually
  await page.getByRole("button", { name: "appended action" }).nth(2).click();
  await expect(page.locator("#input-v-44")).toHaveValue("1");

  // Make sure that the guard actually prevents value setting, instead of just reverting the value
  // This can be ensured by making sure the Camera Gain field doesn't disappear (disappears when the value is -1)
  await page.getByRole("button", { name: "prepended action" }).nth(2).click();
  await page.getByRole("button", { name: "prepended action" }).nth(2).click();
  await expect(page.locator("#input-v-44")).toHaveValue("0");

  await expect(page.getByText("Camera Gain", { exact: true })).toBeVisible();
});
