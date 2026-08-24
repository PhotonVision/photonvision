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

    // The backend keeps pipeline settings in memory across tests (resetBackend only clears the
    // config directory), and CI runs several browser projects against one server -- so normalize
    // the crop state instead of assuming defaults: full-frame bounds, crop off.
    await page.getByRole("button", { name: "Reset Crop" }).click();
    const toggle = cropSwitch(page);
    if (await toggle.isChecked()) {
      await toggle.uncheck();
      await expect(toggle).not.toBeChecked();
    }
  });

  // Vuetify's v-switch exposes the ARIA "checkbox" role, not "switch". The .last() picks the
  // innermost matching row -- ancestor flex containers can also match the text filter once the
  // dashboard lays out multiple stream cards.
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

  // The Processed stream card shows only the cropped pixels; the Raw card (when shown) carries the
  // full frame with the outside dimmed by the backend, and owns all crop overlays and interaction.
  const processedFrame = (page: Page): Locator =>
    page.locator(".stream-frame").filter({ has: page.getByAltText("Processed Stream View") });
  const rawFrame = (page: Page): Locator =>
    page.locator(".stream-frame").filter({ has: page.getByAltText("Raw Stream View") });
  // Turn on the Raw stream via the Stream Display toggle.
  const showRawStream = async (page: Page) => {
    // The Stream Display "Raw" button is a toggle, and earlier settings changes can have left the
    // input stream already on -- only click when the Raw card isn't showing.
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

  // A bounding box that has stopped moving: stream cards reflow when the Raw card appears, and a
  // box measured mid-reflow sends drag coordinates to the wrong place.
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

  test("the processed view shows only the cropped image", async ({ page }) => {
    const frame = processedFrame(page);
    await expect(frame.locator("img")).toBeVisible();

    await cropSwitch(page).check();
    const cropXMax = cropRangeInputs(page, "Crop X Range").nth(1);
    const frameWidth = Number(await cropXMax.getAttribute("max"));
    const frameHeight = Number(await cropRangeInputs(page, "Crop Y Range").nth(1).getAttribute("max"));
    await cropXMax.fill(String(Math.floor(frameWidth / 2)));
    await cropXMax.press("Enter");

    // The processed card reshapes to the crop -- no black box, no repositioned image, just the
    // cropped pixels at the crop's aspect ratio.
    const container = page.locator(".stream-container").filter({ has: page.getByAltText("Processed Stream View") });
    await expect(container).toHaveAttribute(
      "style",
      new RegExp(`aspect-ratio: ${Math.floor(frameWidth / 2)}\\s*/\\s*${frameHeight}`)
    );
    await expect(frame).not.toHaveCSS("background-color", "rgb(0, 0, 0)");
    await expect(frame.locator("img")).not.toHaveAttribute("style", /width: 50%/);
    // And no crop overlays -- those live on the Raw view.
    await expect(frame.locator(".crop-outline")).not.toBeVisible();
    await expect(frame.locator(".crop-handle").first()).not.toBeVisible();

    // The Raw view shows the crop in context: yellow outline and handles at the region.
    await showRawStream(page);
    const raw = rawFrame(page);
    await expect(raw.locator(".crop-outline")).toBeVisible();
    await expect(raw.locator(".crop-outline")).toHaveCSS("outline-color", "rgb(255, 216, 67)");
    await expect(raw.locator(".crop-outline")).toHaveAttribute("style", /width: 50%/);
    await expect(raw.locator(".crop-handle")).toHaveCount(8);
    await expect(raw.locator(".crop-handle").first()).toBeVisible();

    // Turning the crop off removes the overlays and restores the processed card's shape.
    await cropSwitch(page).uncheck();
    await expect(raw.locator(".crop-outline")).not.toBeVisible();
    await expect(container).toHaveAttribute("style", new RegExp(`aspect-ratio: ${frameWidth}\\s*/`));
  });

  test("a crop region can be drawn on the stream", async ({ page }) => {
    // Drawing mode is entered from the Input tab.
    await page.getByRole("button", { name: "Draw Crop Region" }).click();
    await expect(page.getByText("Drag a box on the camera stream")).toBeVisible();

    // Drawing mode shows only the Raw stream; drag a box over the middle of it: 25%..75% in x,
    // 25%..60% in y. The stream card can be scrolled out of view, so bring it back first.
    const frame = rawFrame(page);
    await frame.scrollIntoViewIfNeeded();
    const box = await stableBoundingBox(frame);
    expect(box).not.toBeNull();
    if (!box) return;
    await page.mouse.move(box.x + box.width * 0.25, box.y + box.height * 0.25);
    await page.mouse.down();
    await page.mouse.move(box.x + box.width * 0.75, box.y + box.height * 0.6, { steps: 5 });
    await page.mouse.up();

    // Drawing enables the crop and exits drawing mode.
    await expect(cropSwitch(page)).toBeChecked();
    await expect(page.getByText("Drag a box on the camera stream")).not.toBeVisible();

    // The drawn fractions land in the crop sliders as pixel bounds of the (rotated) frame.
    const xInputs = cropRangeInputs(page, "Crop X Range");
    const yInputs = cropRangeInputs(page, "Crop Y Range");
    const frameWidth = Number(await xInputs.nth(1).getAttribute("max"));
    const frameHeight = Number(await yInputs.nth(1).getAttribute("max"));
    const near = async (locator: Locator, expected: number) => {
      await expect
        .poll(async () => Math.abs(Number(await locator.inputValue()) - expected))
        .toBeLessThanOrEqual(Math.max(10, frameWidth / 100));
    };
    await near(xInputs.nth(0), frameWidth * 0.25);
    await near(xInputs.nth(1), frameWidth * 0.75);
    await near(yInputs.nth(0), frameHeight * 0.25);
    await near(yInputs.nth(1), frameHeight * 0.6);
  });

  test("the reset button restores the full-frame crop", async ({ page }) => {
    await cropSwitch(page).check();

    const xInputs = cropRangeInputs(page, "Crop X Range");
    const yInputs = cropRangeInputs(page, "Crop Y Range");
    const frameWidth = Number(await xInputs.nth(1).getAttribute("max"));
    const frameHeight = Number(await yInputs.nth(1).getAttribute("max"));

    // Shrink the crop, then reset it.
    await xInputs.nth(1).fill("300");
    await xInputs.nth(1).press("Enter");
    await yInputs.nth(0).fill("100");
    await yInputs.nth(0).press("Enter");
    await expect(xInputs.nth(1)).toHaveValue("300");

    await page.getByRole("button", { name: "Reset Crop" }).click();

    // Back to the whole frame on both axes.
    await expect(xInputs.nth(0)).toHaveValue("0");
    await expect(xInputs.nth(1)).toHaveValue(String(frameWidth));
    await expect(yInputs.nth(0)).toHaveValue("0");
    await expect(yInputs.nth(1)).toHaveValue(String(frameHeight));
  });

  test("the crop region can be dragged to a new position", async ({ page }) => {
    await cropSwitch(page).check();

    // A crop region in the middle-left of the frame.
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

    // Grab the crop region on the Raw view and drag it a quarter of the frame to the right.
    await showRawStream(page);
    const frame = rawFrame(page);
    await frame.scrollIntoViewIfNeeded();
    const frameBox = await stableBoundingBox(frame);
    expect(frameBox).not.toBeNull();
    if (!frameBox) return;
    // The region is x[100,500] y[200,500] of the full frame; grab its center.
    const frameHeight = Number(await yInputs.nth(1).getAttribute("max"));
    const grabX = frameBox.x + (300 / frameWidth) * frameBox.width;
    const grabY = frameBox.y + (350 / frameHeight) * frameBox.height;
    await page.mouse.move(grabX, grabY);
    await page.mouse.down();
    await page.mouse.move(grabX + frameBox.width * 0.25, grabY, { steps: 5 });

    // The move is committed live while dragging, so the stream previews the new framing before the
    // pointer is even released.
    await expect.poll(async () => Number(await xInputs.nth(0).inputValue())).toBeGreaterThan(150);

    await page.mouse.up();

    const near = async (locator: Locator, expected: number) => {
      await expect
        .poll(async () => Math.abs(Number(await locator.inputValue()) - expected))
        .toBeLessThanOrEqual(Math.max(10, frameWidth / 100));
    };
    // The region moved right by a quarter frame, keeping its 400px width and its y bounds.
    await near(xInputs.nth(0), 100 + frameWidth * 0.25);
    await near(xInputs.nth(1), 500 + frameWidth * 0.25);
    await expect(yInputs.nth(0)).toHaveValue("200");
    await expect(yInputs.nth(1)).toHaveValue("500");
    // Width is preserved exactly, whatever the drag rounding did.
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
    // Screen position of a frame-pixel coordinate on the Raw view.
    const screenX = (px: number) => frameBox.x + (px / frameWidth) * frameBox.width;
    const screenY = (px: number) => frameBox.y + (px / frameHeight) * frameBox.height;

    const near = async (locator: Locator, expected: number) => {
      await expect
        .poll(async () => Math.abs(Number(await locator.inputValue()) - expected))
        .toBeLessThanOrEqual(Math.max(10, frameWidth / 100));
    };

    // Drag the right border (region x[400,1200] y[200,600]) a tenth of the frame right: only x1
    // follows.
    await page.mouse.move(screenX(1200) - 3, screenY(400));
    await page.mouse.down();
    await page.mouse.move(screenX(1200) - 3 + frameBox.width * 0.1, screenY(400), { steps: 5 });
    await page.mouse.up();

    await near(xInputs.nth(1), 1200 + frameWidth * 0.1);
    await expect(xInputs.nth(0)).toHaveValue("400");
    await expect(yInputs.nth(0)).toHaveValue("200");
    await expect(yInputs.nth(1)).toHaveValue("600");

    // Drag the bottom-right corner inward and down: x1 and y1 follow, the other bounds hold.
    // Changing the crop reshapes the Processed card, which can shift this card's position in the
    // row -- re-measure before computing the corner's screen position.
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

  test("a warning banner shows while the raw stream is open with cropping enabled", async ({ page }) => {
    const banner = page.getByText(/static cropping enabled while the raw stream is open/i);

    // Crop on, but raw stream closed: no banner.
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

    // Opening the raw stream raises the warning.
    await showRawStream(page);
    await expect(banner).toBeVisible();

    // Disabling the crop clears it, even with the raw stream still open.
    await cropSwitch(page).uncheck();
    await expect(banner).not.toBeVisible();
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
