import { expect } from "@playwright/test";
import { test } from "../fixtures";

test("has title", async ({ page }) => {
  await page.goto("");

  // Expect a title "to contain" a substring.
  await expect(page).toHaveTitle(/Photon Client/);
});
