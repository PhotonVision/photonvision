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
                    pipelineType: "reflective", // One of "driver", "reflective", "shape"

                    // Settings that apply to all pipeline types
                    cameraVideoModeIndex: 0,

                    // Settings that apply to reflective
                    exposure: 0,
                    brightness: 0,
                    gain: 0,
                    rotationMode: 0,
                    hue: [0, 15],
                    saturation: [0, 15],
                    value: [0, 25],
                    erode: false,
                    dilate: false,
                    area: [0, 12],
                    ratio: [0, 12],
                    extent: [0, 12],
                    speckle: 5,
                    targetGrouping: 0,
                    targetIntersection: 0,
                    sortMode: 0,
                    multiple: false,
                    isBinary: 0,
                    calibrationMode: 0,
                    videoModeIndex: 0,
                    streamDivisor: 0,
                    is3D: false,
                    targetRegion: 0,
                    targetOrientation: 1

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
        networkSettings: set('networkSettings')
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
