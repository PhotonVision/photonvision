import Vue from 'vue'
import Router from 'vue-router'
Vue.use(Router)

function lazyLoad(view){
  return() => import(`@/views/${view}.vue`)
}
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
      component: lazyLoad('Camera')
    },
    {
      path: '/Settings',
      name: 'Settings',
      component: lazyLoad('Settings')
    }
  ]
})
