import { h, createApp } from "vue";
import App from "@/App.vue";

import { createPinia } from "pinia";
import router from "@/router";
import vuetify from "@/plugins/vuetify";
import axios from "axios";

type PhotonClientRuntimeMode = "production" | "development" | "local-network-development";
const runtimeMode: PhotonClientRuntimeMode = process.env.NODE_ENV as PhotonClientRuntimeMode;

let backendHost: string;
let backendHostname: string;
switch (runtimeMode as PhotonClientRuntimeMode) {
  case "development":
    backendHost = `${location.hostname}:5800`;
    backendHostname = location.hostname;
    break;
  case "local-network-development":
    backendHost = "photonvision.local:5800";
    backendHostname = "photonvision.local";
    break;
  case "production":
    backendHost = location.host;
    backendHostname = location.hostname;
    break;
}

axios.defaults.baseURL = `http://${backendHost}/api`;

// Handle Plugins
const pinia = createPinia();

const app = createApp({
  router,
  vuetify,
  pinia: createPinia(),
  provide: {
    backendHost: backendHost,
    backendHostname: backendHostname
  },
  render: () => h(App)
});
app.use(pinia);
app.use(vuetify);
app.use(router);
app.mount("#app");
