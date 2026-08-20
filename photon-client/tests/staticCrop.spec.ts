import { expect, type Locator, type Page } from "@playwright/test";
import { test } from "./fixtures.ts";

// The test-mode backend loads a camera running a vision pipeline (not driver/calibration/focus),
// so the static-crop controls in the Input tab are available.
test.describe("Static Crop", () => {
  const openInputTab = async (page: Page) => {
    // Dismiss the "Set up some cameras to get started!" prompt if it appears.
    const setupPrompt = page.getByText("Set up some cameras to get started!");
    if (await setupPrompt.isVisible().catch(() => false)) {
      await page.keyboard.press("Escape");
    }

    // The Input tab holds the static-crop controls; make sure it is selected.
    await page.getByRole("tab", { name: "Input", exact: true }).first().click();
  };

  test.beforeEach(async ({ page }) => {
    await page.goto("http://localhost:5800/#/dashboard");
    await openInputTab(page);
  });

  // Vuetify's v-switch exposes the ARIA "checkbox" role, not "switch".
  const cropSwitch = (page: Page): Locator =>
    page.locator("div.d-flex").filter({ hasText: "Static Crop" }).getByRole("checkbox");
  const cropRangeInputs = (page: Page, label: string): Locator =>
    page.locator("div.d-flex").filter({ hasText: label }).locator("input[type=number]");

  const setOrientation = async (page: Page, name: string) => {
    await page.locator("div.d-flex").filter({ hasText: "Orientation" }).locator(".v-select").click();
    await page.getByRole("option", { name, exact: true }).click();
  };

  test("controls are present and default to disabled", async ({ page }) => {
    await expect(page.getByText("Static Crop", { exact: true })).toBeVisible();
    await expect(page.getByText("Crop X Range", { exact: true })).toBeVisible();
    await expect(page.getByText("Crop Y Range", { exact: true })).toBeVisible();

    // Crop is off by default.
    await expect(cropSwitch(page)).not.toBeChecked();

    // The range slider itself is disabled while the crop is off (Vuetify marks this with a class on
    // the input wrapper).
    await expect(
      page.locator("div.d-flex").filter({ hasText: "Crop X Range" }).locator(".v-input--disabled")
    ).toHaveCount(1);
  });

  test("enabling static crop activates the range sliders", async ({ page }) => {
    const toggle = cropSwitch(page);
    await toggle.check();
    await expect(toggle).toBeChecked();

    // With the crop enabled, the range sliders are no longer disabled.
    await expect(
      page.locator("div.d-flex").filter({ hasText: "Crop X Range" }).locator(".v-input--disabled")
    ).toHaveCount(0);
    await expect(
      page.locator("div.d-flex").filter({ hasText: "Crop Y Range" }).locator(".v-input--disabled")
    ).toHaveCount(0);
  });

  test("crop range values can be set and are clamped to the frame", async ({ page }) => {
    await cropSwitch(page).check();

    const cropXMax = cropRangeInputs(page, "Crop X Range").nth(1);
    // The slider max is bound to the (rotated) frame width.
    const frameWidth = Number(await cropXMax.getAttribute("max"));
    expect(frameWidth).toBeGreaterThan(0);

    // The crop defaults to the whole frame: 0 to the frame width.
    await expect(cropRangeInputs(page, "Crop X Range").nth(0)).toHaveValue("0");
    await expect(cropXMax).toHaveValue(String(frameWidth));

    await cropXMax.fill("200");
    await cropXMax.press("Enter");
    await expect(cropXMax).toHaveValue("200");

    // Values beyond the frame width are clamped to the frame width.
    await cropXMax.fill(String(frameWidth + 5000));
    await cropXMax.press("Enter");
    expect(Number(await cropXMax.inputValue())).toBeLessThanOrEqual(frameWidth);
  });

  test("a crop bound on the frame edge follows the frame both ways", async ({ page }) => {
    await cropSwitch(page).check();

    const cropXMax = cropRangeInputs(page, "Crop X Range").nth(1);
    const frameWidth = Number(await cropXMax.getAttribute("max"));
    const frameHeight = Number(await cropRangeInputs(page, "Crop Y Range").nth(1).getAttribute("max"));

    // Rotating 90° swaps the crop bounds, which only resizes an axis on a non-square frame.
    test.skip(frameWidth === frameHeight, "The test camera's frame is square");

    // Crop to the full width, then rotate so that width no longer fits in the frame.
    await cropXMax.fill(String(frameWidth));
    await cropXMax.press("Enter");
    await setOrientation(page, "90° CW");

    await expect(cropXMax).toHaveAttribute("max", String(frameHeight));
    await expect(cropXMax).toHaveValue(String(frameHeight));

    // Back to the wider frame: a bound on the edge tracks the edge, rather than staying at the pixel
    // count of the narrower frame.
    await setOrientation(page, "Normal");
    await expect(cropXMax).toHaveAttribute("max", String(frameWidth));
    await expect(cropXMax).toHaveValue(String(frameWidth));
  });

  test("an interior crop bound survives a frame resize", async ({ page }) => {
    await cropSwitch(page).check();

    const cropXMax = cropRangeInputs(page, "Crop X Range").nth(1);
    const frameWidth = Number(await cropXMax.getAttribute("max"));
    const frameHeight = Number(await cropRangeInputs(page, "Crop Y Range").nth(1).getAttribute("max"));
    test.skip(frameWidth === frameHeight, "The test camera's frame is square");

    // Half the shorter side, so the bound fits inside the frame in either orientation.
    const interiorBound = String(Math.floor(Math.min(frameWidth, frameHeight) / 2));
    await cropXMax.fill(interiorBound);
    await cropXMax.press("Enter");

    // A bound the user picked isn't on the edge, so resizing the frame leaves it exactly where it is.
    await setOrientation(page, "90° CW");
    await expect(cropXMax).toHaveValue(interiorBound);
    await setOrientation(page, "Normal");
    await expect(cropXMax).toHaveValue(interiorBound);
  });
});
