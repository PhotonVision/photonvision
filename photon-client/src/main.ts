import Vue from "vue";
import App from "@/App.vue";

import { createPinia, PiniaVuePlugin } from "pinia";
import router from "@/router";
import vuetify from "@/plugins/vuetify";
import axios from "axios";

// Handle Plugins
Vue.use(PiniaVuePlugin);

const backendAddress = process.env.NODE_ENV === "production"
    ? location.host
    : location.hostname + ":5800";
axios.defaults.baseURL = `http://${backendAddress}/api`;

new Vue({
  router,
  vuetify,
  pinia: createPinia(),
  provide: {
    backendAddress
  },
  render: (h) => h(App)
}).$mount("#app");
