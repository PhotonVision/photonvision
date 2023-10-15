import Vue from "vue";
import App from "@/App.vue";

import { createPinia, PiniaVuePlugin } from "pinia";
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
Vue.use(PiniaVuePlugin);

new Vue({
  router,
  vuetify,
  pinia: createPinia(),
  provide: {
    backendHost: backendHost,
    backendHostname: backendHostname
  },
  render: (h) => h(App)
}).$mount("#app");
