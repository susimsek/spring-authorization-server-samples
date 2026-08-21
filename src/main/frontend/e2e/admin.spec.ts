import { expect, test } from "@playwright/test";

async function signIn(page: import("@playwright/test").Page) {
  await page.goto("/en/admin/");
  await expect(page.locator('input[name="username"]')).toBeVisible({ timeout: 15_000 });
  await page.locator('input[name="username"]').fill("admin");
  await page.locator('input[name="password"]').fill("admin");
  await page.locator('button[type="submit"]').click();
  await expect(page.locator(".admin-sidebar")).toBeVisible({ timeout: 20_000 });
}

test("admin console table, form and modal flows", async ({ page }) => {
  await signIn(page);

  for (const label of ["Clients", "Users", "Sessions", "Consents", "Keys"]) {
    await page.getByRole("link", { name: label }).click();
    const search = page.getByRole("textbox", { name: "Search" });
    await expect(search).toBeVisible();
    await search.fill("admin");
    await expect(page).toHaveURL(/\?q=admin/, { timeout: 2_000 });
  }

  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("link", { name: "Clients" }).click();
  await expect(page.locator(".admin-data-table td[data-label]").first()).toBeVisible();
  await expect(page.locator(".admin-data-table thead")).toBeHidden();
  await page.setViewportSize({ width: 1280, height: 900 });

  await page.getByRole("link", { name: "Clients" }).click();
  await page.getByRole("link", { name: "Create client" }).click();
  await page.locator('button[type="submit"]').click();
  await expect(page.getByText("This field is required.").first()).toBeVisible();

  const clientId = `e2e-client-${Date.now()}`;
  await page.locator('input[name="clientId"]').fill(clientId);
  await page.locator('input[name="clientName"]').fill("Playwright E2E client");
  await page.locator('textarea[name="redirectUris"]').fill("https://example.test/callback");
  await page.locator('button[type="submit"]').click();

  const result = page.getByRole("dialog");
  await expect(result.getByText("New client secret")).toBeVisible();
  await expect(result.locator(".font-monospace")).toContainText(/.+/);
  await result.locator(".modal-footer button").click();

  await page.waitForURL(/\/en\/admin\/clients\/detail\/\?id=/);
  await page.getByRole("button", { name: "Delete" }).click();
  await expect(page.getByRole("dialog").getByText("Delete this client?")).toBeVisible();
  await page.getByRole("dialog").getByRole("button", { name: "Delete" }).click();
  await page.waitForURL(/\/en\/admin\/clients\/?$/);
});
