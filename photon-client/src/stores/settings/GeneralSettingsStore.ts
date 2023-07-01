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

interface GeneralSettingsStore {
    general: GeneralSettings,
    network: NetworkSettings,
    lighting: LightingSettings,
    metrics: MetricData
}

export const useSettingsStore = defineStore("settings", {
    state: (): GeneralSettingsStore => ({
        general: {},
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
        /**
         * Modify the brightness of the LEDs.
         *
         * @param brightness brightness to set [0, 100]
         */
        changeLEDBrightness(brightness: number) {
            const payload = {
                enabledLEDPercentage: brightness
            };
            useStateStore().websocket?.send(payload, true);
        }
    }
});
