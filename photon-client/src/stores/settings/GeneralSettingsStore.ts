import {defineStore} from "pinia";
import type {
    GeneralSettings,
    LightingSettings,
    MetricData,
    NetworkSettings
} from "@/types/SettingTypes";
import {NetworkConnectionType} from "@/types/SettingTypes";
import {useStateStore} from "@/stores/StateStore";
import axios from "axios";
import type {WebsocketSettingsUpdate} from "@/types/WebsocketDataTypes";

interface GeneralSettingsStore {
    general: GeneralSettings,
    network: NetworkSettings,
    lighting: LightingSettings,
    metrics: MetricData
}

export const useSettingsStore = defineStore("settings", {
    state: (): GeneralSettingsStore => ({
        general: {
            version: undefined,
            gpuAcceleration: undefined,
            hardwareModel: undefined,
            hardwarePlatform: undefined
        },
        network: {
            ntServerAddress: "",
            shouldMange: true, // TODO, is this overwritten by the backend before it matters?
            connectionType: NetworkConnectionType.DHCP,
            staticIp: "",
            hostname: "photonvision",
            runNTServer: false
        },
        lighting: {
            supported: true,
            brightness: 0
        },
        metrics: {
            cpuTemp: undefined,
            cpuUtil: undefined,
            cpuMem: undefined,
            gpuMem: undefined,
            ramUtil: undefined,
            gpuMemUtil: undefined,
            cpuThr: undefined,
            cpuUptime: undefined,
            diskUtilPct: undefined
        }
    }),
    getters: {
        gpuAccelerationEnabled(): boolean {
            return this.general.gpuAcceleration !== undefined;
        }
    },
    actions: {
        requestMetricsUpdate() {
            return axios.post("/utils/publishMetrics");
        },
        updateMetricsFromWebsocket(data: Required<MetricData>) {
            // TODO, do this... better
            this.metrics = {
                cpuTemp: data.cpuTemp === "" ? undefined : data.cpuTemp,
                cpuUtil: data.cpuUtil === "" ? undefined : data.cpuUtil,
                cpuMem: data.cpuMem === "" ? undefined : data.cpuMem,
                gpuMem: data.gpuMem === "" ? undefined : data.gpuMem,
                ramUtil: data.ramUtil === "" ? undefined : data.ramUtil,
                gpuMemUtil: data.gpuMemUtil === "" ? undefined : data.gpuMemUtil,
                cpuThr: data.cpuThr === "" ? undefined : data.cpuThr,
                cpuUptime: data.cpuUptime === "" ? undefined : data.cpuUptime,
                diskUtilPct: data.diskUtilPct === "" ? undefined : data.diskUtilPct
            };
        },
        updateGeneralSettingsFromWebsocket(data: WebsocketSettingsUpdate) {
            this.general = {
                // TODO, do this... better
                version: data.general.version === "" ? undefined : data.general.version,
                hardwareModel: data.general.hardwareModel === "" ? undefined : data.general.hardwareModel,
                hardwarePlatform: data.general.hardwarePlatform === "" ? undefined : data.general.hardwarePlatform,
                gpuAcceleration: data.general.gpuAcceleration === "" ? undefined : data.general.gpuAcceleration
            };
                this.lighting = data.lighting;
                this.network = data.networkSettings;
        },
        saveGeneralSettings() {
            const payload: Required<NetworkSettings> = {
                connectionType: this.network.connectionType,
                hostname: this.network.hostname,
                networkManagerIface: this.network.networkManagerIface || "",
                ntServerAddress: this.network.ntServerAddress,
                physicalInterface: this.network.physicalInterface || "",
                runNTServer: this.network.runNTServer,
                setDHCPcommand: this.network.setDHCPcommand || "",
                setStaticCommand: this.network.setStaticCommand || "",
                shouldMange: this.network.shouldMange,
                staticIp: this.network.staticIp
            };
            return axios.post("/settings/general", payload);
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
