import { defineStore } from "pinia";
import type { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import { LogMessage } from "@/types/SettingTypes";
import type { MultitagResult, PipelineResult } from "@/types/PhotonTrackingTypes";
import { CircularBuffer } from "@/lib/CircularBuffer";
import { computed, reactive, readonly, ref, watch } from "vue";
import {
  StringifiedCameraIndex,
  WebsocketLogMessage,
  WebsocketNTUpdate,
  WebsocketPipelineResultUpdate
} from "@/types/WebsocketTypes";

export const useClientStore = defineStore("client", () => {
  const websocket = ref<AutoReconnectingWebsocket>();
  const backendConnected = ref<boolean>(false);

  const currentCameraIndex = ref<number>(0);

  const ntConnectionStatus = ref<WebsocketNTUpdate>({ connected: false });
  const updateNTConnectionStatusFromWebsocket = (status: WebsocketNTUpdate) => {
    ntConnectionStatus.value = status;
  };

  const showLogModal = ref<boolean>(false);
  const sidebarFoldedVal = ref<boolean>(localStorage.getItem("sidebarFolded") === "true");
  const sidebarFolded = computed({
    get: () => sidebarFoldedVal.value,
    set: (v) => {
      sidebarFoldedVal.value = v;
      localStorage.setItem("sidebarFolded", Boolean(v).toString());
    }
  });
  const logMessages = ref<LogMessage[]>([]);

  const addLogFromWebsocket = (log: WebsocketLogMessage) => {
    logMessages.value.push({
      level: log.logMessage.logLevel,
      message: log.logMessage.logMessage,
      timestamp: new Date()
    });
  };
  const clearLogs = () => (logMessages.value = []);

  interface BackendResultsWrapper {
    result: PipelineResult;
    multitagResultBuffer: CircularBuffer<MultitagResult>
  }

  const multitagResultBufferSize = ref(100);
  // BackendResults might be empty if no results have been sent yet. multitagResultBuffer won't be created for a camera unless it is a multitag camera.
  const backendResults = reactive<
    Record<
      StringifiedCameraIndex,
      BackendResultsWrapper | undefined
    >
  >({});
  const updateBackendResultsFromWebsocket = (result: WebsocketPipelineResultUpdate) => {
    const stringifiedCameraIndex = result.cameraIndex.toString();
    if (!Object.prototype.hasOwnProperty.call(backendResults, stringifiedCameraIndex)) {
      backendResults[stringifiedCameraIndex] = {
        result: result,
        multitagResultBuffer: new CircularBuffer(multitagResultBufferSize.value)
      };
    } else {
      backendResults[stringifiedCameraIndex]!.result = result;
    }

    if (!result.multitagResult) return;

    backendResults[stringifiedCameraIndex]?.multitagResultBuffer.add(result.multitagResult);
  };

  const pipelineResultsFromCameraIndex = (cameraIndex: number): PipelineResult | undefined => {
    return backendResults[cameraIndex.toString()]?.result;
  };
  const currentPipelineResults = computed(() => {
    return pipelineResultsFromCameraIndex(currentCameraIndex.value);
  });
  const getMultitagResultBufferFromCameraIndex = (cameraIndex: number) => {
    const stringifiedCameraIndex = cameraIndex.toString();
    if (!stringifiedCameraIndex || !Object.prototype.hasOwnProperty.call(backendResults, stringifiedCameraIndex)) {
      return undefined;
    }

    return backendResults[stringifiedCameraIndex]!.multitagResultBuffer;
  };
  const currentMultitagResultBuffer = computed(() => {
    return getMultitagResultBufferFromCameraIndex(currentCameraIndex.value);
  });
  const clearMultitagResultBufferByCameraIndex = (cameraIndex: number) => {
    const stringifiedCameraIndex = cameraIndex.toString();
    backendResults[stringifiedCameraIndex]!.multitagResultBuffer.clear();
  }
  const clearCurrentMultitagResultBuffer = () => {
    clearMultitagResultBufferByCameraIndex(currentCameraIndex.value);
  };

  const colorPickingFromCameraStream = ref<boolean>(false);

  watch(multitagResultBufferSize, (newBufferSize: number) => {
    const res = Object.values(backendResults);
    for (let i = 0; i < res.length; i++) {
      res[i]?.multitagResultBuffer.resize(newBufferSize);
    }
  });

  return {
    websocket,
    backendConnected: backendConnected,
    currentCameraIndex,
    ntConnectionStatus: readonly(ntConnectionStatus),
    updateNTConnectionStatusFromWebsocket,
    showLogModal,
    sidebarFolded,
    logMessages: readonly(logMessages),
    addLogFromWebsocket,
    clearLogs,
    backendResults: readonly(backendResults),
    updateBackendResultsFromWebsocket,
    pipelineResultsFromCameraIndex,
    multitagResultBufferSize,
    currentPipelineResults,
    getMultitagResultBufferFromCameraIndex,
    currentMultitagResultBuffer,
    clearMultitagResultBufferByCameraIndex,
    clearCurrentMultitagResultBuffer,
    colorPickingFromCameraStream
  };
});
