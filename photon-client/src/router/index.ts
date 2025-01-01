import Vue from "vue";
import VueRouter from "vue-router";

import DashboardView from "@/views/DashboardView.vue";
import CameraSettingsView from "@/views/CameraSettingsView.vue";
import GeneralSettingsView from "@/views/GeneralSettingsView.vue";
import DocsView from "@/views/DocsView.vue";
import NotFoundView from "@/views/NotFoundView.vue";
import CameraMatchingView from "@/views/CameraMatchingView.vue";

Vue.use(VueRouter);

const router = new VueRouter({
  // Using HTML5 History Mode is problematic with Javalin because each route is treated as a server endpoint which causes Javalin to return a 404 error before being redirected to the UI.
  // mode: "history",
  base: import.meta.env.BASE_URL,
  routes: [
    {
      path: "/",
      redirect: "/dashboard"
    },
    {
      path: "/dashboard",
      name: "Dashboard",
      component: DashboardView
    },
    {
      path: "/cameras",
      name: "Cameras",
      component: CameraSettingsView
    },
    {
      path: "/settings",
      name: "Settings",
      component: GeneralSettingsView
    },
    {
      path: "/cameraConfigs",
      name: "Camera Matching",
      component: CameraMatchingView
    },
    {
      path: "/docs",
      name: "Docs",
      component: DocsView
    },
    {
      path: "*",
      name: "NotFound",
      component: NotFoundView
    }
  ]
});

export default router;
