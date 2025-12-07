import { test, expect } from "@playwright/test";
import axios from "axios";

test("New Pipeline", async ({ page }) => {
  await axios.post("http://localhost:5800/api/test/resetBackend");
  await axios.post("http://localhost:5800/api/test/activateTestMode");
  await page.goto("http://localhost:5800/#/dashboard");
  await page.getByTestId("pipeline-menu").click();
  await page.getByTestId("add-pipeline").click();
  await page.getByRole("textbox", { name: "Pipeline Name" }).fill("Test Pipeline");
  await page.getByRole("dialog").getByText("AprilTag").click();
  await page.getByRole("option", { name: "Colored Shape" }).click();
  await page.getByRole("button", { name: "Create" }).click();
  await page.getByRole("combobox").nth(1).click();
  await page.getByRole("option", { name: "Test Pipeline" }).click();
  await expect(page.getByRole("main")).toContainText("Colored Shape");
});
