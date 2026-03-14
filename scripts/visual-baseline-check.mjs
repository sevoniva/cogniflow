#!/usr/bin/env node

import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { chromium } from 'playwright'

const baseUrl = process.env.VISUAL_BASELINE_BASE_URL || process.env.BASE_URL || 'http://127.0.0.1:18090'
const updateMode = process.env.VISUAL_BASELINE_UPDATE === '1'
const tolerance = Number(process.env.VISUAL_BASELINE_LAYOUT_TOLERANCE || 16)
const baselineDir = path.resolve(process.cwd(), 'e2e/visual-baseline')

const pages = [
  {
    name: 'home',
    path: '/',
    selectors: ['.home-page', '.search-box', '.hero-title', '.hero-subtitle']
  },
  {
    name: 'conversation',
    path: '/chatbi/conversation',
    selectors: ['.workspace-card', '.composer', '.messages-shell']
  },
  {
    name: 'advanced-charts',
    path: '/chatbi/advanced-charts',
    selectors: ['.advanced-charts-page', '.market-grid', '.validation-table']
  },
  {
    name: 'admin-ai',
    path: '/admin/ai',
    selectors: ['.ai-admin-page', '.hero-grid', '.runtime-panel'],
    ignoreBodyHeight: true,
    dynamicHeightSelectors: ['.ai-admin-page']
  },
  {
    name: 'admin-permission',
    path: '/admin/permission',
    selectors: ['.permission-page', '.hero-card', '.panel-card']
  }
]

function toInt(value) {
  return Math.round(Number(value || 0))
}

function approxEqual(left, right, maxDiff = tolerance) {
  return Math.abs((left ?? 0) - (right ?? 0)) <= maxDiff
}

async function captureSnapshot(page, item) {
  await page.goto(`${baseUrl}${item.path}`, { waitUntil: 'networkidle', timeout: 30000 })
  const title = await page.title()
  const viewport = page.viewportSize() || { width: 1440, height: 900 }

  const bodyMetrics = await page.evaluate(() => ({
    scrollWidth: document.body.scrollWidth,
    scrollHeight: document.body.scrollHeight
  }))

  const selectorMetrics = []
  for (const selector of item.selectors) {
    const locator = page.locator(selector).first()
    const count = await locator.count()
    if (!count) {
      selectorMetrics.push({ selector, exists: false })
      continue
    }
    const box = await locator.boundingBox()
    selectorMetrics.push({
      selector,
      exists: true,
      rect: {
        x: toInt(box?.x),
        y: toInt(box?.y),
        width: toInt(box?.width),
        height: toInt(box?.height)
      }
    })
  }

  return {
    name: item.name,
    path: item.path,
    title,
    viewport,
    bodyMetrics: {
      scrollWidth: toInt(bodyMetrics.scrollWidth),
      scrollHeight: toInt(bodyMetrics.scrollHeight)
    },
    selectors: selectorMetrics
  }
}

function compareSnapshot(current, baseline, pageConfig = {}) {
  const failures = []
  if (current.title !== baseline.title) {
    failures.push(`title mismatch: current="${current.title}" baseline="${baseline.title}"`)
  }

  if (!approxEqual(current.bodyMetrics.scrollWidth, baseline.bodyMetrics.scrollWidth)) {
    failures.push(`body.scrollWidth mismatch: ${current.bodyMetrics.scrollWidth} vs ${baseline.bodyMetrics.scrollWidth}`)
  }
  if (!pageConfig.ignoreBodyHeight
    && !approxEqual(current.bodyMetrics.scrollHeight, baseline.bodyMetrics.scrollHeight, tolerance * 2)) {
    failures.push(`body.scrollHeight mismatch: ${current.bodyMetrics.scrollHeight} vs ${baseline.bodyMetrics.scrollHeight}`)
  }

  const bySelector = new Map(baseline.selectors.map(item => [item.selector, item]))
  const dynamicHeightSelectors = new Set(pageConfig.dynamicHeightSelectors || [])
  for (const metric of current.selectors) {
    const base = bySelector.get(metric.selector)
    if (!base) {
      failures.push(`selector ${metric.selector} missing in baseline`)
      continue
    }
    if (Boolean(metric.exists) !== Boolean(base.exists)) {
      failures.push(`selector ${metric.selector} exists mismatch: ${metric.exists} vs ${base.exists}`)
      continue
    }
    if (!metric.exists || !base.exists) {
      continue
    }
    for (const key of ['x', 'y', 'width', 'height']) {
      if (key === 'height' && dynamicHeightSelectors.has(metric.selector)) {
        continue
      }
      if (!approxEqual(metric.rect?.[key], base.rect?.[key])) {
        failures.push(`selector ${metric.selector} rect.${key} mismatch: ${metric.rect?.[key]} vs ${base.rect?.[key]}`)
      }
    }
  }
  return failures
}

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true })
}

async function readJson(file) {
  const content = await fs.readFile(file, 'utf-8')
  return JSON.parse(content)
}

async function writeJson(file, data) {
  await fs.writeFile(file, `${JSON.stringify(data, null, 2)}\n`, 'utf-8')
}

async function main() {
  await ensureDir(baselineDir)
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1
  })
  const page = await context.newPage()

  const errors = []
  for (const item of pages) {
    const snapshot = await captureSnapshot(page, item)
    const file = path.join(baselineDir, `${item.name}.json`)
    if (updateMode) {
      await writeJson(file, snapshot)
      console.log(`[VISUAL] baseline updated: ${item.name}`)
      continue
    }

    try {
      const baseline = await readJson(file)
      const failures = compareSnapshot(snapshot, baseline, item)
      if (failures.length) {
        errors.push(`${item.name}: ${failures.join('; ')}`)
      } else {
        console.log(`[VISUAL] baseline passed: ${item.name}`)
      }
    } catch (error) {
      errors.push(`${item.name}: baseline missing or invalid (${error.message})`)
    }
  }

  await context.close()
  await browser.close()

  if (errors.length) {
    console.error('[VISUAL] baseline check failed')
    for (const item of errors) {
      console.error(`- ${item}`)
    }
    process.exit(1)
  }

  console.log('[VISUAL] baseline check passed')
}

main().catch(error => {
  console.error('[VISUAL] fatal:', error)
  process.exit(1)
})
