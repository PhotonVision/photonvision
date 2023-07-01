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
    sidebarFolded: boolean,
    logMessages: LogMessage[]
    currentCameraIndex?: number,

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
            ntConnectionStatus: {
                connected: false
            },
            showLogModal: false,
            sidebarFolded: localStorage.getItem("sidebarFolded") === null ? false : localStorage.getItem("sidebarFolded") === "true",
            logMessages: [],

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
            this.$patch({sidebarFolded: value});
            localStorage.setItem("sidebarFolded", Boolean(value).toString());
        },
        addLogFromWebsocket(data: WebsocketLogMessage) {
            this.$patch(state => state.logMessages.push({
                level: data.logMessage.logLevel,
                message: data.logMessage.logMessage
            }));
        },
        updateNTConnectionStatusFromWebsocket(data: WebsocketNTUpdate) {
            this.$patch({
                ntConnectionStatus: {
                    connected: data.connected,
                    address: data.address,
                    clients: data.clients
                }
            });
        },
        updatePipelineResultsFromWebsocket(data: WebsocketPipelineResultUpdate) {
            if(this.currentCameraIndex === undefined) return;

            const pipelineResultData = data[this.currentCameraIndex];

            this.$patch({pipelineResults: pipelineResultData});
        },
        updateCalibrationStateValuesFromWebsocket(data: WebsocketCalibrationData) {
            this.$patch({
                calibrationData: {
                    imageCount: data.count,
                    videoFormatIndex: data.videoModeIndex,
                    minimumImageCount: data.minCount,
                    hasEnoughImages: data.hasEnough
                }
            });
        },
        showSnackbarMessage(data: {
            show: boolean,
            message: string,
            color: string,
            timeout: number
        }) {
            this.$patch({snackbarData: data});

            if(data.timeout != -1) {
                setTimeout(this.hideSnackbarMessage, data.timeout);
            }
        },
        hideSnackbarMessage() {
            this.$patch({snackbarData: {show: false, timeout: 0}});
        }
    }
});
