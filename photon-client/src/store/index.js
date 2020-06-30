import Vue from 'vue'
import Vuex from 'vuex'

import networkSettings from "./modules/networkSettings"
import undoRedo from "./modules/undoRedo";

Vue.use(Vuex);

const set = key => (state, val) => {
    Vue.set(state, key, val);
};

export default new Vuex.Store({
    modules: {
        networkSettings: networkSettings,
        undoRedo: undoRedo
    },
    state: {
        currentCameraIndex: 0,
        saveBar: false,
        cameraSettings: [ // This is a list of objects representing the settings of all cameras
            {
                tiltDegrees: 0.0,
                currentPipelineIndex: 0,
                pipelineNicknames: ["Unknown"],
                streamPort: 1181,
                nickname: "Unknown",
                resolutionList: [],
                fov: 70.0,
                currentPipelineSettings: {}
            }
        ],
        pipelineResults: [
            // {
            //     fps: 254,
            //     targets: [{
            //         pitch: 0,
            //         yaw: 1,
            //         skew: 2,
            //         pose: {x: 1, y: 2, rot: 4},
            //     }]
            // }
        ]
    },
    mutations: {
        cameraSettings: set('cameraSettings'),
        saveBar: set('saveBar'),
        currentCameraIndex: set('currentCameraIndex'),
        pipelineResults: set('pipelineResults')
    },
    getters: {
        streamAddress: state => {
            return "http://" + location.hostname + ":" + state.cameraSettings[state.currentCameraIndex].streamPort + "/stream.mjpg";
        },
        targets: state => {
            return state.pipelineResults.length
        },
        cameraList: state => {
            return state.cameraSettings.map(it => it.nickname)
        },
        pipelineList: state => {
            return state.pipelineList
        },
        currentCameraIndex: state => {
            return state.currentCameraIndex
        },
        currentPipelineIndex: state => {
            return state.cameraSettings[state.currentCameraIndex].currentPipelineIndex
        },
        resolutionList: state => {
            return state.cameraSettings[state.currentCameraIndex].resolutionList
        }
    }
})
