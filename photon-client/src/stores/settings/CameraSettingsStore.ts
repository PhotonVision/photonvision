import { defineStore } from "pinia";
import type {
  CalibrationTagFamilies,
  CalibrationBoardTypes,
  CameraCalibrationResult,
  UiCameraConfiguration,
  CameraSettingsChangeRequest,
  Resolution,
  RobotOffsetType,
  VideoFormat
} from "@/types/SettingTypes";
import { PlaceholderCameraSettings } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import type { WebsocketCameraSettingsUpdate } from "@/types/WebsocketDataTypes";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import type { ActiveConfigurablePipelineSettings, ActivePipelineSettings, PipelineType } from "@/types/PipelineTypes";
import axios from "axios";
import { resolutionsAreEqual } from "@/lib/PhotonUtils";

interface CameraSettingsStore {
  cameras: { [key: string]: UiCameraConfiguration };
}

export const useCameraSettingsStore = defineStore("cameraSettings", {
  state: (): CameraSettingsStore => ({
    cameras: { [PlaceholderCameraSettings.uniqueName]: PlaceholderCameraSettings }
  }),
  getters: {
    // TODO update types to update this value being undefined. This would be a decently large change.
    currentCameraSettings(): UiCameraConfiguration {
      const currentCameraUniqueName = useStateStore().currentCameraUniqueName;
      return this.cameras[currentCameraUniqueName] || PlaceholderCameraSettings;
    },
    currentPipelineSettings(): ActivePipelineSettings {
      return this.currentCameraSettings.pipelineSettings;
    },
    currentPipelineType(): PipelineType {
      return this.currentPipelineSettings.pipelineType;
    },
    // This method only exists due to just how lazy I am and my dislike of consolidating the pipeline type enums (which mind you, suck as is)
    currentWebsocketPipelineType(): WebsocketPipelineType {
      return this.currentPipelineType - 2;
    },
    currentVideoFormat(): VideoFormat {
      return this.currentCameraSettings.validVideoFormats[this.currentPipelineSettings.cameraVideoModeIndex];
    },
    isCurrentVideoFormatCalibrated(): boolean {
      return this.currentCameraSettings.completeCalibrations.some((v) =>
        resolutionsAreEqual(v.resolution, this.currentVideoFormat.resolution)
      );
    },
    cameraNames(): string[] {
      return Object.values(this.cameras).map((c) => c.nickname);
    },
    cameraUniqueNames(): string[] {
      return Object.values(this.cameras).map((c) => c.uniqueName);
    },
    currentCameraName(): string {
      return this.cameras[useStateStore().currentCameraUniqueName].nickname;
    },
    pipelineNames(): string[] {
      return this.currentCameraSettings.pipelineNicknames;
    },
    currentPipelineName(): string {
      return this.pipelineNames[useStateStore().currentCameraUniqueName];
    },
    isDriverMode(): boolean {
      return this.currentCameraSettings.currentPipelineIndex === WebsocketPipelineType.DriverMode;
    },
    isCalibrationMode(): boolean {
      return this.currentCameraSettings.currentPipelineIndex == WebsocketPipelineType.Calib3d;
    },
    isCSICamera(): boolean {
      return this.currentCameraSettings.isCSICamera;
    },
    minExposureRaw(): number {
      return this.currentCameraSettings.minExposureRaw;
    },
    maxExposureRaw(): number {
      return this.currentCameraSettings.maxExposureRaw;
    },
    minWhiteBalanceTemp(): number {
      return this.currentCameraSettings.minWhiteBalanceTemp;
    },
    maxWhiteBalanceTemp(): number {
      return this.currentCameraSettings.maxWhiteBalanceTemp;
    },
    isConnected(): boolean {
      return this.currentCameraSettings.isConnected;
    },
    hasConnected(): boolean {
      return this.currentCameraSettings.hasConnected;
    }
  },
  actions: {
    updateCameraSettingsFromWebsocket(data: WebsocketCameraSettingsUpdate[]) {
      const configuredCameras = data.reduce<{ [key: string]: UiCameraConfiguration }>((acc, d) => {
        acc[d.uniqueName] = {
          cameraPath: d.cameraPath,
          nickname: d.nickname,
          uniqueName: d.uniqueName,
          fov: {
            value: d.fov,
            managedByVendor: !d.isFovConfigurable
          },
          stream: {
            inputPort: d.inputStreamPort,
            outputPort: d.outputStreamPort
          },
          validVideoFormats: Object.entries(d.videoFormatList)
            .sort(([firstKey], [secondKey]) => parseInt(firstKey) - parseInt(secondKey))
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            .map<VideoFormat>(([k, v], i) => ({
              resolution: {
                width: v.width,
                height: v.height
              },
              fps: v.fps,
              pixelFormat: v.pixelFormat,
              index: v.index || i,
              diagonalFOV: v.diagonalFOV,
              horizontalFOV: v.horizontalFOV,
              verticalFOV: v.verticalFOV,
              standardDeviation: v.standardDeviation,
              mean: v.mean
            })),
          completeCalibrations: d.calibrations,
          isCSICamera: d.isCSICamera,
          minExposureRaw: d.minExposureRaw,
          maxExposureRaw: d.maxExposureRaw,
          pipelineNicknames: d.pipelineNicknames,
          currentPipelineIndex: d.currentPipelineIndex,
          pipelineSettings: d.currentPipelineSettings,
          cameraQuirks: d.cameraQuirks,
          minWhiteBalanceTemp: d.minWhiteBalanceTemp,
          maxWhiteBalanceTemp: d.maxWhiteBalanceTemp,
          matchedCameraInfo: d.matchedCameraInfo,
          isConnected: d.isConnected,
          hasConnected: d.hasConnected
        };
        return acc;
      }, {});
      this.cameras =
        Object.keys(configuredCameras).length > 0
          ? configuredCameras
          : { [PlaceholderCameraSettings.uniqueName]: PlaceholderCameraSettings };

      // Ensure currentCameraUniqueName is valid
      if (!this.cameras[useStateStore().currentCameraUniqueName]) {
        useStateStore().currentCameraUniqueName = Object.keys(this.cameras)[0];
      }
    },

    /**
     * Update the configurable camera settings.
     *
     * @param data camera settings to save.
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    updateCameraSettings(
      data: CameraSettingsChangeRequest,
      cameraUniqueName: String = useStateStore().currentCameraUniqueName
    ) {
      // The camera settings endpoint doesn't actually require all data, instead, it needs key data such as the FOV
      const payload = {
        settings: {
          ...data
        },
        cameraUniqueName: cameraUniqueName
      };
      return axios.post("/settings/camera", payload);
    },
    /**
     * Create a new Pipeline for the provided camera.
     *
     * @param newPipelineName the name of the new pipeline.
     * @param pipelineType the type of the new pipeline. Cannot be {@link WebsocketPipelineType.Calib3d} or {@link WebsocketPipelineType.DriverMode}.
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    createNewPipeline(
      newPipelineName: string,
      pipelineType: Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode>,
      cameraUniqueName: String = useStateStore().currentCameraUniqueName
    ) {
      const payload = {
        addNewPipeline: [newPipelineName, pipelineType],
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Modify the settings of the currently selected pipeline of the provided camera.
     *
     * @param settings settings to modify. The type of the settings should match the currently selected pipeline type.
     * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    changeCurrentPipelineSetting(
      settings: ActiveConfigurablePipelineSettings,
      updateStore = true,
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ) {
      const payload = {
        changePipelineSetting: {
          ...settings,
          cameraUniqueName: cameraUniqueName
        }
      };
      if (updateStore) {
        this.changePipelineSettingsInStore(settings, cameraUniqueName);
      }
      useStateStore().websocket?.send(payload, true);
    },
    changePipelineSettingsInStore(
      settings: Partial<ActivePipelineSettings>,
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ) {
      Object.entries(settings).forEach(([k, v]) => {
        this.cameras[cameraUniqueName].pipelineSettings[k] = v;
      });
    },
    /**
     * Change the nickname of the currently selected pipeline of the provided camera.
     *
     * @param newName the new nickname for the camera.
     * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    changeCurrentPipelineNickname(
      newName: string,
      updateStore = true,
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ) {
      const payload = {
        changePipelineName: newName,
        cameraUniqueName: cameraUniqueName
      };
      if (updateStore) {
        this.cameras[cameraUniqueName].pipelineSettings.pipelineNickname = newName;
      }
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Modify the Pipeline type of the currently selected pipeline of the provided camera. This overwrites the current pipeline's settings when the backend resets the current pipeline settings.
     *
     * @param type the pipeline type to set.  Cannot be {@link WebsocketPipelineType.Calib3d} or {@link WebsocketPipelineType.DriverMode}.
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    changeCurrentPipelineType(
      type: Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode>,
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ) {
      const payload = {
        pipelineType: type,
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Change the index of the pipeline of the currently selected camera.
     *
     * @param index pipeline index to set.
     * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    changeCurrentPipelineIndex(
      index: number,
      updateStore = true,
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ) {
      const payload = {
        currentPipeline: index,
        cameraUniqueName: cameraUniqueName
      };
      if (updateStore) {
        if (
          this.cameras[cameraUniqueName].currentPipelineIndex !== -1 &&
          this.cameras[cameraUniqueName].currentPipelineIndex !== -2
        ) {
          this.cameras[cameraUniqueName].lastPipelineIndex = this.cameras[cameraUniqueName].currentPipelineIndex;
        }
        this.cameras[cameraUniqueName].currentPipelineIndex = index;
      }
      useStateStore().websocket?.send(payload, true);
    },
    setDriverMode(isDriverMode: boolean, cameraUniqueName: string = useStateStore().currentCameraUniqueName) {
      const payload = {
        driverMode: isDriverMode,
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Change the currently selected pipeline of the provided camera.
     *
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    deleteCurrentPipeline(cameraUniqueName: string = useStateStore().currentCameraUniqueName) {
      const payload = {
        deleteCurrentPipeline: {},
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Duplicate the pipeline at the provided index.
     *
     * @param pipelineIndex index of the pipeline to duplicate.
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    duplicatePipeline(pipelineIndex: number, cameraUniqueName: string = useStateStore().currentCameraUniqueName) {
      const payload = {
        duplicatePipeline: pipelineIndex,
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Change the currently set camera
     *
     * @param cameraUniqueName the unique name of the camera.
     * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
     */
    setCurrentCameraUniqueName(cameraUniqueName: string, updateStore = true) {
      const payload = {
        cameraUniqueName: cameraUniqueName
      };
      if (updateStore) {
        useStateStore().currentCameraUniqueName = cameraUniqueName;
      }
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Change the nickname of the provided camera.
     *
     * @param newName the new nickname of the camera.
     * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
     * @param cameraUniqueNamendex the unique name of the camera.
     * @return HTTP request promise to the backend
     */
    changeCameraNickname(
      newName: string,
      updateStore = true,
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ) {
      const payload = {
        name: newName,
        cameraUniqueName: cameraUniqueName
      };
      if (updateStore) {
        this.currentCameraSettings.nickname = newName;
      }
      return axios.post("/settings/camera/setNickname", payload);
    },
    /**
     * Start the 3D calibration process for the provided camera.
     *
     * @param calibrationInitData initialization calibration data.
     * @param cameraUniqueNamendex the unique name of the camera.
     */
    startPnPCalibration(
      calibrationInitData: {
        squareSizeIn: number;
        markerSizeIn: number;
        patternWidth: number;
        patternHeight: number;
        boardType: CalibrationBoardTypes;
        useOldPattern: boolean;
        tagFamily: CalibrationTagFamilies;
      },
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ) {
      const stateCalibData = useStateStore().calibrationData;
      const payload = {
        startPnpCalibration: {
          count: stateCalibData.imageCount,
          minCount: stateCalibData.minimumImageCount,
          hasEnough: stateCalibData.hasEnoughImages,
          videoModeIndex: stateCalibData.videoFormatIndex,
          ...calibrationInitData
        },
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * End the 3D calibration process for the provided camera.
     *
     * @param cameraUniqueName the unique name of the camera.
     * @return HTTP request promise to the backend
     */
    endPnPCalibration(cameraUniqueName: string = useStateStore().currentCameraUniqueName) {
      return axios.post("/calibration/end", { cameraUniqueName: cameraUniqueName });
    },

    importCalibrationFromData(
      data: { calibration: CameraCalibrationResult },
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ) {
      const payload = {
        ...data,
        cameraUniqueName: cameraUniqueName
      };
      return axios.post("/calibration/importFromData", payload);
    },
    /**
     * Take a snapshot for the calibration processes
     *
     * @param cameraUniqueName the unique name of the camera that is currently in the calibration process
     */
    takeCalibrationSnapshot(cameraUniqueName: string = useStateStore().currentCameraUniqueName) {
      const payload = {
        takeCalibrationSnapshot: true,
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Save a snapshot of the input frame of the camera.
     *
     * @param cameraUniqueName the unique name of the camera
     */
    saveInputSnapshot(cameraUniqueName: string = useStateStore().currentCameraUniqueName) {
      const payload = {
        saveInputSnapshot: true,
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Save a snapshot of the output frame of the camera.
     *
     * @param cameraUniqueName the unique name of the camera
     */
    saveOutputSnapshot(cameraUniqueName: string = useStateStore().currentCameraUniqueName) {
      const payload = {
        saveOutputSnapshot: true,
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    /**
     * Set the robot offset mode type.
     *
     * @param type Offset type to take.
     * @param cameraUniqueName the unique name of the camera
     */
    takeRobotOffsetPoint(type: RobotOffsetType, cameraUniqueName: string = useStateStore().currentCameraUniqueName) {
      const payload = {
        robotOffsetPoint: type,
        cameraUniqueName: cameraUniqueName
      };
      useStateStore().websocket?.send(payload, true);
    },
    getCalibrationCoeffs(
      resolution: Resolution,
      cameraUniqueName: string = useStateStore().currentCameraUniqueName
    ): CameraCalibrationResult | undefined {
      return this.cameras[cameraUniqueName].completeCalibrations.find((v) =>
        resolutionsAreEqual(v.resolution, resolution)
      );
    },
    getCalImageUrl(
      host: string,
      resolution: Resolution,
      idx: number,
      cameraUniqueName = useStateStore().currentCameraUniqueName
    ) {
      const url = new URL(`http://${host}/api/utils/getCalSnapshot`);
      url.searchParams.set("width", Math.round(resolution.width).toFixed(0));
      url.searchParams.set("height", Math.round(resolution.height).toFixed(0));
      url.searchParams.set("snapshotIdx", Math.round(idx).toFixed(0));
      url.searchParams.set("cameraUniqueName", cameraUniqueName.replace(" ", "").trim().toLowerCase());

      return url.href;
    },
    getCalJSONUrl(host: string, resolution: Resolution, cameraUniqueName = useStateStore().currentCameraUniqueName) {
      const url = new URL(`http://${host}/api/utils/getCalibrationJSON`);
      url.searchParams.set("width", Math.round(resolution.width).toFixed(0));
      url.searchParams.set("height", Math.round(resolution.height).toFixed(0));
      url.searchParams.set("cameraUniqueName", cameraUniqueName.replace(" ", "").trim().toLowerCase());

      return url.href;
    }
  }
});
