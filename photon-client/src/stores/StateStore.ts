import { defineStore } from "pinia";
import type { LogMessage, VsmState } from "@/types/SettingTypes";
import type { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import type { MultitagResult, PipelineResult } from "@/types/PhotonTrackingTypes";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import type {
  WebsocketCalibrationData,
  WebsocketLogMessage,
  WebsocketNTUpdate,
  WebsocketPipelineResultUpdate
} from "@/types/WebsocketDataTypes";

export interface NTConnectionStatus {
  connected: boolean;
  address?: string;
  clients?: number;
}

interface StateStore {
  backendConnected: boolean;
  websocket?: AutoReconnectingWebsocket;
  ntConnectionStatus: NTConnectionStatus;
  showLogModal: boolean;
  sidebarFolded: boolean;
  logMessages: LogMessage[];
  currentCameraUniqueName: string;

  backendResults: Record<number, PipelineResult>;
  multitagResultBuffer: Record<string, MultitagResult[]>;

  colorPickingMode: boolean;

  calibrationData: {
    imageCount: number;
    videoFormatIndex: number;
    minimumImageCount: number;
    hasEnoughImages: boolean;
  };

  snackbarData: {
    show: boolean;
    message: string;
    color: string;
    timeout: number;
  };

  vsmState: VsmState;
}

export const useStateStore = defineStore("state", {
  state: (): StateStore => {
    const cameraStore = useCameraSettingsStore();
    return {
      backendConnected: false,
      websocket: undefined,
      ntConnectionStatus: {
        connected: false
      },
      showLogModal: false,
      // Ignored if the display is too small
      sidebarFolded:
        localStorage.getItem("sidebarFolded") === null ? false : localStorage.getItem("sidebarFolded") === "true",
      logMessages: [],
      currentCameraUniqueName: Object.keys(cameraStore.cameras)[0],

      backendResults: {
        0: {
          classNames: [],
          fps: 1,
          latency: 2,
          sequenceID: 3,
          targets: [],
          multitagResult: undefined
        }
      },
      multitagResultBuffer: {},

      colorPickingMode: false,

      calibrationData: {
        imageCount: 0,
        videoFormatIndex: 0,
        minimumImageCount: 12,
        hasEnoughImages: false
      },

      snackbarData: {
        show: false,
        message: "No Message",
        color: "info",
        timeout: 2000
      },

      vsmState: {
        allConnectedCameras: [],
        disabledConfigs: []
      }
    };
  },
  getters: {
    currentPipelineResults(): PipelineResult | undefined {
      return this.backendResults[this.currentCameraUniqueName.toString()];
    },
    currentMultitagBuffer(): MultitagResult[] | undefined {
      if (!this.multitagResultBuffer[this.currentCameraUniqueName])
        this.multitagResultBuffer[this.currentCameraUniqueName] = [];
      return this.multitagResultBuffer[this.currentCameraUniqueName];
    }
  },
  actions: {
    setSidebarFolded(value: boolean) {
      this.sidebarFolded = value;
      localStorage.setItem("sidebarFolded", Boolean(value).toString());
    },
    addLogFromWebsocket(data: WebsocketLogMessage) {
      this.logMessages.push({
        level: data.logMessage.logLevel,
        message: data.logMessage.logMessage,
        timestamp: new Date()
      });
    },
    updateNTConnectionStatusFromWebsocket(data: WebsocketNTUpdate) {
      this.ntConnectionStatus = {
        connected: data.connected,
        address: data.address,
        clients: data.clients
      };
    },
    updateBackendResultsFromWebsocket(data: WebsocketPipelineResultUpdate) {
      this.backendResults = {
        ...this.backendResults,
        ...data
      };

      for (const key in data) {
        const multitagRes = data[key].multitagResult;

        if (multitagRes) {
          if (!this.multitagResultBuffer[key]) {
            this.multitagResultBuffer[key] = [];
          }

          this.multitagResultBuffer[key].push(multitagRes);
          if (this.multitagResultBuffer[key].length > 100) {
            this.multitagResultBuffer[key].shift();
          }
        }
      }
    },
    updateCalibrationStateValuesFromWebsocket(data: WebsocketCalibrationData) {
      this.calibrationData = {
        imageCount: data.count,
        videoFormatIndex: data.videoModeIndex,
        minimumImageCount: data.minCount,
        hasEnoughImages: data.hasEnough
      };
    },
    updateDiscoveredCameras(data: VsmState) {
      this.vsmState = data;
    },
    showSnackbarMessage(data: { message: string; color: string; timeout?: number }) {
      this.snackbarData = {
        show: true,
        message: data.message,
        color: data.color,
        timeout: data.timeout || 2000
      };
    }
  }
});
