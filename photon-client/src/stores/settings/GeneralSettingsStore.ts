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

interface GeneralSettingsStore {
  general: GeneralSettings;
  network: NetworkSettings;
  lighting: LightingSettings;
  metrics: MetricData;
  currentFieldLayout;
}

interface MetricsEntry {
  time: number;
  metrics: MetricData;
}

class MetricsHistory {
  private MAX_METRIC_HISTORY = 60;
  private UPDATE_INTERVAL_MS = 900;

  private buffer: (MetricsEntry | undefined)[];
  private size: number;
  private index = 0;
  private count = 0;
  private lastUpdate = 0;

  constructor(size = this.MAX_METRIC_HISTORY) {
    this.size = size;
    this.buffer = new Array<MetricsEntry | undefined>(size);
  }

  update(value: MetricsEntry): boolean {
    const now = Date.now();
    if (now - this.lastUpdate < this.UPDATE_INTERVAL_MS) return false;

    this.lastUpdate = now;
    this.buffer[this.index] = value;
    this.index = (this.index + 1) % this.size;
    this.count = Math.min(this.count + 1, this.size);
    return true;
  }

  getHistory(): MetricsEntry[] {
    const result: MetricsEntry[] = new Array(this.count);
    for (let i = 0; i < this.count; i++) {
      const idx = (this.index - this.count + i + this.size) % this.size;
      result[i] = this.buffer[idx]!;
    }
    return result;
  }
}

const metricsHistoryBuffer = new MetricsHistory();
export const metricsHistorySnapshot = ref<MetricsEntry[]>([]);

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
      diskUsableSpace: undefined,
      npuUsage: undefined,
      ipAddress: undefined,
      uptime: undefined,
      sentBitRate: undefined,
      recvBitRate: undefined
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
        diskUsableSpace: data.diskUsableSpace || undefined,
        npuUsage: data.npuUsage || undefined,
        ipAddress: data.ipAddress || undefined,
        uptime: data.uptime || undefined,
        sentBitRate: data.sentBitRate || undefined,
        recvBitRate: data.recvBitRate || undefined
      };
      if (metricsHistoryBuffer.update({ time: Date.now(), metrics: this.metrics })) {
        metricsHistorySnapshot.value = metricsHistoryBuffer.getHistory();
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
