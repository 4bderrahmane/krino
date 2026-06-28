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
  server: {
    port: 5000,
  },
  preview: {
    port: 5000,
  },
})
