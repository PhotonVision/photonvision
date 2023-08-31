import { defineStore } from "pinia";
import type { LogMessage } from "@/types/SettingTypes";
import type { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import type { PipelineResult } from "@/types/PhotonTrackingTypes";
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
  currentCameraIndex: number;

  pipelineResults?: PipelineResult;

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
}

export const useStateStore = defineStore("state", {
  state: (): StateStore => {
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
      currentCameraIndex: 0,

      pipelineResults: undefined,

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
      }
    };
  },
  actions: {
    setSidebarFolded(value: boolean) {
      this.sidebarFolded = value;
      localStorage.setItem("sidebarFolded", Boolean(value).toString());
    },
    addLogFromWebsocket(data: WebsocketLogMessage) {
      this.logMessages.push({
        level: data.logMessage.logLevel,
        message: data.logMessage.logMessage
      });
    },
    updateNTConnectionStatusFromWebsocket(data: WebsocketNTUpdate) {
      this.ntConnectionStatus = {
        connected: data.connected,
        address: data.address,
        clients: data.clients
      };
    },
    updatePipelineResultsFromWebsocket(data: WebsocketPipelineResultUpdate) {
      for (const cameraIndex in data) {
        if (parseInt(cameraIndex) === this.currentCameraIndex) {
          this.pipelineResults = data[cameraIndex];
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
