import vuetify from "@/plugins/vuetify";
import pinia from "@/stores";
import router from "@/router";

import type { App } from "vue";

export function registerPlugins(app: App) {
  app.use(vuetify).use(router).use(pinia);
}
