import { defineConfig } from "cypress";

export default defineConfig({
  allowCypressEnv: false,
  e2e: {
    baseUrl: process.env.E2E_BASE_URL ?? "http://localhost:9090",
    specPattern: "cypress/e2e/**/*.cy.ts",
    supportFile: "cypress/support/e2e.ts",
    testIsolation: true,
    setupNodeEvents(on, config) {
      return config;
    },
  },
  defaultCommandTimeout: 10_000,
  pageLoadTimeout: 30_000,
  requestTimeout: 15_000,
  responseTimeout: 15_000,
  env: {
    adminUsername: process.env.E2E_ADMIN_USERNAME ?? "admin",
    adminPassword: process.env.E2E_ADMIN_PASSWORD ?? "admin",
  },
  retries: { runMode: 1, openMode: 0 },
  screenshotOnRunFailure: true,
  video: false,
  viewportWidth: 1280,
  viewportHeight: 900,
});
