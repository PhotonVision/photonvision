import Vue from "vue";
import VueRouter from "vue-router";

import DashboardView from "@/views/DashboardView.vue";
import CameraSettingsView from "@/views/CameraSettingsView.vue";
import GeneralSettingsView from "@/views/GeneralSettingsView.vue";
import DocsView from "@/views/DocsView.vue";
import NotFoundView from "@/views/NotFoundView.vue";

Vue.use(VueRouter);

const router = new VueRouter({
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
