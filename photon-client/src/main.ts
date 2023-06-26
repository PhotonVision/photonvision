import Vue from "vue";

import App from "./App.vue";
import router from "./router";

import "./assets/style/main.css";

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
