import {defineStore} from "pinia";
import type {ActivePipelineSettings, CameraSettings} from "@/types/SettingTypes";
import {useStateStore} from "@/stores/StateStore";
import type {WebsocketCameraSettingsUpdate} from "@/types/WebsocketDataTypes";
import type {CameraCalibrationResult, VideoFormat} from "@/types/SettingTypes";
import type {WebsocketPipelineType} from "@/types/WebsocketDataTypes";
import type {
    ConfigurableAprilTagPipelineSettings,
    ConfigurableColoredShapePipelineSettings,
    ConfigurableReflectivePipelineSettings
} from "@/types/PipelineTypes";
import type {CalibrationBoardTypes} from "@/types/SettingTypes";
import type {RobotOffsetType} from "@/types/SettingTypes";
import {PlaceholderCameraSettings} from "@/types/SettingTypes";

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
        currentCameraSettings(): CameraSettings | undefined {
            const currentCameraIndex = useStateStore().currentCameraIndex;

            if(this.cameras.length === 0 || currentCameraIndex === undefined) {
                return undefined;
            }

            return this.cameras[currentCameraIndex];
        }
    },
    actions: {
        updateCameraSettingsFromWebsocket(data: WebsocketCameraSettingsUpdate[]) {
            this.$patch({
                cameras: data.map<CameraSettings>((d) => ({
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
                        .map<VideoFormat>((k, i) => ({...d.videoFormatList[k], index: i})),
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
                    currentPipelineIndex: d.currentPipelineIndex,
                    pipelineSettings: d.currentPipelineSettings
                }))
            });
        },
        /**
         * Create a new Pipeline for the provided camera.
         *
         * @param newPipelineName the name of the new pipeline.
         * @param pipelineType the type of the new pipeline. Cannot be {@link WebsocketPipelineType.Calib3d} or {@link WebsocketPipelineType.DriverMode}.
         * @param cameraIndex the index of the camera
         */
        createNewPipeline(newPipelineName: string, pipelineType: Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode>, cameraIndex: number) {
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
         * @param cameraIndex the index of the camera
         */
        changeCurrentPipelineSetting(settings: ConfigurableReflectivePipelineSettings | ConfigurableColoredShapePipelineSettings | ConfigurableAprilTagPipelineSettings, cameraIndex: number) {
            const payload = {
                changePipelineSetting: {
                    ...settings,
                    cameraIndex: cameraIndex
                }
            };
            this.$patch(store => store.cameras[cameraIndex].pipelineSettings = {
                ...store.cameras[cameraIndex].pipelineSettings,
                ...settings
            });
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Change the nickname of the currently selected pipeline of the provided camera.
         *
         * @param newName the new nickname for the camera.
         * @param cameraIndex the index of the camera
         */
        changeCurrentPipelineNickname(newName: string, cameraIndex: number) {
            const payload = {
                changePipelineName: newName,
                cameraIndex: cameraIndex
            };
            this.$patch(store => store.cameras[cameraIndex].pipelineSettings.pipelineNickname = newName);
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Modify the Pipeline type of the currently selected pipeline of the provided camera. This overwrites the current pipeline's settings.
         *
         * @param type the pipeline type to set.  Cannot be {@link WebsocketPipelineType.Calib3d} or {@link WebsocketPipelineType.DriverMode}.
         * @param cameraIndex the index of the camera.
         */
        changeCurrentPipelineType(type: Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode>, cameraIndex: number) {
            const payload = {
                pipelineType: type,
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Change the currently selected pipeline of the provided camera.
         *
         * @param cameraIndex the index of the camera's pipeline to change.
         */
        deleteCurrentPipeline(cameraIndex: number) {
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
        duplicatePipeline(pipelineIndex: number, cameraIndex: number) {
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
         */
        setCurrentCameraIndex(cameraIndex: number) {
            const payload = {
                currentCamera: cameraIndex
            };
            useStateStore().$patch({currentCameraIndex: cameraIndex});
            useStateStore().websocket?.send(payload, true);
        },
        /**
         * Start the 3D calibration process for the provided camera. This method should be called along with an update to the store with the current pipeline index set to {@link WebsocketPipelineType.Calib3d}. Note that the backend already handles updating the pipeline index prop.
         *
         * @param calibrationInitData initialization calibration data.
         * @param cameraIndex the index of the camera.
         */
        startPnpCalibration(calibrationInitData: {
            squareSizeIn: number,
            patternWidth: number,
            patternHeight: number,
            boardType: CalibrationBoardTypes
        }, cameraIndex: number) {
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
         * Take a snapshot for the calibration processes
         *
         * @param takeSnapshot whether or not to take a snapshot. Defaults to true
         * @param cameraIndex the index of the camera that is currently in the calibration process
         */
        takeCalibrationSnapshot(takeSnapshot = true, cameraIndex: number) {
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
        takeRobotOffsetPoint(type: RobotOffsetType, cameraIndex: number) {
            const payload = {
                robotOffsetPoint: type,
                cameraIndex: cameraIndex
            };
            useStateStore().websocket?.send(payload, true);
        }
    }
});
