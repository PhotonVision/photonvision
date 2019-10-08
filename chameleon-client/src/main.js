import Vue from 'vue';
import App from './App.vue';
import VueRouter from 'vue-router';
import iView from 'iview';
import router from "./routes";
import '../theme/index.less';
import VueNativeSock from 'vue-native-websocket';
import locale from 'iview/dist/locale/en-US';
import {store} from './store';

Vue.use(VueRouter);
Vue.use(iView , { locale });
Vue.use(VueNativeSock,'ws://'+location.hostname+':8888/websocket',{format:'JSON'});
Vue.config.productionTip = false;

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
