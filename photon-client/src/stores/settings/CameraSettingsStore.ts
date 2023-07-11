import { defineStore } from "pinia";
import type {
    CalibrationBoardTypes,
    CameraCalibrationResult,
    CameraSettings,
    ConfigurableCameraSettings,
    RobotOffsetType,
    VideoFormat
} from "@/types/SettingTypes";
import { PlaceholderCameraSettings } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import type { WebsocketCameraSettingsUpdate } from "@/types/WebsocketDataTypes";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import type {
    ActiveConfigurablePipelineSettings,
    ActivePipelineSettings
} from "@/types/PipelineTypes";
import { PipelineType } from "@/types/PipelineTypes";
import axios from "axios";

interface CameraSettingsStore {
    cameras: CameraSettings[]
}

export const useCameraSettingsStore = defineStore("cameraSettings", {
    state: (): CameraSettingsStore => ({
        cameras: [
            PlaceholderCameraSettings
        ]
    }),
    getters: {
        currentCameraSettings(): CameraSettings {
            return this.cameras[useStateStore().currentCameraIndex];
        },
        currentPipelineSettings(): ActivePipelineSettings {
          return this.currentCameraSettings.pipelineSettings;
        },
        currentPipelineType(): PipelineType {
            return this.currentPipelineSettings.pipelineType;
        },
        // This method only exists due to just how lazy I am and my dislike of consolidating the pipeline type enums (which mind you, suck as is)
        currentWebsocketPipelineType(): Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode> {
            switch (this.currentPipelineType) {
                case PipelineType.Reflective:
                    return WebsocketPipelineType.Reflective;
                case PipelineType.ColoredShape:
                    return WebsocketPipelineType.ColoredShape;
                case PipelineType.AprilTag:
                    return WebsocketPipelineType.AprilTag;
                case PipelineType.Aruco:
                    return WebsocketPipelineType.Aruco;
            }
        },
        currentVideoFormat(): VideoFormat {
            return this.currentCameraSettings.validVideoFormats[this.currentPipelineSettings.cameraVideoModeIndex];
        },
        isCurrentVideoFormatCalibrated(): boolean {
            return this.currentCameraSettings.completeCalibrations.some(v => v.resolution == this.currentVideoFormat.resolution);
        },
        cameraNames(): string[] {
            return this.cameras.map(c => c.nickname);
        },
        pipelineNames(): string[] {
            return this.currentCameraSettings.pipelineNicknames;
        },
        isDriverMode(): boolean {
            return this.currentCameraSettings.currentPipelineIndex === WebsocketPipelineType.DriverMode;
        },
        isCalibrationMode(): boolean {
            return this.currentCameraSettings.currentPipelineIndex == WebsocketPipelineType.Calib3d;
        }
    },
    actions: {
        updateCameraSettingsFromWebsocket(data: WebsocketCameraSettingsUpdate[]) {
            this.cameras = data.map<CameraSettings>((d) => ({
                nickname: d.nickname,
                fov: {
                    value: d.fov,
                    managedByVendor: !d.isFovConfigurable
                },
                stream: {
                    inputPort: d.inputStreamPort,
                    outputPort: d.outputStreamPort
                },
                validVideoFormats: Object.keys(d.videoFormatList)
                    .sort((a, b) => parseInt(a) - parseInt(b)) // TODO, does the backend already return this sorted
                    .map<VideoFormat>((k, i) => ({ ...d.videoFormatList[k], index: i })),
                completeCalibrations: d.calibrations.map<CameraCalibrationResult>(calib => ({
                    resolution: {
                        height: calib.height,
                        width: calib.width
                    },
                    distCoeffs: calib.distCoeffs,
                    standardDeviation: calib.standardDeviation,
                    perViewErrors: calib.perViewErrors,
                    intrinsics: calib.intrinsics
                })),
                pipelineNicknames: d.pipelineNicknames,
                currentPipelineIndex: d.currentPipelineIndex,
                pipelineSettings: d.currentPipelineSettings
            }));
        },
        /**
         * Update the configurable camera settings.
         *
         * @param data camera settings to save.
         * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
         * @param cameraIndex the index of the camera.
         */
        updateCameraSettings(data: ConfigurableCameraSettings, updateStore = true, cameraIndex: number = useStateStore().currentCameraIndex) {
            // The camera settings endpoint doesn't actually require all data, instead, it needs key data such as the FOV
            const payload = {
                settings: {
                    ...data
                },
                index: cameraIndex
            };
            if(updateStore) {
                this.currentCameraSettings.fov.value = data.fov;
            }
            return axios.post("/settings/camera", payload);
        },
        /**
         * Create a new Pipeline for the provided camera.
         *
         * @param newPipelineName the name of the new pipeline.
         * @param pipelineType the type of the new pipeline. Cannot be {@link WebsocketPipelineType.Calib3d} or {@link WebsocketPipelineType.DriverMode}.
         * @param cameraIndex the index of the camera
         */
        createNewPipeline(newPipelineName: string, pipelineType: Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode>, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                addNewPipeline: [newPipelineName, pipelineType],
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Modify the settings of the currently selected pipeline of the provided camera.
         *
         * @param settings settings to modify. The type of the settings should match the currently selected pipeline type.
         * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
         * @param cameraIndex the index of the camera
         */
        changeCurrentPipelineSetting(settings: ActiveConfigurablePipelineSettings, updateStore = true, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                changePipelineSetting: {
                    ...settings,
                    cameraIndex: cameraIndex
                }
            };
            if(updateStore) {
                this.cameras[cameraIndex].pipelineSettings = {
                    ...this.cameras[cameraIndex].pipelineSettings,
                    ...settings
                };
            }
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Change the nickname of the currently selected pipeline of the provided camera.
         *
         * @param newName the new nickname for the camera.
         * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
         * @param cameraIndex the index of the camera
         */
        changeCurrentPipelineNickname(newName: string, updateStore = true, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                changePipelineName: newName,
                cameraIndex: cameraIndex
            };
            if(updateStore) {
                this.cameras[cameraIndex].pipelineSettings.pipelineNickname = newName;
            }
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Modify the Pipeline type of the currently selected pipeline of the provided camera. This overwrites the current pipeline's settings when the backend resets the current pipeline settings.
         *
         * @param type the pipeline type to set.  Cannot be {@link WebsocketPipelineType.Calib3d} or {@link WebsocketPipelineType.DriverMode}.
         * @param cameraIndex the index of the camera.
         */
        changeCurrentPipelineType(type: Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode>, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                pipelineType: type,
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Change the index of the pipeline of the currently selected camera.
         *
         * @param index pipeline index to set.
         * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
         * @param cameraIndex the index of the camera.
         */
        changeCurrentPipelineIndex(index: number, updateStore = true, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                currentPipeline: index,
                cameraIndex: cameraIndex
            };
            if(updateStore) {
                this.cameras[cameraIndex].currentPipelineIndex = index;
            }
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Change the currently selected pipeline of the provided camera.
         *
         * @param cameraIndex the index of the camera's pipeline to change.
         */
        deleteCurrentPipeline(cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                deleteCurrentPipeline: {},
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Duplicate the pipeline at the provided index.
         *
         * @param pipelineIndex index of the pipeline to duplicate.
         * @param cameraIndex the index of the camera.
         */
        duplicatePipeline(pipelineIndex: number, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                duplicatePipeline: pipelineIndex,
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Change the currently set camera
         *
         * @param cameraIndex the index of the camera to set as the current camera.
         * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
         */
        setCurrentCameraIndex(cameraIndex: number, updateStore = true) {
            const payload = {
                currentCamera: cameraIndex
            };
            if(updateStore) {
                useStateStore().currentCameraIndex = cameraIndex;
            }
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Change the nickname of the provided camera.
         *
         * @param newName the new nickname of the camera.
         * @param updateStore whether or not to update the store. This is useful if the input field already models the store reference.
         * @param cameraIndex the index of the camera.
         * @return HTTP request promise to the backend
         */
        changeCameraNickname(newName: string,  updateStore = true, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                name: newName,
                cameraIndex: cameraIndex
            };
            if(updateStore) {
                this.currentCameraSettings.nickname = newName;
            }
            return axios.post("/settings/camera/setNickname", payload);
        },
        /**
         * Start the 3D calibration process for the provided camera.
         *
         * @param calibrationInitData initialization calibration data.
         * @param cameraIndex the index of the camera.
         */
        startPnPCalibration(calibrationInitData: {
            squareSizeIn: number,
            patternWidth: number,
            patternHeight: number,
            boardType: CalibrationBoardTypes
        }, cameraIndex: number = useStateStore().currentCameraIndex) {
            const stateCalibData = useStateStore().calibrationData;
            const payload = {
                startPnpCalibration: {
                    count: stateCalibData.imageCount,
                    minCount: stateCalibData.minimumImageCount,
                    hasEnough: stateCalibData.hasEnoughImages,
                    videoModeIndex: stateCalibData.videoFormatIndex,
                    ...calibrationInitData
                },
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * End the 3D calibration process for the provided camera.
         *
         * @param cameraIndex the index of the camera
         * @return HTTP request promise to the backend
         */
        endPnPCalibration(cameraIndex: number = useStateStore().currentCameraIndex) {
           return axios.post("/calibration/end", { index: cameraIndex });
        },
        /**
         * Import calibration data that was computed using CalibDB.
         *
         * @param data Data from the uploaded CalibDB config
         * @param cameraIndex the index of the camera
         */
        importCalibDB(data: { payload: string, filename: string }, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                ...data,
                cameraIndex: cameraIndex
            };
            return axios.post("/calibration/importFromCalibDB", payload, { headers: { "Content-Type": "text/plain" } });
        },
        /**
         * Take a snapshot for the calibration processes
         *
         * @param takeSnapshot whether or not to take a snapshot. Defaults to true
         * @param cameraIndex the index of the camera that is currently in the calibration process
         */
        takeCalibrationSnapshot(takeSnapshot = true, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                takeCalibrationSnapshot: takeSnapshot,
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Set the robot offset mode type.
         *
         * @param type Offset type to take.
         * @param cameraIndex the index of the camera.
         */
        takeRobotOffsetPoint(type: RobotOffsetType, cameraIndex: number = useStateStore().currentCameraIndex) {
            const payload = {
                robotOffsetPoint: type,
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        }
    }
});
