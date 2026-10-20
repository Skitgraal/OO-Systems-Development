import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// In dev, /api calls are forwarded to the Spring Boot REST service.
// API_PORT overrides its port (set by start.bat when 8080 is already taken).
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': `http://localhost:${process.env.API_PORT || 8080}`
    }
  }
})
