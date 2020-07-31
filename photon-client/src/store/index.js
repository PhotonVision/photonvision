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
        backendConnected: false,
        colorPicking: false,
        saveBar: false,
        compactMode: localStorage.getItem("compactMode") === undefined ? undefined : localStorage.getItem("compactMode") === "true", // Compact mode is initially unset on purpose
        currentCameraIndex: 0,
        selectedOutputs: [0, 1], // 0 indicates normal, 1 indicates threshold
        cameraSettings: [ // This is a list of objects representing the settings of all cameras
            {
                tiltDegrees: 0.0,
                currentPipelineIndex: 0,
                pipelineNicknames: ["Unknown"],
                outputStreamPort: 1181,
                inputStreamPort: 1182,
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
                isFovConfigurable: true,
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
                    streamingFrameDivisor: 0,

                    // Settings that apply to reflective
                    hsvHue: [0, 15],
                    hsvSaturation: [0, 15],
                    hsvValue: [0, 25],
                    erode: false,
                    dilate: false,
                    contourArea: [0, 12],
                    contourRatio: [0, 12],
                    contourFullness: [0, 12],
                    contourSpecklePercentage: 5,
                    contourGroupingMode: 0,
                    contourIntersection: 0,
                    contourSortMode: 0,
                    outputShowMultipleTargets: false,
                    offsetRobotOffsetMode: 0,
                    solvePNPEnabled: false,
                    targetRegion: 0,
                    contourTargetOrientation: 1,
                    is3D: false,

                    // Settings that apply to shape
                }
            }
        ],
        pipelineResults: [
            {
                fps: 0,
                latency: 0,
                targets: [{
                    // Available in both 2D and 3D
                    pitch: 0,
                    yaw: 0,
                    skew: 0,
                    area: 0,
                    // 3D only
                    pose: {x: 0, y: 0, rotation: 0},
                }]
            }
        ],
        settings: {
            general: {
                version: "Unknown",
                // Empty string means unsupported, otherwise the value in the string is the transfer mode
                gpuAcceleration: "",

                hardwareModel: "Unknown",
                hardwarePlatform: "Unknown",
            },
            networking: {
                teamNumber: 0,

                supported: true,
                // Below options are only configurable if supported is true
                connectionType: 0, // 0 = DHCP, 1 = Static
                staticIp: "",
                netmask: "",
                hostname: "photonvision",
            },
            lighting: {
                supported: true,
                brightness: 0.0,
            },
        }
    },
    mutations: {
        saveBar: set('saveBar'),
        compactMode: set('compactMode'),
        cameraSettings: set('cameraSettings'),
        currentCameraIndex: set('currentCameraIndex'),
        pipelineResults: set('pipelineResults'),
        networkSettings: set('networkSettings'),
        selectedOutputs: set('selectedOutputs'),

        is3D: (state, val) => {
            state.cameraSettings[state.currentCameraIndex].currentPipelineSettings.is3D = val;
        },

        currentPipelineIndex: (state, val) => {
            const settings = state.cameraSettings[state.currentCameraIndex];
            Vue.set(settings, 'currentPipelineIndex', val);
        },

        // TODO change everything to use this
        mutatePipeline: (state, payload) => {
            for (let key in payload) {
                if (!payload.hasOwnProperty(key)) continue;
                const value = payload[key];
                const settings = state.cameraSettings[state.currentCameraIndex].currentPipelineSettings;
                if (settings.hasOwnProperty(key)) {
                    Vue.set(settings, key, value);
                }
            }
        },

        mutatePipelineResults(state, payload) {
            // Key: index, value: result
            let newResultArray = [];
            for (let key in payload) {
                if (!payload.hasOwnProperty(key)) continue;
                const index = parseInt(key);
                newResultArray[index] = payload[key];
            }

            Vue.set(state, 'pipelineResults', newResultArray)
        }
    },
    getters: {
        isDriverMode: state => state.cameraSettings[state.currentCameraIndex].currentPipelineIndex === -1,
        pipelineSettings: state => state.pipelineSettings,
        streamAddress: state =>
            ["http://" + location.hostname + ":" + state.cameraSettings[state.currentCameraIndex].inputStreamPort + "/stream.mjpg",
                "http://" + location.hostname + ":" + state.cameraSettings[state.currentCameraIndex].outputStreamPort + "/stream.mjpg"],
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