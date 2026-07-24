import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";
import Components from "unplugin-vue-components/vite";
import Icons from "unplugin-icons/vite";
import IconsResolver from "unplugin-icons/resolver";

export default defineConfig({
  server: {
    allowedHosts: ["drawn-signing-literacy-finished.trycloudflare.com"]
  },
  base: "./",
  plugins: [tailwindcss(), Components({
    dts: true,
    // enabled by default if `typescript` is installed
    resolvers: [IconsResolver({
      prefix: "icon"
    })]
  }), Icons({
    compiler: "vue3"
  }), vue()],
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
