import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import vuetify from './plugins/vuetify';
import VueNativeSock from 'vue-native-websocket';
import msgPack from 'msgpack5';
import axios from 'axios';
import VueAxios from "vue-axios";

Vue.config.productionTip = false;

if (process.env.NODE_ENV === "production"){
    Vue.prototype.$address = location.host;
} else if (process.env.NODE_ENV === "development"){
    Vue.prototype.$address = location.hostname + ":5800";
}

const url = 'ws://' + Vue.prototype.$address + '/websocket';
var ws = new WebSocket(url);
ws.binaryType = "arraybuffer";

Vue.use(VueNativeSock, url,{
    WebSocket: ws
});
Vue.use(VueAxios, axios);
Vue.prototype.$msgPack = msgPack(true);

Vue.mixin({
    methods: {
        handleInput(key, value) {
            let msg = this.$msgPack.encode({[key]: value});
            this.$socket.send(msg);
        }
    }
});
new Vue({
    router,
    store,
    vuetify,
    render: h => h(App)
}).$mount('#app');
