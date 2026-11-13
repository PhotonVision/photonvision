import { expect, type Locator, type Page } from "@playwright/test";
import { test } from "./fixtures.ts";

test.describe("Static Crop", () => {
  const openInputTab = async (page: Page) => {
    const setupPrompt = page.getByText("Set up some cameras to get started!");
    if (await setupPrompt.isVisible().catch(() => false)) {
      await page.keyboard.press("Escape");
    }

    await page.getByRole("tab", { name: "Input", exact: true }).first().click();
  };

  test.beforeEach(async ({ page }) => {
    await page.goto("http://localhost:5800/#/dashboard");
    await openInputTab(page);

    await page.getByRole("button", { name: "Reset Crop" }).click();
    const toggle = cropSwitch(page);
    if (await toggle.isChecked()) {
      await toggle.uncheck();
      await expect(toggle).not.toBeChecked();
    }
  });

  const cropSwitch = (page: Page): Locator =>
    page
      .locator("div.d-flex")
      .filter({ hasText: "Static Crop" })
      .filter({ has: page.getByRole("checkbox") })
      .last()
      .getByRole("checkbox");
  const cropRangeInputs = (page: Page, label: string): Locator =>
    page
      .locator("div.d-flex")
      .filter({ hasText: label })
      .filter({ has: page.locator("input[type=number]") })
      .last()
      .locator("input[type=number]");

  const processedFrame = (page: Page): Locator =>
    page.locator(".stream-frame").filter({ has: page.getByAltText("Processed Stream View") });
  const rawFrame = (page: Page): Locator =>
    page.locator(".stream-frame").filter({ has: page.getByAltText("Raw Stream View") });
  const showRawStream = async (page: Page) => {
    await page.waitForTimeout(300);
    if (
      !(await rawFrame(page)
        .isVisible()
        .catch(() => false))
    ) {
      await page.getByRole("button", { name: "Raw", exact: true }).click();
    }
    await expect(rawFrame(page)).toBeVisible();
  };

  const stableBoundingBox = async (locator: Locator) => {
    let previous = await locator.boundingBox();
    for (let attempt = 0; attempt < 20; attempt++) {
      await locator.page().waitForTimeout(150);
      const current = await locator.boundingBox();
      if (
        previous &&
        current &&
        Math.abs(current.x - previous.x) < 1 &&
        Math.abs(current.y - previous.y) < 1 &&
        Math.abs(current.width - previous.width) < 1 &&
        Math.abs(current.height - previous.height) < 1
      ) {
        return current;
      }
      previous = current;
    }
    return previous;
  };

  const setOrientation = async (page: Page, name: string) => {
    await page.locator("div.d-flex").filter({ hasText: "Orientation" }).locator(".v-select").click();
    await page.getByRole("option", { name, exact: true }).click();
  };

  test("controls are present and start disabled", async ({ page }) => {
    await expect(page.getByText("Static Crop", { exact: true })).toBeVisible();
    await expect(page.getByText("Crop X Range", { exact: true })).toBeVisible();
    await expect(page.getByText("Crop Y Range", { exact: true })).toBeVisible();

    await expect(cropSwitch(page)).not.toBeChecked();

    await expect(
      page.locator("div.d-flex").filter({ hasText: "Crop X Range" }).locator(".v-input--disabled")
    ).toHaveCount(1);
  });

  test("enabling static crop activates the range sliders", async ({ page }) => {
    const toggle = cropSwitch(page);
    await toggle.check();
    await expect(toggle).toBeChecked();

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
    const frameWidth = Number(await cropXMax.getAttribute("max"));
    expect(frameWidth).toBeGreaterThan(0);

    await expect(cropRangeInputs(page, "Crop X Range").nth(0)).toHaveValue("0");
    await expect(cropXMax).toHaveValue(String(frameWidth));

    await cropXMax.fill("200");
    await cropXMax.press("Enter");
    await expect(cropXMax).toHaveValue("200");

    await cropXMax.fill(String(frameWidth + 5000));
    await cropXMax.press("Enter");
    expect(Number(await cropXMax.inputValue())).toBeLessThanOrEqual(frameWidth);
  });

  test("a crop bound on the frame edge follows the frame both ways", async ({ page }) => {
    await cropSwitch(page).check();

    const cropXMax = cropRangeInputs(page, "Crop X Range").nth(1);
    const frameWidth = Number(await cropXMax.getAttribute("max"));
    const frameHeight = Number(await cropRangeInputs(page, "Crop Y Range").nth(1).getAttribute("max"));

    test.skip(frameWidth === frameHeight, "The test camera's frame is square");

    await cropXMax.fill(String(frameWidth));
    await cropXMax.press("Enter");
    await setOrientation(page, "90° CW");

    await expect(cropXMax).toHaveAttribute("max", String(frameHeight));
    await expect(cropXMax).toHaveValue(String(frameHeight));

    await setOrientation(page, "Normal");
    await expect(cropXMax).toHaveAttribute("max", String(frameWidth));
    await expect(cropXMax).toHaveValue(String(frameWidth));
  });

  test("the processed view shows only the cropped image", async ({ page }) => {
    const frame = processedFrame(page);
    await expect(frame.locator("img")).toBeVisible();

    await cropSwitch(page).check();
    const cropXMax = cropRangeInputs(page, "Crop X Range").nth(1);
    const frameWidth = Number(await cropXMax.getAttribute("max"));
    const frameHeight = Number(await cropRangeInputs(page, "Crop Y Range").nth(1).getAttribute("max"));
    await cropXMax.fill(String(Math.floor(frameWidth / 2)));
    await cropXMax.press("Enter");

    const container = page.locator(".stream-container").filter({ has: page.getByAltText("Processed Stream View") });
    await expect(container).toHaveAttribute(
      "style",
      new RegExp(`aspect-ratio: ${frameWidth}\\s*\\/\\s*${frameHeight}`)
    );
    await expect(frame).not.toHaveCSS("background-color", "rgb(0, 0, 0)");
    await expect(frame.locator("img")).not.toHaveAttribute("style", /width: 50%/);
    // And no crop overlays -- those live on the Raw view.
    await expect(frame.locator(".crop-outline")).not.toBeVisible();
    await expect(frame.locator(".crop-handle").first()).not.toBeVisible();

    await showRawStream(page);
    const raw = rawFrame(page);
    await expect(raw.locator(".crop-outline")).toBeVisible();
    await expect(raw.locator(".crop-outline")).toHaveCSS("outline-color", "rgb(255, 216, 67)");
    await expect(raw.locator(".crop-outline")).toHaveAttribute("style", /width: 50%/);
    await expect(raw.locator(".crop-handle")).toHaveCount(8);
    await expect(raw.locator(".crop-handle").first()).toBeVisible();

    await cropSwitch(page).uncheck();
    await expect(raw.locator(".crop-outline")).not.toBeVisible();
    await expect(container).toHaveAttribute(
      "style",
      new RegExp(`aspect-ratio: ${frameWidth}\\s*\\/\\s*${frameHeight}`)
    );
  });

  test("enabling the crop shows the adjustable region on the raw stream", async ({ page }) => {
    await showRawStream(page);
    const raw = rawFrame(page);

    await expect(raw.locator(".crop-outline")).not.toBeVisible();

    await cropSwitch(page).check();
    await expect(raw.locator(".crop-outline")).toBeVisible();
    await expect(raw.locator(".crop-outline")).toHaveAttribute("style", /width: 100%/);
    await expect(raw.locator(".crop-handle")).toHaveCount(8);
    await expect(raw.locator(".crop-handle").first()).toBeVisible();

    const xInputs = cropRangeInputs(page, "Crop X Range");
    const frameWidth = Number(await xInputs.nth(1).getAttribute("max"));
    await raw.scrollIntoViewIfNeeded();
    const box = await stableBoundingBox(raw);
    expect(box).not.toBeNull();
    if (!box) return;
    await page.mouse.move(box.x + box.width - 3, box.y + box.height * 0.5);
    await page.mouse.down();
    await page.mouse.move(box.x + box.width * 0.75, box.y + box.height * 0.5, { steps: 5 });
    await page.mouse.up();

    await expect
      .poll(async () => Math.abs(Number(await xInputs.nth(1).inputValue()) - frameWidth * 0.75))
      .toBeLessThanOrEqual(Math.max(10, frameWidth / 100));
    await expect(xInputs.nth(0)).toHaveValue("0");
  });

  test("the reset button restores the full-frame crop", async ({ page }) => {
    await cropSwitch(page).check();

    const xInputs = cropRangeInputs(page, "Crop X Range");
    const yInputs = cropRangeInputs(page, "Crop Y Range");
    const frameWidth = Number(await xInputs.nth(1).getAttribute("max"));
    const frameHeight = Number(await yInputs.nth(1).getAttribute("max"));

    await xInputs.nth(1).fill("300");
    await xInputs.nth(1).press("Enter");
    await yInputs.nth(0).fill("100");
    await yInputs.nth(0).press("Enter");
    await expect(xInputs.nth(1)).toHaveValue("300");

    await page.getByRole("button", { name: "Reset Crop" }).click();

    await expect(xInputs.nth(0)).toHaveValue("0");
    await expect(xInputs.nth(1)).toHaveValue(String(frameWidth));
    await expect(yInputs.nth(0)).toHaveValue("0");
    await expect(yInputs.nth(1)).toHaveValue(String(frameHeight));
  });

  test("the crop region can be dragged to a new position", async ({ page }) => {
    await cropSwitch(page).check();

    const xInputs = cropRangeInputs(page, "Crop X Range");
    const yInputs = cropRangeInputs(page, "Crop Y Range");
    const frameWidth = Number(await xInputs.nth(1).getAttribute("max"));
    await xInputs.nth(0).fill("100");
    await xInputs.nth(0).press("Enter");
    await xInputs.nth(1).fill("500");
    await xInputs.nth(1).press("Enter");
    await yInputs.nth(0).fill("200");
    await yInputs.nth(0).press("Enter");
    await yInputs.nth(1).fill("500");
    await yInputs.nth(1).press("Enter");

    await showRawStream(page);
    const frame = rawFrame(page);
    await frame.scrollIntoViewIfNeeded();
    const frameBox = await stableBoundingBox(frame);
    expect(frameBox).not.toBeNull();
    if (!frameBox) return;
    const frameHeight = Number(await yInputs.nth(1).getAttribute("max"));
    const grabX = frameBox.x + (300 / frameWidth) * frameBox.width;
    const grabY = frameBox.y + (350 / frameHeight) * frameBox.height;
    await page.mouse.move(grabX, grabY);
    await page.mouse.down();
    await page.mouse.move(grabX + frameBox.width * 0.25, grabY, { steps: 5 });

    await expect.poll(async () => Number(await xInputs.nth(0).inputValue())).toBeGreaterThan(150);

    await page.mouse.up();

    const near = async (locator: Locator, expected: number) => {
      await expect
        .poll(async () => Math.abs(Number(await locator.inputValue()) - expected))
        .toBeLessThanOrEqual(Math.max(10, frameWidth / 100));
    };
    await near(xInputs.nth(0), 100 + frameWidth * 0.25);
    await near(xInputs.nth(1), 500 + frameWidth * 0.25);
    await expect(yInputs.nth(0)).toHaveValue("200");
    await expect(yInputs.nth(1)).toHaveValue("500");
    expect(Number(await xInputs.nth(1).inputValue()) - Number(await xInputs.nth(0).inputValue())).toBe(400);
  });

  test("the crop region borders can be dragged to resize it", async ({ page }) => {
    await cropSwitch(page).check();

    const xInputs = cropRangeInputs(page, "Crop X Range");
    const yInputs = cropRangeInputs(page, "Crop Y Range");
    const frameWidth = Number(await xInputs.nth(1).getAttribute("max"));
    const frameHeight = Number(await yInputs.nth(1).getAttribute("max"));
    await xInputs.nth(0).fill("400");
    await xInputs.nth(0).press("Enter");
    await xInputs.nth(1).fill("1200");
    await xInputs.nth(1).press("Enter");
    await yInputs.nth(0).fill("200");
    await yInputs.nth(0).press("Enter");
    await yInputs.nth(1).fill("600");
    await yInputs.nth(1).press("Enter");

    await showRawStream(page);
    const frame = rawFrame(page);
    await frame.scrollIntoViewIfNeeded();
    const frameBox = await stableBoundingBox(frame);
    expect(frameBox).not.toBeNull();
    if (!frameBox) return;
    const screenX = (px: number) => frameBox.x + (px / frameWidth) * frameBox.width;
    const screenY = (px: number) => frameBox.y + (px / frameHeight) * frameBox.height;

    const near = async (locator: Locator, expected: number) => {
      await expect
        .poll(async () => Math.abs(Number(await locator.inputValue()) - expected))
        .toBeLessThanOrEqual(Math.max(10, frameWidth / 100));
    };

    await page.mouse.move(screenX(1200) - 3, screenY(400));
    await page.mouse.down();
    await page.mouse.move(screenX(1200) - 3 + frameBox.width * 0.1, screenY(400), { steps: 5 });
    await page.mouse.up();

    await near(xInputs.nth(1), 1200 + frameWidth * 0.1);
    await expect(xInputs.nth(0)).toHaveValue("400");
    await expect(yInputs.nth(0)).toHaveValue("200");
    await expect(yInputs.nth(1)).toHaveValue("600");

    const frameBox2 = await stableBoundingBox(frame);
    expect(frameBox2).not.toBeNull();
    if (!frameBox2) return;
    const newX1 = Number(await xInputs.nth(1).inputValue());
    const cornerX = frameBox2.x + (newX1 / frameWidth) * frameBox2.width - 3;
    const cornerY = frameBox2.y + (600 / frameHeight) * frameBox2.height - 3;
    await page.mouse.move(cornerX, cornerY);
    await page.mouse.down();
    await page.mouse.move(cornerX - frameBox.width * 0.05, cornerY + frameBox.height * 0.1, { steps: 5 });
    await page.mouse.up();

    const x1AfterFirstDrag = 1200 + frameWidth * 0.1;
    await near(xInputs.nth(1), x1AfterFirstDrag - frameWidth * 0.05);
    await near(yInputs.nth(1), 600 + frameHeight * 0.1);
    await expect(xInputs.nth(0)).toHaveValue("400");
    await expect(yInputs.nth(0)).toHaveValue("200");
  });

  test("borders and corners can be grabbed from outside the region", async ({ page }) => {
    await cropSwitch(page).check();

    const xInputs = cropRangeInputs(page, "Crop X Range");
    const yInputs = cropRangeInputs(page, "Crop Y Range");
    const frameWidth = Number(await xInputs.nth(1).getAttribute("max"));
    const frameHeight = Number(await yInputs.nth(1).getAttribute("max"));
    await xInputs.nth(0).fill("400");
    await xInputs.nth(0).press("Enter");
    await xInputs.nth(1).fill("800");
    await xInputs.nth(1).press("Enter");
    await yInputs.nth(0).fill("200");
    await yInputs.nth(0).press("Enter");
    await yInputs.nth(1).fill("400");
    await yInputs.nth(1).press("Enter");

    await showRawStream(page);
    const frame = rawFrame(page);
    await frame.scrollIntoViewIfNeeded();
    const frameBox = await stableBoundingBox(frame);
    expect(frameBox).not.toBeNull();
    if (!frameBox) return;
    const screenX = (px: number) => frameBox.x + (px / frameWidth) * frameBox.width;
    const screenY = (px: number) => frameBox.y + (px / frameHeight) * frameBox.height;

    const near = async (locator: Locator, expected: number) => {
      await expect
        .poll(async () => Math.abs(Number(await locator.inputValue()) - expected))
        .toBeLessThanOrEqual(Math.max(10, frameWidth / 100));
    };

    await page.mouse.move(screenX(800) + 8, screenY(300));
    await page.mouse.down();
    await page.mouse.move(screenX(800) + 8 + frameBox.width * 0.1, screenY(300), { steps: 5 });
    await page.mouse.up();

    await near(xInputs.nth(1), 800 + frameWidth * 0.1);
    await expect(xInputs.nth(0)).toHaveValue("400");
    await expect(yInputs.nth(0)).toHaveValue("200");
    await expect(yInputs.nth(1)).toHaveValue("400");

    const frameBox2 = await stableBoundingBox(frame);
    expect(frameBox2).not.toBeNull();
    if (!frameBox2) return;
    const newX1 = Number(await xInputs.nth(1).inputValue());
    const cornerX = frameBox2.x + (newX1 / frameWidth) * frameBox2.width + 6;
    const cornerY = frameBox2.y + (400 / frameHeight) * frameBox2.height + 6;
    await page.mouse.move(cornerX, cornerY);
    await page.mouse.down();
    await page.mouse.move(cornerX + frameBox2.width * 0.05, cornerY + frameBox2.height * 0.1, { steps: 5 });
    await page.mouse.up();

    const x1AfterFirstDrag = 800 + frameWidth * 0.1;
    await near(xInputs.nth(1), x1AfterFirstDrag + frameWidth * 0.05);
    await near(yInputs.nth(1), 400 + frameHeight * 0.1);
    await expect(xInputs.nth(0)).toHaveValue("400");
    await expect(yInputs.nth(0)).toHaveValue("200");
  });

  test("a warning banner shows while the raw stream is open with cropping enabled", async ({ page }) => {
    const banner = page.getByText(/static cropping enabled while the raw stream is open/i);

    await cropSwitch(page).check();
    if (
      await rawFrame(page)
        .isVisible()
        .catch(() => false)
    ) {
      await page.getByRole("button", { name: "Raw", exact: true }).click();
      await expect(rawFrame(page)).not.toBeVisible();
    }
    await expect(banner).not.toBeVisible();

    await showRawStream(page);
    await expect(banner).toBeVisible();

    await cropSwitch(page).uncheck();
    await expect(banner).not.toBeVisible();
  });

  test("an interior crop bound survives a frame resize", async ({ page }) => {
    await cropSwitch(page).check();

    const cropXMax = cropRangeInputs(page, "Crop X Range").nth(1);
    const frameWidth = Number(await cropXMax.getAttribute("max"));
    const frameHeight = Number(await cropRangeInputs(page, "Crop Y Range").nth(1).getAttribute("max"));
    test.skip(frameWidth === frameHeight, "The test camera's frame is square");

    const interiorBound = String(Math.floor(Math.min(frameWidth, frameHeight) / 2));
    await cropXMax.fill(interiorBound);
    await cropXMax.press("Enter");

    await setOrientation(page, "90° CW");
    await expect(cropXMax).toHaveValue(interiorBound);
    await setOrientation(page, "Normal");
    await expect(cropXMax).toHaveValue(interiorBound);
  });
});
