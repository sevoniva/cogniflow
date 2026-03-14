import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'test-results/**',
        'playwright-report/**',
        'playwright.config.ts',
        'vitest.enterprise.config.ts',
        'scripts/**',
        'src/test/',
        'src/main.ts',
        'src/router/**',
        'src/adapters/**',
        'src/store/**',
        'src/stores/**',
        'src/views/**',
        '**/*.d.ts',
        '**/*.vue',
        '**/mocks/**'
      ],
      thresholds: {
        lines: 70,
        functions: 70,
        branches: 70,
        statements: 70
      }
    },
    include: ['src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
    reporters: ['default', 'html'],
    outputFile: {
      html: 'test-results/html/index.html'
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
