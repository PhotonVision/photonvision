import VueRouter from "vue-router";
import Input from "./components/InputTab.vue";
import ThreeD from "./components/3DTab.vue";

const routes = [
  { path: '/', redirect: '/input' },
  { path: '/input', component: Input },
  { path: '/3d', component: ThreeD }
]

const router = new VueRouter({
  routes // short for `routes: routes`
})

export default router;