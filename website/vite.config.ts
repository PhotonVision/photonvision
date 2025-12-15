import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: {
    allowedHosts: ["dictionaries-motorcycle-regular-years.trycloudflare.com"],
  },
});
