import Vue from 'vue'
import Router from 'vue-router'
import Camera from './views/Camera.vue'
import Settings from './views/Settings.vue'
Vue.use(Router)

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/',
      redirect:'/Vision'
    },
    {
      path: '/Vision',
      name: 'Vision',
      component: Camera
    },
    {
      path: '/Settings',
      name: 'Settings',
      component: Settings
    }
  ]
})
