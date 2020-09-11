import Vue from 'vue'
import Vuex from 'vuex'

import undoRedo from "./modules/undoRedo";

Vue.use(Vuex);

const set = key => (state, val) => {
    Vue.set(state, key, val);
};

export default new Vuex.Store({
    modules: {
        undoRedo: undoRedo
    },
    state: {
        backendConnected: false,
        colorPicking: false,
        logsOverlay: false,
        compactMode: localStorage.getItem("compactMode") === undefined ? undefined : localStorage.getItem("compactMode") === "true", // Compact mode is initially unset on purpose
        logMessages: [],
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
                calibrations: [ ],
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
                    outputShouldDraw: true,
                    outputShowMultipleTargets: false,
                    offsetRobotOffsetMode: 0,
                    solvePNPEnabled: false,
                    targetRegion: 0,
                    contourTargetOrientation: 1,

                    cornerDetectionAccuracyPercentage: 10,

                    // Settings that apply to shape
                }
            }
        ],
        pipelineResults: {
                fps: 0,
                latency: 0,
                targets: [{
                    // Available in both 2D and 3D
                    pitch: 0,
                    yaw: 0,
                    skew: 0,
                    area: 0,
                    // 3D only
                    pose: {x: 0, y: 0, rot: 0},
                }]
            },
        settings: {
            general: {
                version: "Unknown",
                // Empty string means unsupported, otherwise the value in the string is the transfer mode
                gpuAcceleration: "",

                hardwareModel: "Unknown",
                hardwarePlatform: "Unknown",
            },
            networkSettings: {
                teamNumber: 0,

                supported: true,
                // Below options are only configurable if supported is true
                connectionType: 0, // 0 = DHCP, 1 = Static
                staticIp: "",
                netmask: "",
                hostname: "photonvision",
                runNTServer: false,
            },
            lighting: {
                supported: true,
                brightness: 0.0,
            },
        },
        calibrationData: {
            count: 0,
            videoModeIndex: 0,
            minCount: 25,
            hasEnough: false,
            squareSizeIn: 1.0,
            patternWidth: 7,
            patternHeight: 7,
            boardType: 0, // Chessboard, dotboard
        },
        metrics: {
            cpuTemp: "41.318",
            cpuUtil: "4.2",
            cpuMem: "896",
            gpuTemp: "40.8",
            gpuMem: "128mb",
            ramUtil: "68mb"
        }
    },
    mutations: {
        compactMode: set('compactMode'),
        cameraSettings: set('cameraSettings'),
        currentCameraIndex: set('currentCameraIndex'),
        selectedOutputs: set('selectedOutputs'),
        settings: set('settings'),
        calibrationData: set('calibrationData'),
        metrics: set('metrics'),
        logString: (state, newStr) => {
            const str = state.logMessages;
            str.push(newStr)
            Vue.set(state, 'logString', str)
        },

        solvePNPEnabled: (state, val) => {
            state.cameraSettings[state.currentCameraIndex].currentPipelineSettings.solvePNPEnabled = val;
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

        mutateSettings: (state, payload) => {
            for (let key in payload) {
                if (!payload.hasOwnProperty(key)) continue;
                const value = payload[key];
                const settings = state.settings;
                if (settings.hasOwnProperty(key)) {
                    Vue.set(settings, key, value);
                }
            }
        },

        mutatePipelineResults(state, payload) {
            // Key: index, value: result
            for (let key in payload) {
                if (!payload.hasOwnProperty(key)) continue;
                const index = parseInt(key);
                if(index === state.currentCameraIndex) {
                    Vue.set(state, 'pipelineResults', payload[key])
                }
            }
        },

        mutateEnabledLEDPercentage(state, payload)  {
            const settings = state.settings;
            settings.lighting.brightness = payload;
            Vue.set(state, "settings", settings);
        },

        mutateCalibrationState: (state, payload) => {
            for (let key in payload) {
                if (!payload.hasOwnProperty(key)) continue;
                const value = payload[key];
                const calibration = state.calibrationData;
                if (calibration.hasOwnProperty(key)) {
                    calibration[key] = value
                }
                Vue.set(state, 'calibrationData', calibration)
            }
        },
    },
    getters: {
        isDriverMode: state => state.cameraSettings[state.currentCameraIndex].currentPipelineIndex === -1,
        streamAddress: state =>
            ["http://" + location.hostname + ":" + state.cameraSettings[state.currentCameraIndex].inputStreamPort + "/stream.mjpg",
                "http://" + location.hostname + ":" + state.cameraSettings[state.currentCameraIndex].outputStreamPort + "/stream.mjpg"],
        currentPipelineResults: state => {
            return state.pipelineResults;
        },
        isCalibrated: state => {
            let resolution = state.cameraSettings[state.currentCameraIndex].videoFormatList[state.cameraSettings[state.currentCameraIndex].currentPipelineSettings.cameraVideoModeIndex];
            return state.cameraSettings[state.currentCameraIndex].calibrations
                .some(e => e.width === resolution.width && e.height === resolution.height);
        },
        cameraList: state => state.cameraSettings.map(it => it.nickname),
        currentCameraSettings: state => state.cameraSettings[state.currentCameraIndex],
        currentCameraIndex: state => state.currentCameraIndex,
        currentPipelineIndex: state => state.cameraSettings[state.currentCameraIndex].currentPipelineIndex,
        currentPipelineSettings: state => state.cameraSettings[state.currentCameraIndex].currentPipelineSettings,
        videoFormatList: state => {
            return Object.values(state.cameraSettings[state.currentCameraIndex].videoFormatList); // convert to a list
        },
        pipelineList: state => state.cameraSettings[state.currentCameraIndex].pipelineNicknames,
        calibrationList: state => state.cameraSettings[state.currentCameraIndex].calibrations,
    }
})