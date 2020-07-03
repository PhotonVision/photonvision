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
        reflectivePipelineSettings: {
            state: {
                currentResolutionIndex: 0,
            },
        },
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
                videoFormatList: [
                    {
                        "width": 1920,
                        "height": 1080,
                        "fps": 30,
                        "pixelFormat": "BGR"
                    }
                ],
                fov: 70.0,
                calibrated: false,
                currentPipelineSettings: {
                    pipelineType: 2, // One of "driver", "reflective", "shape"
                    // 2 is reflective

                    // Settings that apply to all pipeline types
                    cameraExposure: 1,
                    cameraBrightness: 2,
                    cameraGain: 3,
                    inputImageRotationMode: 0,
                    cameraVideoModeIndex: 0,
                    outputFrameDivisor: 0,

                    // Settings that apply to reflective
                    hsvHue: [0, 15],
                    hsvSaturation: [0, 15],
                    hsvValue: [0, 25],
                    erode: false,
                    dilate: false,
                    contourArea: [0, 12],
                    contourRatio: [0, 12],
                    contourExtent: [0, 12],
                    contourSpecklePercentage: 5,
                    contourGroupingMode: 0,
                    contourIntersection: 0,
                    contourSortMode: 0,
                    outputShowMultipleTargets: false,
                    outputShowThresholded: 0,
                    offsetRobotOffsetMode: 0,
                    solvePNPEnabled: false,
                    targetRegion: 0,
                    contourTargetOrientation: 1

                    // Settings that apply to shape
                }
            }
        ],
        pipelineResults: [
            {
                fps: 0,
                targets: [{
                    // Available in both 2D and 3D
                    pitch: 0,
                    yaw: 0,
                    skew: 0,
                    area: 0,
                    // 3D only
                    pose: {x: 0, y: 0, rot: 0},
                }]
            }
        ]
    },
    mutations: {
        cameraSettings: set('cameraSettings'),
        saveBar: set('saveBar'),
        currentCameraIndex: set('currentCameraIndex'),
        pipelineResults: set('pipelineResults'),
        networkSettings: set('networkSettings'),

        currentPipelineIndex: (state, val) => state.cameraSettings[state.currentCameraIndex].currentPipelineIndex = val,

        // TODO change everything to use this
        mutatePipeline: (state, payload) => {
            for (let key in payload) {
                if (!payload.hasOwnProperty(key)) continue;
                const value = payload[key];
                const settings = state.cameraSettings[state.currentCameraIndex].currentPipelineSettings;
                if (settings.hasOwnProperty(key)) {
                    settings[key] = value;
                }
            }
        },

        mutatePipelineResults(state, payload) {
            // Key: index, value: result
            for (let key in payload) {
                if (!payload.hasOwnProperty(key)) continue;
                const index = parseInt(key);
                state.pipelineResults[index] = payload[key];
            }
        }
    },
    getters: {
        pipelineSettings: state => state.pipelineSettings,
        streamAddress: state =>
            "http://" + location.hostname + ":" + state.cameraSettings[state.currentCameraIndex].streamPort + "/stream.mjpg",
        targets: state => state.pipelineResults.length,
        currentPipelineResults: state =>
            state.pipelineResults[state.cameraSettings[state.currentCameraIndex].currentPipelineIndex],
        cameraList: state => state.cameraSettings.map(it => it.nickname),
        currentCameraSettings: state => state.cameraSettings[state.currentCameraIndex],
        currentCameraIndex: state => state.currentCameraIndex,
        currentPipelineIndex: state => state.cameraSettings[state.currentCameraIndex].currentPipelineIndex,
        currentPipelineSettings: state => state.cameraSettings[state.currentCameraIndex].currentPipelineSettings,
        videoFormatList: state => {
            return Object.values(state.cameraSettings[state.currentCameraIndex].videoFormatList); // convert to a list
        },
        pipelineList: state => state.cameraSettings[state.currentCameraIndex].pipelineNicknames,
        currentCameraFPS: state => state.pipelineResults[state.currentCameraIndex].fps
    }
})