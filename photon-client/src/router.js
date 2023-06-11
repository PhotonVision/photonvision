import Vue from 'vue'
import VueRouter from "vue-router";

import Dashboard from "./views/PipelineView";
import Cameras from "./views/CamerasView";
import Settings from "./views/SettingsView";
import Docs from "./views/DocsView";
import NotFoundView from "./views/NotFoundView";


Vue.use(VueRouter);

export default new VueRouter({
    mode: 'history',
    base: process.env.BASE_URL,
    routes: [
        {
            path: '/',
            redirect: '/dashboard'
        },
        {
            path: '/dashboard',
            name: 'Dashboard',
            component: Dashboard
        },
        {
            path: '/cameras',
            name: 'Cameras',
            component: Cameras
        },
        {
            path: '/settings',
            name: 'Settings',
            component: Settings
        },
        {
            path: '/docs',
            name: 'Docs',
            component: Docs
        },
        {
            path: "*",
            name: "NotFound",
            component: NotFoundView
        }
    ]
})
