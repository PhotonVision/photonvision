import { defineStore } from "pinia";
import type {
    GeneralSettings,
    LightingSettings,
    MetricData,
    NetworkSettings
} from "@/types/SettingTypes";
import { NetworkConnectionType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import axios from "axios";
import type { WebsocketSettingsUpdate } from "@/types/WebsocketDataTypes";
import type { AprilTagFieldLayout } from "@/types/PhotonTrackingTypes";

interface GeneralSettingsStore {
    general: GeneralSettings,
    network: NetworkSettings,
    lighting: LightingSettings,
    metrics: MetricData,
    currentFieldLayout?: AprilTagFieldLayout
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
            shouldManage: true,
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
        },
        currentFieldLayout: undefined
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
            this.metrics = {
                cpuTemp: data.cpuTemp || undefined,
                cpuUtil: data.cpuUtil || undefined,
                cpuMem: data.cpuMem || undefined,
                gpuMem: data.gpuMem || undefined,
                ramUtil: data.ramUtil || undefined,
                gpuMemUtil: data.gpuMemUtil || undefined,
                cpuThr: data.cpuThr || undefined,
                cpuUptime: data.cpuUptime || undefined,
                diskUtilPct: data.diskUtilPct || undefined
            };
        },
        updateGeneralSettingsFromWebsocket(data: WebsocketSettingsUpdate) {
            this.general = {
                version: data.general.version || undefined,
                hardwareModel: data.general.hardwareModel || undefined,
                hardwarePlatform: data.general.hardwarePlatform || undefined,
                gpuAcceleration: data.general.gpuAcceleration || undefined
            };
            this.lighting = data.lighting;
            this.network = data.networkSettings;
            this.currentFieldLayout = data.atfl;
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
                shouldManage: this.network.shouldManage,
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
