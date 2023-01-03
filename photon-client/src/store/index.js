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
        websocket: null,
        ntConnectionInfo: {
            connected: false,
            address: "",
            clients: 0,
        },
        networkInfo: {
            possibleRios: ["Loading..."],
            deviceips: ["Loading..."],
        },
        connectedCallbacks: [],
        colorPicking: false,
        logsOverlay: false,
        compactMode: localStorage.getItem("compactMode") === undefined ? undefined : localStorage.getItem("compactMode") === "true", // Compact mode is initially unset on purpose
        logMessages: [],
        currentCameraIndex: 0,
        cameraSettings: [ // This is a list of objects representing the settings of all cameras
            {
                tiltDegrees: 0.0,
                currentPipelineIndex: 0,
                pipelineNicknames: ["Unknown"],
                outputStreamPort: 0,
                inputStreamPort: 0,
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
                    pipelineType: 5, // One of "calib", "driver", "reflective", "shape", "AprilTag"
                    // 2 is reflective

                    // Settings that apply to all pipeline types
                    cameraExposure: 1,
                    cameraBrightness: 2,
                    cameraAutoExposure: false,
                    cameraRedGain: 3,
                    cameraBlueGain: 4,
                    inputImageRotationMode: 0,
                    cameraVideoModeIndex: 0,
                    streamingFrameDivisor: 0,

                    // Settings that apply to reflective
                    hsvHue: [0, 15],
                    hsvSaturation: [0, 15],
                    hsvValue: [0, 25],
                    hueInverted: false,
                    contourArea: [0, 12],
                    contourRatio: [0, 12],
                    contourFullness: [0, 12],
                    contourSpecklePercentage: 5,
                    contourFilterRangeX: 5,
                    contourFilterRangeY: 5,
                    contourGroupingMode: 0,
                    contourIntersection: 0,
                    contourSortMode: 0,
                    inputShouldShow: true,
                    outputShouldShow: true,
                    outputShouldDraw: true,
                    outputShowMultipleTargets: false,
                    offsetRobotOffsetMode: 0,
                    solvePNPEnabled: false,
                    targetRegion: 0,
                    contourTargetOrientation: 1,

                    cornerDetectionAccuracyPercentage: 10,

                    // Settings that apply to AprilTag
                    tagFamily: 1,
                    decimate: 1.0,
                    blur: 0.0,
                    threads: 1,
                    debug: false,
                    refineEdges: true,
                    numIterations: 1,
                    decisionMargin: 0,
                    hammingDist: 0,
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
                    pose: {x: 1, y: 1, z: 0, qw: 1, qx: 0, qy: 0, qz: 0},
                },
            {
                // Available in both 2D and 3D
                pitch: 0,
                yaw: 0,
                skew: 0,
                area: 0,
                // 3D only
                pose: {x: 2, y: 3, z: 0, qw: 1, qx: 0, qy: 0, qz: 0},
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
            minCount: 12, // Gets set by backend anyways, but we need a sane default
            hasEnough: false,
            squareSizeIn: 1.0,
            patternWidth: 8,
            patternHeight: 8,
            boardType: 0, // Chessboard, dotboard
        },
        metrics: {
            cpuTemp: "N/A",
            cpuUtil: "N/A",
            cpuMem: "N/A",
            gpuMem: "N/A",
            ramUtil: "N/A",
            gpuMemUtil: "N/A",
        }
    },
    mutations: {
        compactMode: set('compactMode'),
        websocket: set('websocket'),
        cameraSettings: set('cameraSettings'),
        currentCameraIndex: set('currentCameraIndex'),
        selectedOutputs: set('selectedOutputs'),
        settings: set('settings'),
        calibrationData: set('calibrationData'),
        metrics: set('metrics'),
        ntConnectionInfo: set('ntConnectionInfo'),
        networkInfo: set('networkInfo'),
        backendConnected: set('backendConnected'),
        logString: (state, newStr) => {
            const str = state.logMessages;
            str.push(newStr);
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

        mutateNetworkSettings: (state, payload) => {
            for (let key in payload) {
                if (!payload.hasOwnProperty(key)) continue;
                const value = payload[key];
                const settings = state.settings.networkSettings;
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
        currentVideoFormat: state => state.cameraSettings[state.currentCameraIndex].videoFormatList[state.cameraSettings[state.currentCameraIndex].currentPipelineSettings.cameraVideoModeIndex],
        videoFormatList: state => {
            return Object.values(state.cameraSettings[state.currentCameraIndex].videoFormatList); // convert to a list
        },
        pipelineList: state => state.cameraSettings[state.currentCameraIndex].pipelineNicknames,
        calibrationList: state => state.cameraSettings[state.currentCameraIndex].calibrations,
        pipelineType: state => state.cameraSettings[state.currentCameraIndex].currentPipelineSettings.pipelineType
    }
})
