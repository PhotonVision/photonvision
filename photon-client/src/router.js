import Vue from 'vue'
import Router from 'vue-router'
import Dashboard from "./views/PipelineView";
import Settings from "./views/SettingsView";
import Docs from "./views/DocsView";
Vue.use(Router);

export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [{
        path: '/',
        redirect: '/dashboard'
    }, {
        path: '/dashboard',
        name: 'Dashboard',
        component: Dashboard
    }, {
        path: '/settings',
        name: 'Settings',
        component: Settings
    }, {
        path: '/docs',
        name: 'Docs',
        component: Docs
    }]
})
