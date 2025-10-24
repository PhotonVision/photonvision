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
import { ref } from "vue";

interface MetricsEntry {
  time: number;
  metrics: MetricData;
}

interface GeneralSettingsStore {
  general: GeneralSettings;
  network: NetworkSettings;
  lighting: LightingSettings;
  metrics: MetricData;
  metricsHistory: MetricsEntry[];
  metricsLastSaved: number;
  currentFieldLayout;
}

const MAX_METRIC_HISTORY = 50;
const UPDATE_INTERVAL_MS = 900;
const updateTimeElapsed = ref(true);

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
    lighting: {
      supported: true,
      brightness: 0
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
    metricsHistory: [],
    metricsLastSaved: 0,
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
      // let test: MetricData = {
      //   cpuTemp: 50 + Math.random() * 20,
      //   cpuUtil: 50 + Math.random() * 20,
      //   cpuThr: undefined,
      //   ramMem: 50 + Math.random() * 20,
      //   ramUtil: 50 + Math.random() * 20,
      //   gpuMem: data.gpuMem || undefined,
      //   gpuMemUtil: data.gpuMemUtil || undefined,
      //   diskUtilPct: 50 + Math.random() * 20,
      //   npuUsage: data.npuUsage || undefined,
      //   ipAddress: data.ipAddress || undefined,
      //   uptime: data.uptime || undefined
      // };
      if (updateTimeElapsed.value) {
        updateTimeElapsed.value = false;
        setTimeout(() => (updateTimeElapsed.value = true), UPDATE_INTERVAL_MS);
        this.metricsHistory.push({ time: Date.now(), metrics: this.metrics });
        // this.metricsHistory.push({ time: Date.now(), metrics: test });
        while (this.metricsHistory.length > MAX_METRIC_HISTORY) this.metricsHistory.shift();
      }
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
