import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import fs from 'fs'

function githubPagesSpa(): Plugin {
  return {
    name: 'github-pages-spa-fallback',
    closeBundle() {
      const distDir = path.resolve(import.meta.dirname, 'dist')
      const indexHtml = path.join(distDir, 'index.html')
      const fallbackHtml = path.join(distDir, '404.html')
      if (fs.existsSync(indexHtml)) {
        fs.copyFileSync(indexHtml, fallbackHtml)
      }
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  base: '/attendance-management-system/',
  plugins: [react(), githubPagesSpa()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
