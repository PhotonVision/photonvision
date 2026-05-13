import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";
import vuetify from "vite-plugin-vuetify";

export default defineConfig({
  server: {
    allowedHosts: ["drawn-signing-literacy-finished.trycloudflare.com"]
  },
  base: "./",
  plugins: [
    tailwindcss(),
    vue(),
    vuetify({
      styles: {
        configFile: "src/assets/styles/settings.scss"
      }
    })
  ],
  css: {
    preprocessorOptions: {
      sass: {}
    }
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    }
  },
  build: {
    rolldownOptions: {
      external: ["html2canvas", "dompurify", "canvg"]
    },
    sourcemap: true
  }
});
