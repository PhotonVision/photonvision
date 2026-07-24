import { expect } from "@playwright/test";
import { test } from "./fixtures.ts";

test("Brightness Slider won't go past max or min", async ({ page }) => {
  await page.goto("http://localhost:5800/#/dashboard");

  const brightnessInput = page.getByRole("spinbutton", { name: "Brightness value" });
  const increaseBrightness = page.getByRole("button", { name: "Increase Brightness value" });
  const decreaseBrightness = page.getByRole("button", { name: "Decrease Brightness value" });

  // Fill in Brightness text field with 1000
  await brightnessInput.fill("1000");
  await brightnessInput.press("Enter");
  await expect(brightnessInput).toHaveValue("100");

  // Try using buttons to go past the max
  await increaseBrightness.click();
  await expect(brightnessInput).toHaveValue("100");

  // Make sure the value is actually properly limited, not just visually
  await decreaseBrightness.click();
  await expect(brightnessInput).toHaveValue("99");

  await brightnessInput.fill("-10");
  await brightnessInput.press("Enter");
  await expect(brightnessInput).toHaveValue("0");

  await decreaseBrightness.click();
  await expect(brightnessInput).toHaveValue("0");

  // Make sure the value is actually properly limited, not just visually
  await increaseBrightness.click();
  await expect(brightnessInput).toHaveValue("1");

  await decreaseBrightness.click();
  await decreaseBrightness.click();
  await expect(brightnessInput).toHaveValue("0");

  await expect(page.getByText("Brightness", { exact: true })).toBeVisible();
});

test("Camera Gain Slider won't go past max or min", async ({ page }) => {
  await page.goto("http://localhost:5800/#/dashboard");

  const cameraGainInput = page.getByRole("spinbutton", { name: "Camera Gain value" });
  const increaseCameraGain = page.getByRole("button", { name: "Increase Camera Gain value" });
  const decreaseCameraGain = page.getByRole("button", { name: "Decrease Camera Gain value" });

  await cameraGainInput.fill("1000");
  await cameraGainInput.press("Enter");
  await expect(cameraGainInput).toHaveValue("100");

  await increaseCameraGain.click();
  await expect(cameraGainInput).toHaveValue("100");

  await decreaseCameraGain.click();
  await expect(cameraGainInput).toHaveValue("99");

  await cameraGainInput.fill("-10");
  await cameraGainInput.press("Enter");
  await expect(cameraGainInput).toHaveValue("0");

  await decreaseCameraGain.click();
  await expect(cameraGainInput).toHaveValue("0");

  await increaseCameraGain.click();
  await expect(cameraGainInput).toHaveValue("1");

  await decreaseCameraGain.click();
  await decreaseCameraGain.click();
  await expect(cameraGainInput).toHaveValue("0");
});
