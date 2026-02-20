import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(async ({ mode }) => {
  const plugins = [vue()]

  if (mode === 'development') {
<<<<<<< HEAD
    const vueDevTools = (await import('vite-plugin-vue-devtools')).default
    plugins.push(vueDevTools())
=======
    try {
      const vueDevTools = (await import('vite-plugin-vue-devtools')).default
      plugins.push(vueDevTools())
    } catch (e) {
      console.warn('vite-plugin-vue-devtools non trovato, salto plugin')
    }
>>>>>>> dev
  }

  return {
    plugins,
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
  }
})
