import {defineStore} from "pinia";
import type {CameraSettings} from "@/types/SettingTypes";
import {useStateStore} from "@/stores/StateStore";
import type {WebsocketCameraSettingsUpdate} from "@/types/WebsocketDataTypes";
import type {CameraCalibrationResult, VideoFormat} from "@/types/SettingTypes";


interface CameraSettingsStore {
    cameras: CameraSettings[]
}

export const useCameraSettingsStore = defineStore("cameraSettings", {
    state: (): CameraSettingsStore => ({
        cameras: []
    }),
    getters: {
        currentCameraSettings(): CameraSettings | null {
            const currentCameraIndex = useStateStore().currentCameraIndex;

            if(this.cameras.length === 0 || currentCameraIndex === undefined) {
                return null;
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
        }
    }
});