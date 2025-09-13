import { defineStore } from "pinia";
import type { ConfigurableNetworkSettings, GeneralSettings, MetricData, NetworkSettings } from "@/types/SettingTypes";
import { NetworkConnectionType } from "@/types/SettingTypes";
import axios from "axios";
import type { WebsocketSettingsUpdate } from "@/types/WebsocketDataTypes";

interface GeneralSettingsStore {
  general: GeneralSettings;
  network: NetworkSettings;
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
      availableModels: [],
      supportedBackends: [],
      conflictingHostname: false,
      conflictingCameras: ""
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
    metrics: {
      cpuTemp: undefined,
      cpuUtil: undefined,
      cpuThr: undefined,
      ramMem: undefined,
      ramUtil: undefined,
      gpuMem: undefined,
      gpuMemUtil: undefined,
      diskUtilPct: undefined,
      npuUsage: undefined,
      ipAddress: undefined,
      uptime: undefined
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
        cpuThr: data.cpuThr || undefined,
        ramMem: data.ramMem || undefined,
        ramUtil: data.ramUtil || undefined,
        gpuMem: data.gpuMem || undefined,
        gpuMemUtil: data.gpuMemUtil || undefined,
        diskUtilPct: data.diskUtilPct || undefined,
        npuUsage: data.npuUsage || undefined,
        ipAddress: data.ipAddress || undefined,
        uptime: data.uptime || undefined
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
        supportedBackends: data.general.supportedBackends || [],
        conflictingHostname: data.general.conflictingHostname || false,
        conflictingCameras: data.general.conflictingCameras || ""
      };
      this.network = data.networkSettings;
      this.currentFieldLayout = data.atfl;
    },
    updateGeneralSettings(payload: Required<ConfigurableNetworkSettings>) {
      return axios.post("/settings/general", payload);
    }
  }
});
