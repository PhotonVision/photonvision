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

  // The Processed stream card: the one that shows only the cropped pixels inside the black box.
  // (The Raw card, when shown, carries the full frame with the outside dimmed by the backend.)
  const processedFrame = (page: Page): Locator =>
    page.locator(".stream-frame").filter({ has: page.getByAltText("Processed Stream View") });

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

  test("the stream shows a black box around the cropped region", async ({ page }) => {
    // With no crop, the stream's frame box is transparent, and the stream actually renders --
    // Playwright visibility requires a non-empty bounding box, so this also guards against the
    // stream layout collapsing to zero size.
    const frame = processedFrame(page);
    await expect(frame).not.toHaveCSS("background-color", "rgb(0, 0, 0)");
    await expect(frame.locator("img")).toBeVisible();
    expect((await frame.locator("img").boundingBox())?.height ?? 0).toBeGreaterThan(50);

    await cropSwitch(page).check();

    const cropXMax = cropRangeInputs(page, "Crop X Range").nth(1);
    const frameWidth = Number(await cropXMax.getAttribute("max"));

    // Crop away the right half of the frame.
    await cropXMax.fill(String(Math.floor(frameWidth / 2)));
    await cropXMax.press("Enter");

    // The cropped-away area shows as a black box around the (repositioned) stream.
    await expect(frame).toHaveCSS("background-color", "rgb(0, 0, 0)");
    // The stream image itself now occupies only the left half of the frame box, and both the black
    // box and the stream have real on-screen size.
    const stream = frame.locator("img");
    await expect(stream).toHaveAttribute("style", /width: 50%/);
    await expect(stream).toHaveAttribute("style", /left: 0%/);
    // The crop region is outlined in PhotonVision yellow, with resize handles on its corners and
    // border midpoints.
    await expect(frame.locator(".crop-outline")).toHaveCSS("outline-color", "rgb(255, 216, 67)");
    await expect(frame.locator(".crop-outline")).toHaveAttribute("style", /width: 50%/);
    await expect(frame.locator(".crop-handle")).toHaveCount(8);
    await expect(frame.locator(".crop-handle").first()).toBeVisible();
    await expect(stream).toBeVisible();
    const frameBox = await frame.boundingBox();
    const streamBox = await stream.boundingBox();
    expect(frameBox?.height ?? 0).toBeGreaterThan(50);
    // The black box extends past the stream's right edge by about the cropped-away half.
    expect((streamBox?.width ?? 0) * 2).toBeCloseTo(frameBox?.width ?? 0, -1);

    // Turning the crop off removes the box and the handles.
    await cropSwitch(page).uncheck();
    await expect(frame).not.toHaveCSS("background-color", "rgb(0, 0, 0)");
    await expect(frame.locator(".crop-handle").first()).not.toBeVisible();
  });

  test("a crop region can be drawn on the stream", async ({ page }) => {
    // Drawing mode is entered from the Input tab.
    await page.getByRole("button", { name: "Draw Crop Region" }).click();
    await expect(page.getByText("Drag a box on the camera stream")).toBeVisible();

    // Drag a box over the middle of the stream: 25%..75% in x, 25%..60% in y. The stream card can
    // be scrolled out of view when the Input tab is focused, so bring it back first.
    const frame = processedFrame(page);
    await frame.scrollIntoViewIfNeeded();
    const box = await frame.boundingBox();
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

    // Grab the visible crop region and drag it a quarter of the frame to the right.
    const frame = processedFrame(page);
    await frame.scrollIntoViewIfNeeded();
    const stream = frame.locator("img");
    const streamBox = await stream.boundingBox();
    const frameBox = await frame.boundingBox();
    expect(streamBox).not.toBeNull();
    expect(frameBox).not.toBeNull();
    if (!streamBox || !frameBox) return;
    const grabX = streamBox.x + streamBox.width / 2;
    const grabY = streamBox.y + streamBox.height / 2;
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

    const frame = processedFrame(page);
    await frame.scrollIntoViewIfNeeded();
    const stream = frame.locator("img");
    const frameBox = await frame.boundingBox();
    let streamBox = await stream.boundingBox();
    expect(frameBox).not.toBeNull();
    expect(streamBox).not.toBeNull();
    if (!frameBox || !streamBox) return;

    const near = async (locator: Locator, expected: number) => {
      await expect
        .poll(async () => Math.abs(Number(await locator.inputValue()) - expected))
        .toBeLessThanOrEqual(Math.max(10, frameWidth / 100));
    };

    // Drag the right border a tenth of the frame to the right: only x1 follows.
    await page.mouse.move(streamBox.x + streamBox.width - 3, streamBox.y + streamBox.height / 2);
    await page.mouse.down();
    await page.mouse.move(
      streamBox.x + streamBox.width - 3 + frameBox.width * 0.1,
      streamBox.y + streamBox.height / 2,
      {
        steps: 5
      }
    );
    await page.mouse.up();

    await near(xInputs.nth(1), 1200 + frameWidth * 0.1);
    await expect(xInputs.nth(0)).toHaveValue("400");
    await expect(yInputs.nth(0)).toHaveValue("200");
    await expect(yInputs.nth(1)).toHaveValue("600");

    // Drag the bottom-right corner inward and down: x1 and y1 follow, the other bounds hold.
    streamBox = await stream.boundingBox();
    expect(streamBox).not.toBeNull();
    if (!streamBox) return;
    const cornerX = streamBox.x + streamBox.width - 3;
    const cornerY = streamBox.y + streamBox.height - 3;
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
