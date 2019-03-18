import VueRouter from "vue-router";
import Input from "./components/InputTab.vue";
import ThreeD from "./components/3DTab.vue";
import System from "./components/SystemTab.vue";
import Camera from "./components/CameraTab.vue";

const routes = [
  { path: '/', redirect: '/vision/input'},
  { path: '/vision/input', component: Input, name:'input' },
  { path: '/vision/3d', component: ThreeD ,name:'threshold'},
  {path:'/settings/system', component: System },
  {path:'/settings/camera', component: Camera}
]

const router = new VueRouter({
  routes // short for `routes: routes`
})

export default router;