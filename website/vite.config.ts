import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  publicDir: "public",
  plugins: [vue(), tailwindcss()],
});
