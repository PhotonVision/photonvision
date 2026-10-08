import { expect } from "@playwright/test";
import { test } from "./fixtures.ts";

test("Quirks should be able to be changed", async ({ page }) => {
  await page.goto("http://localhost:5800/#/cameras");

  await page.getByRole("combobox", { name: "Arducam Model" }).click();
  await page.getByRole("option", { name: "OV9281", exact: true }).click();
  await page.getByRole("button", { name: "Save Changes" }).click();

  await expect(page.locator(".snackbar-title").last()).toHaveText("Camera settings updated successfully");

  await page.getByRole("combobox", { name: "Arducam Model" }).click();
  await page.getByRole("option", { name: "None", exact: true }).click();
  await page.getByRole("button", { name: "Save Changes" }).click();

  await expect(page.locator(".snackbar-title").last()).toHaveText("Camera settings updated successfully");
});
