import { defineConfig, devices } from '@playwright/test';

const fullMatrix = process.env.PW_FULL_MATRIX === 'true';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'test-results/e2e/results.xml' }]
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://127.0.0.1:18090',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: fullMatrix
    ? [
        {
          name: 'chromium',
          use: { ...devices['Desktop Chrome'], channel: 'chrome' },
        },
        {
          name: 'firefox',
          use: { ...devices['Desktop Firefox'] },
        },
        {
          name: 'webkit',
          use: { ...devices['Desktop Safari'] },
        },
        {
          name: 'Mobile Chrome',
          use: { ...devices['Pixel 5'] },
        },
        {
          name: 'Mobile Safari',
          use: { ...devices['iPhone 12'] },
        },
      ]
    : [
        {
          name: 'chromium',
          use: { ...devices['Desktop Chrome'], channel: 'chrome' },
        },
      ],
  outputDir: 'test-results/',
});
