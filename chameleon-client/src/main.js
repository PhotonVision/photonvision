import Vue from 'vue'
import App from './App.vue'
import VueRouter from 'vue-router'
import iView from 'iview';
import router from "./routes";
import '../theme/index.less';

Vue.use(VueRouter);
Vue.use(iView);

Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
