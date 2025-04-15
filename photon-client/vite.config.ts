import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import Vue from "@vitejs/plugin-vue";
import Components from "unplugin-vue-components/vite";
import { Vuetify3Resolver } from "unplugin-vue-components/resolvers";

export default defineConfig({
  base: "./",
  plugins: [
    Vue(),
    Components({
      resolvers: [Vuetify3Resolver()],
      dts: true,
      transformer: "vue3",
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
