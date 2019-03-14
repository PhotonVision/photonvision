import Vue from 'vue'
import App from './App.vue'
import VueRouter from 'vue-router'
import iView from 'iview';
//import 'iview/dist/styles/iview.css'
import '../theme/index.less';

Vue.use(VueRouter);
Vue.use(iView);

Vue.config.productionTip = false

new Vue({
  render: h => h(App)
}).$mount('#app')
