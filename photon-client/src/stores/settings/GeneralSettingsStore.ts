import { defineStore } from "pinia";
import type {
  ConfigurableNetworkSettings,
  GeneralSettings,
  LightingSettings,
  MetricData,
  NetworkSettings
} from "@/types/SettingTypes";
import { NetworkConnectionType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import axios from "axios";
import type { WebsocketSettingsUpdate } from "@/types/WebsocketDataTypes";

interface GeneralSettingsStore {
  general: GeneralSettings;
  network: NetworkSettings;
  lighting: LightingSettings;
  metrics: MetricData;
  currentFieldLayout;
}

export const useSettingsStore = defineStore("settings", {
  state: (): GeneralSettingsStore => ({
    general: {
      version: undefined,
      gpuAcceleration: undefined,
      hardwareModel: undefined,
      hardwarePlatform: undefined,
      mrCalWorking: true,
      availableModels: {},
      supportedBackends: []
    },
    network: {
      ntServerAddress: "",
      shouldManage: true,
      canManage: true,
      connectionType: NetworkConnectionType.DHCP,
      staticIp: "",
      hostname: "photonvision",
      runNTServer: false,
      shouldPublishProto: false,
      networkInterfaceNames: [
        {
          connName: "Example Wired Connection",
          devName: "eth0"
        }
      ],
      networkingDisabled: false
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
      diskUtilPct: undefined,
      npuUsage: undefined,
      ipAddress: undefined
    },
    currentFieldLayout: {
      field: {
        length: 16.4592,
        width: 8.2296
      },
      tags: []
    }
  }),
  getters: {
    gpuAccelerationEnabled(): boolean {
      return this.general.gpuAcceleration !== undefined;
    },
    networkInterfaceNames(): string[] {
      return this.network.networkInterfaceNames.map((i) => i.devName);
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
        diskUtilPct: data.diskUtilPct || undefined,
        npuUsage: data.npuUsage || undefined,
        ipAddress: data.ipAddress || undefined
      };
    },
    updateGeneralSettingsFromWebsocket(data: WebsocketSettingsUpdate) {
      this.general = {
        version: data.general.version || undefined,
        hardwareModel: data.general.hardwareModel || undefined,
        hardwarePlatform: data.general.hardwarePlatform || undefined,
        gpuAcceleration: data.general.gpuAcceleration || undefined,
        mrCalWorking: data.general.mrCalWorking,
        availableModels: data.general.availableModels || undefined,
        supportedBackends: data.general.supportedBackends || []
      };
      this.lighting = data.lighting;
      this.network = data.networkSettings;
      this.currentFieldLayout = data.atfl;
    },
    updateGeneralSettings(payload: Required<ConfigurableNetworkSettings>) {
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
