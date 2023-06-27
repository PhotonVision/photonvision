import Vue from "vue";
import App from "@/App.vue";

import { createPinia, PiniaVuePlugin } from "pinia";
import router from "@/router";
import vuetify from "@/plugins/vuetify";

// Handle Plugins
Vue.use(PiniaVuePlugin);

new Vue({
  router,
  vuetify,
  pinia: createPinia(),
  provide: {
    backendAddress: process.env.NODE_ENV === "production" ? location.host : location.hostname + ":5800"
  },
  render: (h) => h(App)
}).$mount("#app");
