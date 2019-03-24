import VueRouter from "vue-router";
import Input from "./components/InputTab.vue";
import Threshold from "./components/ThresholdTab.vue";
import System from "./components/SystemTab.vue";
import Camera from "./components/CameraTab.vue";
import Contours from "./components/contourTab.vue";

const routes = [
  { path: '/', redirect: '/vision/input'},
  { path: '/vision/input', component: Input, name:'input' },
  { path: '/vision/threshold', component: Threshold ,name:'threshold'},
  {path:'/settings/system', component: System },
  {path:'/settings/camera', component: Camera},
  {path:'/settings/contours', component: Contours}
]

const router = new VueRouter({
  routes // short for `routes: routes`
})

export default router;