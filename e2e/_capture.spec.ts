import { test } from '@playwright/test'

test('capture pages', async ({ page }) => {
  const cases = [
    ['home', '/'],
    ['admin', '/admin'],
    ['sales', '/sales-dashboard'],
    ['operation', '/operation-dashboard'],
    ['agile', '/agile-dashboard'],
    ['result', '/chatbi/result?query=%E6%9C%AC%E6%9C%88%E9%94%80%E5%94%AE%E9%A2%9D']
  ] as const

  for (const [name, path] of cases) {
    await page.goto(path, { waitUntil: 'domcontentloaded' })
    await page.screenshot({ path: `test-results/${name}.png`, fullPage: true })
  }
})
