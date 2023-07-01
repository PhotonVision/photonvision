import {defineStore} from "pinia";
import type {
    CameraCalibrationResult,
    CameraSettings,
    GeneralSettings,
    LightingSettings,
    MetricData,
    NetworkSettings, VideoFormat
} from "@/types/SettingTypes";
import {NetworkConnectionType} from "@/types/SettingTypes";
import {useStateStore} from "@/stores/StateStore";
import axios from "axios";
import type {WebsocketCameraSettingsUpdate, WebsocketSettingsUpdate} from "@/types/WebsocketDataTypes";

interface SettingsStore {
    general: GeneralSettings,
    cameras: CameraSettings[],
    network: NetworkSettings,
    lighting: LightingSettings,
    metrics: MetricData
}

export const useSettingsStore = defineStore("settings", {
    state: (): SettingsStore => ({
        general: {},
        cameras: [],
        network: {
            supported: true,
            connectionType: NetworkConnectionType.DHCP,
            hostname: "photonvision",
            runNTServer: false
        },
        lighting: {
            supported: true,
            brightness: 0
        },
        metrics: {}
    }),
    getters: {
        currentCameraSettings(): CameraSettings | null {
            const currentCameraIndex = useStateStore().currentCameraIndex;

            if(this.cameras.length === 0 || currentCameraIndex === undefined) {
                return null;
            }

            return this.cameras[currentCameraIndex];
        }
    },
    actions: {
        requestMetricsUpdate() {
            return axios.post("/utils/publishMetrics");
        },
        updateMetricsFromWebsocket(data: Required<MetricData>) {
            // Websocket returns empty strings instead of undefined so that needs to be fixed
            // TODO, do this... better
            this.$patch({metrics: {
                cpuTemp: data.cpuTemp === "" ? undefined : data.cpuTemp,
                cpuUtil: data.cpuUtil === "" ? undefined : data.cpuUtil,
                cpuMem: data.cpuMem === "" ? undefined : data.cpuMem,
                gpuMem: data.gpuMem === "" ? undefined : data.gpuMem,
                ramUtil: data.ramUtil === "" ? undefined : data.ramUtil,
                gpuMemUtil: data.gpuMemUtil === "" ? undefined : data.gpuMemUtil,
                cpuThr: data.cpuThr === "" ? undefined : data.cpuThr,
                cpuUptime: data.cpuUptime === "" ? undefined : data.cpuUptime,
                diskUtilPct: data.diskUtilPct === "" ? undefined : data.diskUtilPct
            }});
        },
        updateGeneralSettingsFromWebsocket(data: WebsocketSettingsUpdate) {
            this.$patch({
                general: data.general,
                lighting: data.lighting,
                network: {
                    ntServerAddress: data.networkSettings.ntServerAddress,
                    connectionType: data.networkSettings.connectionType,
                    staticIp: data.networkSettings.staticIp,
                    hostname: data.networkSettings.hostname,
                    runNTServer: data.networkSettings.runNTServer,
                    shouldMange: data.networkSettings.shouldManage
                }
            });
        },
        updateCameraSettingsFromWebsocket(data: WebsocketCameraSettingsUpdate[]) {
            this.$patch({
                cameras: data.map<CameraSettings>((d) => ({
                    nickname: d.nickname,
                    fov: {
                        value: d.fov,
                        managedByVendor: !d.isFovConfigurable
                    },
                    stream: {
                        inputPort: d.inputStreamPort,
                        outputPort: d.outputStreamPort
                    },
                    validVideoFormats: Object.keys(d.videoFormatList)
                        .sort((a, b) => parseInt(a) - parseInt(b)) // TODO, does the backend already return this sorted
                        .map<VideoFormat>((k, i) => ({...d.videoFormatList[k], index: i})),
                    completeCalibrations: d.calibrations.map<CameraCalibrationResult>(calib => ({
                        resolution: {
                            height: calib.height,
                            width: calib.width
                        },
                        distCoeffs: calib.distCoeffs,
                        standardDeviation: calib.standardDeviation,
                        perViewErrors: calib.perViewErrors,
                        intrinsics: calib.intrinsics
                    })),
                    currentPipelineIndex: d.currentPipelineIndex,
                    pipelineSettings: d.currentPipelineSettings
                }))
            });
        }
    }
});
