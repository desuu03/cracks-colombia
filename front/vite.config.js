import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // The backend has no CORS configuration, so the browser talks to the Vite
  // server and Vite forwards /api/* to Spring Boot without the prefix.
  const proxy = {
    '/api': {
      target: env.API_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
    },
  }
  return {
    plugins: [vue()],
    server: { proxy },
    preview: { proxy },
  }
})
