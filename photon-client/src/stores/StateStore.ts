import {defineStore} from "pinia";
import type {LogMessage} from "@/types/SettingTypes";
import type {AutoReconnectingWebsocket} from "@/lib/AutoReconnectingWebsocket";
import type {PipelineResult} from "@/types/PhotonTrackingTypes";
import type {
    WebsocketCalibrationData,
    WebsocketLogMessage,
    WebsocketNTUpdate,
    WebsocketPipelineResultUpdate
} from "@/types/WebsocketDataTypes";

export interface NTConnectionStatus {
    connected: boolean,
    address?: string,
    clients?: number
}

interface StateStore {
    backendConnected: boolean,
    websocket?: AutoReconnectingWebsocket,
    ntConnectionStatus: NTConnectionStatus,
    showLogModal: boolean,
    logMessages: LogMessage[]
    currentCameraIndex: number,

    pipelineResults?: PipelineResult,

    calibrationData: {
        imageCount: number,
        videoFormatIndex: number,
        minimumImageCount: number,
        hasEnoughImages: boolean
    },

    snackbarData: {
        show: boolean,
        message: string,
        color: string,
        timeout: number
    }
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
            logMessages: [],
            currentCameraIndex: 0,

            pipelineResults: undefined,

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
            this.pipelineResults = data[this.currentCameraIndex];
        },
        updateCalibrationStateValuesFromWebsocket(data: WebsocketCalibrationData) {
            this.calibrationData = {
                imageCount: data.count,
                videoFormatIndex: data.videoModeIndex,
                minimumImageCount: data.minCount,
                hasEnoughImages: data.hasEnough
            };
        },
        showSnackbarMessage(data: {
            show?: boolean,
            message: string,
            color: string,
            timeout?: number
        }) {
            this.snackbarData = {
                show: data.show || true,
                message: data.message,
                color: data.color,
                timeout: data.timeout || 2000
            };

            if(data.timeout != -1) {
                setTimeout(this.hideSnackbarMessage, data.timeout || 2000);
            }
        },
        hideSnackbarMessage() {
            this.snackbarData = {show: false, timeout: 0, color: "error", message: ""};
        }
    }
});
