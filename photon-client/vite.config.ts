import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import Vue2 from "@vitejs/plugin-vue2";
import Components from "unplugin-vue-components/vite";
import { VuetifyResolver } from "unplugin-vue-components/resolvers";

export default defineConfig({
  base: "./",
  plugins: [
    Vue2(),
    Components({
      resolvers: [VuetifyResolver()],
      dts: true,
      transformer: "vue2",
      types: [
        {
          from: "vue-router",
          names: ["RouterLink", "RouterView"]
        }
      ],
      version: 2.7
    })
  ],
  css: {
    preprocessorOptions: {
      sass: {
        additionalData: ["@import \"@/assets/styles/variables.scss\"", ""].join("\n")
      }
    }
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    }
  },
  build: {
    sourcemap: true
  }
});
