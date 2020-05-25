import Vue from 'vue'
import Router from 'vue-router'
import Camera from "./views/PipelineView";
import Settings from "./views/SettingsView";
Vue.use(Router);


export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [{
        path: '/',
        redirect: '/vision'
    }, {
        path: '/vision',
        name: 'Vision',
        component: Camera
    }, {
        path: '/settings',
        name: 'Settings',
        component: Settings
    }]
})
