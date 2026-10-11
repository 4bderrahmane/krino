/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Absolute path to ./src, derived from this file's location so we don't need
// @types/node just to set up the alias. import.meta.url is a file:// URL;
// strip the scheme and the filename to get the project directory (POSIX paths).
// import.meta.url is cast because this file is type-checked by tsconfig.node.json,
// which has no DOM/node libs to type ImportMeta.
const metaUrl = (import.meta as unknown as { url: string }).url
const srcDir =
  decodeURIComponent(metaUrl.replace(/^file:\/\//, '')).replace(/\/vite\.config\.ts$/, '') + '/src'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': srcDir,
    },
  },
  // Dev mirrors production: nginx reverse-proxies /api/ to the backend so the SPA
  // and the API share one origin (see client/nginx.conf), and the client asks for
  // "/api/..." relative to wherever it is served. Proxying the same prefix here
  // keeps development on that same shape instead of talking cross-origin to
  // :8080, which is a path production never takes.
  server: {
    port: 5000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
      },
    },
  },
  preview: {
    port: 5000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
      },
    },
  },
  test: {
    // Tests live next to the code they cover (*.test.ts / *.test.tsx).
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // No global describe/it/expect: every test imports what it uses, so the
    // provenance is visible and no extra tsconfig types are needed.
    globals: false,
    // Components import .css for side effects only; parsing it buys nothing here.
    css: false,
    restoreMocks: true,
  },
})
