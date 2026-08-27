import { expect, type Locator, type Page } from "@playwright/test";
import { test } from "./fixtures.ts";

const row = (page: Page, label: string): Locator =>
  page
    .locator("div.d-flex")
    .filter({ hasText: label })
    .filter({ has: page.locator("input[type=number]") })
    .last()
    .locator("input[type=number]");

// These tests need a camera with several capture modes, which the file-based test cameras don't
// have -- they run against a real webcam when one is attached and skip cleanly otherwise (CI).
test("resolution change scaling and reset", async ({ page }) => {
  test.setTimeout(120000);

  // Activate the real webcam (multi-mode) via the camera matching page if it isn't active yet.
  await page.goto("http://localhost:5800/#/cameraConfigs");
  await page.waitForTimeout(2000);
  const webcamCard = page.locator(".v-card").filter({ hasText: /webcam/i });
  if (
    await webcamCard
      .first()
      .isVisible()
      .catch(() => false)
  ) {
    await webcamCard.first().getByRole("button", { name: "Activate" }).click();
    await page.waitForTimeout(8000);
  }

  await page.goto("http://localhost:5800/#/dashboard");
  await page.waitForTimeout(2000);
  const prompt = page.getByText("Set up some cameras to get started!");
  if (await prompt.isVisible().catch(() => false)) await page.keyboard.press("Escape");
  await page.getByRole("tab", { name: "Input", exact: true }).first().click();

  // The file cameras are single-mode; the real webcam (when present) has several capture modes.
  const camSelect = page
    .locator("div.d-flex")
    .filter({ has: page.getByText("Camera", { exact: true }) })
    .filter({ has: page.locator(".v-select") })
    .last()
    .locator(".v-select")
    .first();
  await camSelect.click();
  const camOpts = page.getByRole("option");
  await camOpts.first().waitFor();
  const cams: string[] = [];
  for (let i = 0; i < (await camOpts.count()); i++) cams.push(((await camOpts.nth(i).textContent()) ?? "").trim());
  const webcam = cams.findIndex((c) => /webcam|usb|laptop/i.test(c));

  if (webcam >= 0) {
    await camOpts.nth(webcam).click();
    await page.waitForTimeout(1500);
  } else {
    await page.keyboard.press("Escape");
  }
  test.skip(webcam < 0, "no real webcam available for multi-resolution testing");

  // The label "Resolution" exactly -- "Stream Resolution" is a different select.
  const resSelect = page
    .locator("div.d-flex")
    .filter({ has: page.getByText("Resolution", { exact: true }) })
    .filter({ has: page.locator(".v-select") })
    .last()
    .locator(".v-select")
    .first();
  await resSelect.click();
  const opts = page.getByRole("option");
  await opts.first().waitFor();
  const names: string[] = [];
  for (let i = 0; i < (await opts.count()); i++) names.push(((await opts.nth(i).textContent()) ?? "").trim());
  await page.keyboard.press("Escape");
  test.skip(names.length < 2, "camera has a single video mode; cannot exercise resolution changes");

  const pick = async (pattern: RegExp) => {
    await resSelect.click();
    await page.getByRole("option", { name: pattern }).first().click();
    await page.waitForTimeout(1500);
  };

  // Start from the largest 16:9-ish mode available.
  const start = names.find((n) => /1920x1080/.test(n)) ?? names[0];
  await pick(new RegExp(start.split(" ")[0]));
  const [sw, sh] = start.split(" ")[0].split("x").map(Number);
  const cropSwitch = page
    .locator("div.d-flex")
    .filter({ hasText: "Static Crop" })
    .filter({ has: page.getByRole("checkbox") })
    .last()
    .getByRole("checkbox");
  if (!(await cropSwitch.isChecked())) await cropSwitch.check();
  const x = row(page, "Crop X Range");
  const y = row(page, "Crop Y Range");
  test.skip(sw < 1280 || sh < 720, "starting mode too small for the chosen crop bounds");
  for (const [loc, v] of [
    [x.nth(0), 400],
    [x.nth(1), 1200],
    [y.nth(0), 200],
    [y.nth(1), 500]
  ] as [Locator, number][]) {
    await loc.fill(String(v));
    await loc.press("Enter");
  }
  await page.waitForTimeout(500);

  const dims = (n: string) => n.split(" ")[0].split("x").map(Number) as [number, number];
  const sameAspectOf = (n: string) => {
    const [w, h] = dims(n);
    return Math.abs(sw * h - w * sh) <= 0.01 * sw * h;
  };

  // Same aspect ratio: bounds scale.
  const sameAspect = names.find((n) => n !== start && sameAspectOf(n));
  test.skip(!sameAspect, "no same-aspect mode");
  await pick(new RegExp((sameAspect ?? "").split(" ")[0]));
  const [aw] = dims(sameAspect ?? "0x0");
  const scale = aw / sw;
  await expect(x.nth(0)).toHaveValue(String(Math.round(400 * scale)));
  await expect(x.nth(1)).toHaveValue(String(Math.round(1200 * scale)));
  await expect(y.nth(0)).toHaveValue(String(Math.round(200 * scale)));
  await expect(y.nth(1)).toHaveValue(String(Math.round(500 * scale)));
  await expect(page.getByText(/crop region was reset/i)).not.toBeVisible();

  // Different aspect ratio: reset + popup.
  const diffAspect = names.find((n) => !sameAspectOf(n));
  test.skip(!diffAspect, "no different-aspect mode on this camera");
  await pick(new RegExp((diffAspect ?? "").split(" ")[0]));
  const [dw, dh] = dims(diffAspect ?? "0x0");
  await expect(x.nth(0)).toHaveValue("0");
  await expect(x.nth(1)).toHaveValue(String(dw));
  await expect(y.nth(0)).toHaveValue("0");
  await expect(y.nth(1)).toHaveValue(String(dh));
  await expect(page.getByText(/crop region was reset/i)).toBeVisible();
});
