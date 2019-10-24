import Vue from 'vue'
import Router from 'vue-router'
Vue.use(Router)

function lazyLoad(view){
  return() => import(`@/views/${view}.vue`)
}
export default new Router({
  // mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/',
      redirect:'/vision'
    },
    {
      path: '/vision',
      name: 'Vision',
      component: lazyLoad('Camera')
    },
    {
      path: '/settings',
      name: 'Settings',
      component: lazyLoad('Settings')
    }
  ]
})
