import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import vuetify from './plugins/vuetify';
import VueNativeSock from 'vue-native-websocket';
import msgPack from 'msgpack5';

Vue.config.productionTip = false;
// Vue.use(VueNativeSock,'ws://' + location.host + '/websocket',{format: 'json'});
Vue.use(VueNativeSock,'ws://'+location.hostname+':8888/websocket');
Vue.prototype.$msgPack = msgPack;
Vue.mixin({
  methods:{
    handleInput(key,value){
      let msg = this.$msgPack().encode({key,value})
      this.$socket.send(msg);
    }
  }
})
new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
}).$mount('#app')
