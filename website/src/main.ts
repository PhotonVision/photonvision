import { ViteSSG } from "vite-ssg/single-page";
import App from "./App.vue";
import "./css/main.css";

export const createApp = ViteSSG(App);
