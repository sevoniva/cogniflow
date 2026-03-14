import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:19091'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 8080,
      open: true,
      watch: {
        ignored: [
          '**/coverage/**',
          '**/test-results/**',
          '**/playwright-report/**'
        ]
      },
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    },
    build: {
      // 代码分割配置
      rollupOptions: {
        output: {
          // 手动分块策略
          manualChunks: {
            // Element Plus 单独打包
            'element-plus': ['element-plus'],
            // Vue 生态单独打包
            'vue-vendor': ['vue', 'vue-router']
          },
          //  chunk 文件命名
          chunkFileNames: 'assets/js/[name]-[hash].js',
          entryFileNames: 'assets/js/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            const info = assetInfo.name || ''
            if (info.endsWith('.css')) {
              return 'assets/css/[name]-[hash][extname]'
            }
            return 'assets/[name]-[hash][extname]'
          }
        }
      },
      // 压缩配置
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true
        }
      },
      // 源码映射（生产环境关闭）
      sourcemap: false,
      // 报告压缩后大小
      reportCompressedSize: true
    }
  }
})
