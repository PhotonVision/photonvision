import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import vuetify from './plugins/vuetify';
import VueNativeSock from 'vue-native-websocket';
Vue.config.productionTip = false
// Vue.use(VueNativeSock,'ws://' + location.hostname + ':8888/websocket',{format:'JSON'});
new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
}).$mount('#app')
