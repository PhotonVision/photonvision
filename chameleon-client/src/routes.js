import VueRouter from "vue-router";
import Vision from "./components/Vision.vue"
import Setting from "./components/Settings.vue"
import Input from "./components/InputTab.vue";
import Threshold from "./components/ThresholdTab.vue";
import System from "./components/SystemTab.vue";
import Camera from "./components/CameraTab.vue";
import Contours from "./components/contourTab.vue";

const routes = [
  { path: '/', redirect: '/vision/input'},
  { path: '/vision', component: Vision, children: [
    { path: 'input', component: Input },
    { path: 'threshold', component: Threshold },
    { path: 'contours', component: Contours }
  ]},
  { path: '/settings', component: Setting, children: [
    { path: 'system', component: System },
    { path: 'camera', component: Camera }
  ]}
]

const router = new VueRouter({
  routes
})

export default router;