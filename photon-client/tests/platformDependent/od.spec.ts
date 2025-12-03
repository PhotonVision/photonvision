import { expect } from "@playwright/test";
import { test } from "../fixtures";
import axios from "axios";
import path from "path";

const fakeModelName = "FAKE-MODEL";
const fakeLabels = "test, 1, woof";
const newModelName = "foo-bar";
const platforms = ["LINUX_RK3588_64", "LINUX_QCS6490"];

for (const platform of platforms) {
  test.describe(`Platform: ${platform}`, () => {
    test.beforeEach(async ({ page }) => {
      await page.goto("/#/settings");
      await axios.post("/override/platform", { platform: platform });
      await page.reload();
    });

    test("testSettingsPage", async ({ page }) => {
      if (platform.endsWith("RK3588_64")) {
        await expect(page.getByRole("main")).toContainText("Linux AARCH 64-bit with RK3588");
      } else if (platform.endsWith("QCS6490")) {
        await expect(page.getByRole("main")).toContainText("Linux AARCH 64-bit with QCS6490");
      }
      await expect(page.getByText("Object Detection")).toBeVisible();
    });

    test("Upload model", async ({ page }) => {
      const testsDir = process.env.TESTS_DIR;
      if (!testsDir) {
        throw new Error("TESTS_DIR is not set");
      }

      await page.getByRole("button", { name: "Import Model" }).click();
      await page.getByRole("textbox", { name: "Labels" }).fill(fakeLabels);
      await page.getByRole("spinbutton", { name: "Width" }).fill("640");
      await page.getByRole("spinbutton", { name: "Height" }).fill("640");
      await page.getByTestId("import-version-select").click();
      await page.getByRole("option", { name: "YOLOv8" }).click();

      const modelFile = platform.endsWith("RK3588_64") ? `${fakeModelName}.rknn` : `${fakeModelName}.tflite`;
      await page
        .getByRole("button", { name: "Model File Model File" })
        .setInputFiles(path.join(testsDir, "tests/resources", modelFile));

      await page.getByRole("button", { name: "Import Object Detection Model" }).click();

      await page.goto("/#/settings");
      const tableRow = page.getByTestId("model-table").locator("tr", { hasText: fakeModelName });

      await expect(tableRow).toBeVisible();
      await expect(tableRow).toContainText(fakeLabels);
    });

    test("Rename model", async ({ page }) => {
      const tableRow = page.getByTestId("model-table").locator("tr", { hasText: fakeModelName });

      await tableRow.getByRole("button", { name: "Rename Model" }).click();
      await page.getByRole("textbox", { name: "New Name New Name" }).fill(newModelName);
      await page.getByRole("button", { name: "Rename", exact: true }).click();

      await page.reload();

      const renamedRow = page.getByTestId("model-table").locator("tr", { hasText: newModelName });
      await expect(renamedRow).toContainText(fakeLabels);
    });

    test("Delete model", async ({ page }) => {
      const tableRow = page.getByTestId("model-table").locator("tr", { hasText: newModelName });

      await tableRow.getByRole("button", { name: "Delete Model" }).click();
      await page.getByRole("button", { name: "Delete model", exact: true }).click();

      await page.reload();
      const deletedRow = page.getByTestId("model-table").locator("tr", { hasText: newModelName });
      await expect(deletedRow).toHaveCount(0);
    });
  });
}
