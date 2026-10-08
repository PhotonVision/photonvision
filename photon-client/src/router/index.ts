import { createRouter, createWebHistory } from "vue-router";

import DashboardView from "@/views/DashboardView.vue";
import CameraSettingsView from "@/views/CameraSettingsView.vue";
import GeneralSettingsView from "@/views/GeneralSettingsView.vue";
import DocsView from "@/views/DocsView.vue";
import NotFoundView from "@/views/NotFoundView.vue";
import CameraMatchingView from "@/views/CameraMatchingView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
      path: "/:pathMatch(.*)",
      name: "NotFound",
      component: NotFoundView
    }
  ]
});

export default router;
