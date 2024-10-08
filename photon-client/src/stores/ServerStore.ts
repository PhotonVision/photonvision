import { defineStore } from "pinia";
import { computed, readonly, ref } from "vue";
import {
  CameraCalibrationCoefficients,
  CameraConfig,
  ConfigurableMiscellaneousSettings,
  ConfigurableNetworkSettings,
  InstanceConfig,
  NetworkConnectionType,
  PlatformMetrics,
  type Resolution,
  RobotOffsetOperationMode,
  ValidQuirks,
  VideoFormat
} from "@/types/SettingTypes";
import { AprilTagFieldLayout } from "@/types/PhotonTrackingTypes";
import axios from "axios";
import { StartCalibrationPayload, WebsocketSettingsUpdate } from "@/types/WebsocketTypes";
import { useClientStore } from "@/stores/ClientStore";
import {
  ConfigurableUserPipelineSettings,
  PipelineType,
  PossiblePipelineSettings,
  UserPipelineType
} from "@/types/PipelineTypes";
import { resolutionsAreEqual } from "@/lib/PhotonUtils";

export const useServerStore = defineStore("server", () => {
  const clientStore = useClientStore();

  const instanceConfig = ref<InstanceConfig>();
  const platformMetrics = ref<PlatformMetrics>();
  const settings = ref<WebsocketSettingsUpdate>();

  const activeATFL = ref<AprilTagFieldLayout>();

  const cameras = ref<CameraConfig[]>();

  const networkInterfaceNames = computed<string[]>(
    () => settings.value?.network.networkInterfaceNames.map((i) => i.connName) || []
  );

  const getCameraSettingsFromIndex = (cameraIndex: number): CameraConfig | undefined => {
    return cameras.value?.find((v) => v.cameraIndex === cameraIndex);
  };
  const getPipelineSettingsFromIndex = (cameraIndex: number, pipelineIndex: number): PossiblePipelineSettings | undefined => {
    return getCameraSettingsFromIndex(cameraIndex)?.pipelineSettings.find((v) => v.pipelineIndex === pipelineIndex);
  };
  const getActivePipelineSettingsByCameraIndex = (cameraIndex: number): PossiblePipelineSettings | undefined => {
    const camSettings = getCameraSettingsFromIndex(cameraIndex);
    return camSettings?.pipelineSettings.find((v) => v.pipelineIndex === camSettings?.activePipelineIndex);
  };

  const currentCameraSettings = computed<CameraConfig | undefined>(() => {
    return getCameraSettingsFromIndex(clientStore.currentCameraIndex);
  });
  const currentPipelineSettings = computed<PossiblePipelineSettings | undefined>(() => {
    return getActivePipelineSettingsByCameraIndex(clientStore.currentCameraIndex);
  });
  const currentPipelineType = computed<PipelineType | undefined>(() => currentPipelineSettings.value?.pipelineType);
  const currentVideoFormat = computed<VideoFormat | undefined>(() => {
    const currentCamera = currentCameraSettings.value;
    const currentPipelineIndex = currentCamera?.activePipelineIndex;
    const currentPipelineConfig = currentCamera?.pipelineSettings.find((v) => v.pipelineIndex === currentPipelineIndex);
    const currentSourceVideoFormatIdx = currentPipelineConfig?.cameraVideoModeIndex;
    return currentCamera?.videoFormats.find((v) => v.sourceIndex === currentSourceVideoFormatIdx);
  });
  const isCurrentVideoFormatCalibrated = computed<boolean | undefined>(() => {
    const currentCamera = currentCameraSettings.value;
    const currentPipelineIndex = currentCamera?.activePipelineIndex;
    const currentPipelineConfig = currentCamera?.pipelineSettings.find((v) => v.pipelineIndex === currentPipelineIndex);
    const currentSourceVideoFormatIdx = currentPipelineConfig?.cameraVideoModeIndex;
    const currentVideoMode = currentCamera?.videoFormats.find((v) => v.sourceIndex === currentSourceVideoFormatIdx);

    if (!currentVideoMode?.resolution) return undefined;

    return currentCamera?.calibrations.some((v) => resolutionsAreEqual(v.resolution, currentVideoMode?.resolution));
  });
  const cameraNames = computed<string[]>(() => cameras.value?.map((c) => c.nickname) || []);
  const currentCameraName = computed<string | undefined>(() => currentCameraSettings.value?.nickname);
  const pipelineNames = computed<string[]>(
    () => currentCameraSettings.value?.pipelineSettings.map((v) => v.pipelineNickname) || []
  );
  const currentPipelineName = computed<string | undefined>(() => currentPipelineSettings.value?.pipelineNickname);
  const isDriverMode = computed<boolean | undefined>(
    () => currentPipelineSettings.value?.pipelineType === PipelineType.DriverMode
  );
  const isCalibMode = computed<boolean | undefined>(
    () => currentPipelineSettings.value?.pipelineType === PipelineType.Calib3d
  );
  const isCSICamera = computed<boolean | undefined>(() => currentCameraSettings.value?.isCSICamera);

  const requestMetricsUpdate = () => {
    const payload = {
      publishMetrics: true
    };
    clientStore.websocket?.send(payload, true);
  };
  const updatePlatformMetricsFromWebsocket = (metrics: PlatformMetrics) => {
    platformMetrics.value = metrics;
  };

  const updateInstanceConfigFromWebsocket = (config: InstanceConfig) => {
    instanceConfig.value = config;
  };
  const updateSettingsFromWebsocket = (wsSettings: WebsocketSettingsUpdate) => {
    settings.value = wsSettings;
  };
  const updateATFLFromWebsocket = (atfl: AprilTagFieldLayout) => {
    activeATFL.value = atfl;
  };
  const updateCamerasFromWebsocket = (wsCameras: CameraConfig[]) => {
    cameras.value = wsCameras;
  };

  // TODO MAKE EVERYTHING UPDATE STORE

  const updateNetworkSettings = (networkSettings: ConfigurableNetworkSettings) => {
    const changingStaticIP = networkSettings.connectionType === NetworkConnectionType.Static && networkSettings.staticIp !== settings.value?.network.staticIp;

    axios.post("/settings/network", networkSettings)
      .then((response) => {
        // TODO snackbar message about success

        if (changingStaticIP) {
        // TODO snackbar message about changing IP address
          setTimeout(() => {
            window.open(`http://${networkSettings.staticIp}:${window.location.port}/`);
          }, 2000);
        }

        // Technically not needed because the backend will broadcast the settings change
        if (settings.value) {
          settings.value.network = {
            ntServerAddress: networkSettings.ntServerAddress,
            connectionType: networkSettings.connectionType,
            staticIp: networkSettings.staticIp,
            hostname: networkSettings.hostname,
            runNTServer: networkSettings.runNTServer,
            shouldManage: networkSettings.shouldManage,
            canManage: settings.value.network.canManage,
            networkManagerIface: networkSettings.networkManagerIface,
            setStaticCommand: networkSettings.setStaticCommand,
            setDHCPcommand: networkSettings.setDHCPcommand,
            networkInterfaceNames: settings.value.network.networkInterfaceNames,
            networkingDisabled: settings.value.network.networkingDisabled,
            shouldPublishProto: networkSettings.shouldPublishProto
          };
        }
      })
      .catch((error) => {
        if (error.response) {
          if (error.status === 504 || changingStaticIP) {
          // TODO
          // useStateStore().showSnackbarMessage({
          //   color: "error",
          //   message: `Connection lost! Try the new static IP at ${useSettingsStore().network.staticIp}:5800 or ${
          //     useSettingsStore().network.hostname
          //   }:5800?`
          // });
          } else {
          // TODO
          // useStateStore().showSnackbarMessage({
          //   color: "error",
          //   message: error.response.data.text || error.response.data
          // });
          }
        } else if (error.request) {
        // TODO
        // useStateStore().showSnackbarMessage({
        //   color: "error",
        //   message: "Error while trying to process the request! The backend didn't respond."
        // });
        } else {
        // TODO
        // useStateStore().showSnackbarMessage({
        //   color: "error",
        //   message: "An error occurred while trying to process the request."
        // });
        }
      });
  };
  const updateMiscSettings = (miscSettings: ConfigurableMiscellaneousSettings) => {
    axios.post("/settings/misc", miscSettings)
      .then((response) => {
        // TODO
        // useStateStore().showSnackbarMessage({
        //   message: response.data.text || response.data,
        //   color: "success"
        // });

        // Technically not needed because the backend will broadcast the settings change
        if (settings.value) {
          settings.value.misc = {
            matchCamerasOnlyByPath: miscSettings.matchCamerasOnlyByPath
          };
        }
      })
      .catch((error) => {
        if (error.response) {
          // TODO
          // useStateStore().showSnackbarMessage({
          //   color: "error",
          //   message: error.response.data.text || error.response.data
          // });
        } else if (error.request) {
          // TODO
          // useStateStore().showSnackbarMessage({
          //   color: "error",
          //   message: "Error while trying to process the request! The backend didn't respond."
          // });
        } else {
          // TODO
          // useStateStore().showSnackbarMessage({
          //   color: "error",
          //   message: "An error occurred while trying to process the request."
          // });
        }
      });
  };
  const updateLEDBrightness = (brightness: number) => {
    const payload = {
      ledPercentage: brightness
    };
    clientStore.websocket?.send(payload, true);
  };

  const changeActivePipeline = (newActivePipelineIndex: number, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      changeActivePipeline: {
        cameraIndex: cameraIndex,
        newActivePipelineIndex: newActivePipelineIndex
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const setDriverMode = (driverMode: boolean, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }

    const payload = {
      driverMode: {
        cameraIndex: cameraIndex,
        driverMode: driverMode
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const changeCameraNickname = (cameraNickname: string, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }

    const payload = {
      changeCameraNickname: {
        cameraIndex: cameraIndex,
        nickname: cameraNickname
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const changePipelineNickname = (pipelineIndex: number, pipelineNickname: string, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }

    const payload = {
      changePipelineNickname: {
        cameraIndex: cameraIndex,
        pipelineIndex: pipelineIndex,
        nickname: pipelineNickname
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const createNewPipeline = (nickname: string, type: PipelineType, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      createNewPipeline: {
        cameraIndex: cameraIndex,
        nickname: nickname,
        type: type
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const duplicatePipeline = (
    targetPipelineIndex: number,
    newPipeNickname: string,
    setActive: boolean,
    cameraIndex?: number
  ) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      duplicatePipeline: {
        cameraIndex: cameraIndex,
        nickname: newPipeNickname,
        targetIndex: targetPipelineIndex,
        setActive: setActive
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const resetPipeline = (targetPipelineIndex: number, type: UserPipelineType | undefined, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      resetPipeline: {
        cameraIndex: cameraIndex,
        pipelineIndex: targetPipelineIndex,
        type: type
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const deletePipeline = (targetPipelineIndex: number, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      deletePipeline: {
        cameraIndex: cameraIndex,
        pipelineIndex: targetPipelineIndex
      }
    };
    clientStore.websocket?.send(payload, true);
  };

  const startCalibration = (request: StartCalibrationPayload) => {
    const payload = {
      startCalib: request
    };

    clientStore.websocket?.send(payload, true);
  };
  const takeCalibrationSnapshot = (cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      takeCalibSnapshot: {
        cameraIndex: cameraIndex
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const attemptCancelCalibration = (cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      cancelCalib: {
        cameraIndex: cameraIndex
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const attemptCompleteCalibration = (cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      completeCalib: {
        cameraIndex: cameraIndex
      }
    };
    clientStore.websocket?.send(payload, true);
  };

  const saveInputSnapshot = (cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      saveInputSnapshot: {
        cameraIndex: cameraIndex
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const saveOutputSnapshot = (cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      saveOutputSnapshot: {
        cameraIndex: cameraIndex
      }
    };
    clientStore.websocket?.send(payload, true);
  };

  const changeCameraFOV = (newFoV: number, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      changeCameraFOV: {
        cameraIndex: cameraIndex,
        fov: newFoV
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const updateCameraQuirks = (quirksToChange: Partial<Record<ValidQuirks, boolean>>, updateStore: boolean, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      changeCameraQuirks: {
        cameraIndex: cameraIndex,
        quirks: quirksToChange
      }
    };

    clientStore.websocket?.send(payload, true);

    if (!updateStore) return;

    const cameraQuirks = cameras.value?.find((v) => v.cameraIndex === cameraIndex)?.cameraQuirks.quirks;
    if (!cameraQuirks) return;
    for (const k in quirksToChange) {
      cameraQuirks[k as ValidQuirks] = quirksToChange[k as ValidQuirks] as boolean;
    }
  };
  const setArduCamModel = (model: ValidQuirks.ArduOV2311 | ValidQuirks.ArduOV9281 | ValidQuirks.ArduOV9782, updateStore: boolean, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }

    let quirksToChange: Partial<Record<ValidQuirks, boolean>>;
    switch (model) {
      case ValidQuirks.ArduOV2311:
        quirksToChange = {
          ArduOV2311: true,
          ArduOV9281: false,
          ArduOV9782: false
        };
        break;
      case ValidQuirks.ArduOV9281:
        quirksToChange = {
          ArduOV2311: false,
          ArduOV9281: true,
          ArduOV9782: false
        };
        break;
      case ValidQuirks.ArduOV9782:
        quirksToChange = {
          ArduOV2311: false,
          ArduOV9281: false,
          ArduOV9782: true
        };
        break;
    }

    updateCameraQuirks(quirksToChange, updateStore, cameraIndex);
  };

  const restartProgram = () => {
    const payload = {
      restartProgram: true
    };
    clientStore.websocket?.send(payload, true);
    clientStore.websocket?.forceReconnectAfterTimeout(4000);
  };
  const restartDevice = () => {
    const payload = {
      restartDevice: true
    };
    clientStore.websocket?.send(payload, true);
    clientStore.websocket?.forceReconnectAfterTimeout(4000);
  };
  const publishMetrics = () => {
    const payload = {
      publishMetrics: true
    };
    clientStore.websocket?.send(payload, true);
  };

  const importCalibration = (calibration: CameraCalibrationCoefficients, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      importCalibFromData: {
        cameraIndex: cameraIndex,
        calibration: calibration
      }
    };
    clientStore.websocket?.send(payload, true);
  };
  const importCalibDBCalibration = (calibration: string, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    const payload = {
      importCalibFromCalibDB: {
        cameraIndex: cameraIndex,
        calibration: calibration
      }
    };
    clientStore.websocket?.send(payload, true);
  };

  const updatePipelineSettings = (
    cameraIndex: number,
    pipelineIndex: number,
    configuredSettings: Partial<ConfigurableUserPipelineSettings>,
    updateStore: boolean,
    updateBackend: boolean
  ) => {
    if (updateStore) {
      const targetCameraConfig = cameras.value?.find((v) => v?.cameraIndex === cameraIndex);
      const targetPipelineSettings = targetCameraConfig?.pipelineSettings.find(
        (v) => v?.pipelineIndex === pipelineIndex
      );

      if (
        !targetPipelineSettings ||
        targetPipelineSettings.pipelineType === PipelineType.DriverMode ||
        targetPipelineSettings.pipelineType === PipelineType.Calib3d
      ) {
        throw new Error("Attempting to mutate non-user pipeline settings");
      }

      for (const settingProp in configuredSettings) {
        if (!Object.prototype.hasOwnProperty.call(targetPipelineSettings, settingProp)) {
          throw new Error("Attempting to mutate setting prop that isn't in pipeline");
        }

        const settingVal = configuredSettings[settingProp as keyof ConfigurableUserPipelineSettings];
        (targetPipelineSettings[settingProp as keyof ConfigurableUserPipelineSettings] as typeof settingVal) =
          settingVal;
      }
    }

    if (updateBackend) {
      const mutationPayload = {
        changePipelineSettings: {
          cameraIndex: cameraIndex,
          pipelineIndex: pipelineIndex,
          configuredSettings: configuredSettings
        }
      };
      clientStore.websocket?.send(mutationPayload, true);
    }
  };
  const updateCurrentPipelineSettings = (configuredSettings: Partial<ConfigurableUserPipelineSettings>, updateStore: boolean, updateBackend: boolean) => {
    const currentCamSettings = currentCameraSettings.value;
    updatePipelineSettings(currentCamSettings!.cameraIndex, currentCamSettings!.activePipelineIndex, configuredSettings, updateStore, updateBackend);
  };
  const takeRobotOffsetPoint = (type: RobotOffsetOperationMode, cameraIndex?: number) => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    // TODO do i need to handle pipeline index for this?
    const payload = {
      robotOffsetPoint: {
        cameraIndex: cameraIndex,
        type: type
      }
    };
    clientStore.websocket?.send(payload, true);
  };

  const getCalibrationCoeffs = (
    resolution: Resolution,
    cameraIndex?: number
  ): CameraCalibrationCoefficients | undefined => {
    if (!cameraIndex) {
      cameraIndex = clientStore.currentCameraIndex;
    }
    return cameras.value
      ?.find((v) => v.cameraIndex === cameraIndex)
      ?.calibrations.find((v) => resolutionsAreEqual(v.resolution, resolution));
  };
  const driverMode = computed<boolean>({
    get: () => driverMode.value,
    set: (driverMode: boolean) => setDriverMode(driverMode)
  });

  return {
    instanceConfig: readonly(instanceConfig),
    platformMetrics: readonly(platformMetrics),
    settings: settings,
    activeATFL: readonly(activeATFL),
    cameras: readonly(cameras),
    networkInterfaceNames,
    getCameraSettingsFromIndex,
    getPipelineSettingsFromIndex,
    getActivePipelineSettingsByCameraIndex,
    currentCameraSettings,
    currentPipelineSettings,
    currentPipelineType,
    currentVideoFormat,
    isCurrentVideoFormatCalibrated,
    cameraNames,
    currentCameraName,
    pipelineNames,
    currentPipelineName,
    isDriverMode,
    isCalibMode,
    isCSICamera,
    requestMetricsUpdate,
    updatePlatformMetricsFromWebsocket,
    updateInstanceConfigFromWebsocket,
    updateSettingsFromWebsocket,
    updateATFLFromWebsocket,
    updateCamerasFromWebsocket,
    updateNetworkSettings,
    updateMiscSettings,
    updateLEDBrightness,
    changeActivePipeline,
    setDriverMode,
    changeCameraNickname,
    changePipelineNickname,
    createNewPipeline,
    duplicatePipeline,
    resetPipeline,
    deletePipeline,
    startCalibration,
    takeCalibrationSnapshot,
    attemptCancelCalibration,
    attemptCompleteCalibration,
    saveInputSnapshot,
    saveOutputSnapshot,
    changeCameraFOV,
    updateCameraQuirks,
    setArduCamModel,
    restartProgram,
    restartDevice,
    publishMetrics,
    importCalibration,
    importCalibDBCalibration,
    updatePipelineSettings,
    updateCurrentPipelineSettings,
    takeRobotOffsetPoint,
    getCalibrationCoeffs,
    driverMode
  };
});
