import Vue from 'vue'
import App from './App.vue'
import VueRouter from 'vue-router'
import iView from 'iview';
import router from "./routes";
import '../theme/index.less';
import VueNativeSock from 'vue-native-websocket'

Vue.use(VueRouter);
Vue.use(iView);
Vue.use(VueNativeSock,'ws://'+location.hostname+':8888/websocket');
Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
