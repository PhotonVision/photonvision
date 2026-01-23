import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vuetify from "vite-plugin-vuetify";

export default defineConfig({
  base: "./",
  plugins: [
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
    rollupOptions: {
      external: ["html2canvas", "dompurify", "canvg"]
    },
    sourcemap: true
  }
});
