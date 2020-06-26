import Vue from 'vue'
import Vuex from 'vuex'

import pipeline from "./modules/pipeline";
import generalSettings from "./modules/generalSettings";
import cameraSettings from "./modules/cameraSettings";
import undoRedo from "./modules/undoRedo";

Vue.use(Vuex);

const set = key => (state, val) => {
    Vue.set(state, key, val);
};

export default new Vuex.Store({
    modules: {
        pipeline: pipeline,
        settings: generalSettings,
        cameraSettings: cameraSettings,
        undoRedo: undoRedo
    },
    state: {
        resolutionList: [],
        port: 1181,
        currentCameraIndex: 0,
        currentPipelineIndex: 0,
        cameraList: [],
        pipelineList: [],
        point: {},
        saveBar: false
    },
    mutations: {
        settings: set('settings'),
        pipeline: set('pipeline'),
        cameraSettings: set('cameraSettings'),
        resolutionList: set('resolutionList'),
        port: set('port'),
        currentCameraIndex: set('currentCameraIndex'),
        currentPipelineIndex: set('currentPipelineIndex'),
        cameraList: set('cameraList'),
        pipelineList: set('pipelineList'),
        point: set('point'),
        driverMode: set('driverMode'),
        saveBar: set("saveBar")
    },
    getters: {
        streamAddress: state => {
            return "http://" + location.hostname + ":" + state.port + "/stream.mjpg";
        },
        targets: state => {
            return state.point['targets']
        },
        cameraList: state => {
            return state.cameraList
        },
        pipelineList: state => {
            return state.pipelineList
        },
        currentCameraIndex: state => {
            return state.currentCameraIndex
        },
        currentPipelineIndex: state => {
            return state.currentPipelineIndex
        }
    }
})
