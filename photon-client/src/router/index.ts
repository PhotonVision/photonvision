import { createRouter, createWebHistory } from "vue-router/auto";
import { routes } from "vue-router/auto-routes";

const router = createRouter({
  // Using HTML5 History Mode is problematic with Javalin because each route is treated as a server endpoint which causes Javalin to return a 404 error before being redirected to the UI.
  // history: createWebHashHistory(),
  history: createWebHistory(),
  routes: routes
});

router.addRoute({ path: "/", redirect: "/dashboard" });
router.addRoute({ path: "/:pathMatch(.*)*", redirect: "/not_found" });

// Workaround for https://github.com/vitejs/vite/issues/11804
router.onError((err, to) => {
  if (err?.message?.includes?.("Failed to fetch dynamically imported module")) {
    if (!localStorage.getItem("vuetify:dynamic-reload")) {
      console.log("Reloading page to fix dynamic import error");
      localStorage.setItem("vuetify:dynamic-reload", "true");
      location.assign(to.fullPath);
    } else {
      console.error("Dynamic import error, reloading page did not fix it", err);
    }
  } else {
    console.error(err);
  }
});

router.isReady().then(() => {
  localStorage.removeItem("vuetify:dynamic-reload");
});

export default router;
