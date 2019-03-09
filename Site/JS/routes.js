// need to import templates from files
const Vision = { template: '<div id="vision"><nav class="nav"><router-link to="/input"><a>Input</a></router-link></nav><router-view></router-view></div>' }
const Input = { template: '<p>Input page</p>' }
const Setting = { template: '<p>Settings page</p>' }

const routes = [
  { path: '/', component: Vision , children: [
    { path: 'input', component: Input }
  ]},
  { path: '/vision', component: Vision , children: [
    { path: 'input', component: Input }
  ]},
  { path: '/settings', component: Setting }
]

const router = new VueRouter({
  routes
})

const app = new Vue({
  router
}).$mount('#app')