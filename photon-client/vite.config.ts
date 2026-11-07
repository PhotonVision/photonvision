import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";
import Components from "unplugin-vue-components/vite";
import Icons from "unplugin-icons/vite";

export default defineConfig({
  base: "./",
  plugins: [
    tailwindcss(),
    Components({
      dts: true
    }),
    Icons({
      compiler: "vue3"
    }),
    vue()
  ],
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
