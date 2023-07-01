import type {GeneralSettings, LightingSettings, MetricData} from "@/types/SettingTypes";
import type {
    AprilTagPipelineSettings,
    ColoredShapePipelineSettings,
    ConfigurableAprilTagPipelineSettings,
    ConfigurableColoredShapePipelineSettings,
    ConfigurableReflectivePipelineSettings,
    ReflectivePipelineSettings
} from "@/types/PipelineTypes";

export interface WebsocketLogMessage {
    logMessage: {
        logLevel: number,
        logMessage: string
    }
}
export interface WebsocketSettingsUpdate {
    general: Required<GeneralSettings>,
    lighting: Required<LightingSettings>,
    networkSettings: {
        connectionType: number,
        hostname: string,
        networkMangerIface?: string,
        ntServerAddress: string,
        physicalInterface?: string,
        runNTServer: boolean,
        setDHCPcommand?: string,
        setStaticCommand?: string,
        shouldManage: boolean,
        staticIp: string
    }
}
export interface WebsocketCameraSettingsUpdate {
    calibrations: {
        distCoeffs: number[],
        height: number,
        width: number,
        standardDeviation: number,
        perViewErrors: number[],
        intrinsics: number[],
    }[],
    currentPipelineIndex: number,
    currentPipelineSettings: ReflectivePipelineSettings | ColoredShapePipelineSettings | AprilTagPipelineSettings,
    fov: number,
    inputStreamPort: number,
    isFovConfigurable: boolean,
    nickname: string,
    outputStreamPort: number,
    pipelineNicknames: string[],
    videoFormatList: {
        [key: number]: {
            fps: number,
            height: number,
            width: number,
            pixelFormat: string,
            index?: number,
            diagonalFOV?: number,
            horizontalFOV?: number,
            verticalFOV?: number,
            standardDeviation?: number,
            mean?: number
        }
    }
}
export interface WebsocketNTUpdate {
    connected: boolean,
    address?: string,
    clients?: number
}
export interface WebsocketPipelineResultUpdate {
    [key: number]: {
        fps: number,
        latency: number,
        targets: {
            yaw: number,
            pitch: number,
            skew: number,
            area: number,
            ambiguity: number,
            fiducialId: number,
            pose: {
                "angle_z": number,
                "qw": number,
                "qx": number,
                "x": number,
                "qy": number,
                "y": number,
                "qz": number,
                "z": number
            },
        }[]
    }
}
export interface WebsocketCalibrationData {
    "patternWidth": number,
    "boardType": number,
    "hasEnough": boolean,
    "count": number,
    "minCount": number,
    "videoModeIndex": number,
    "patternHeight": number,
    "squareSizeIn": number
}

export interface IncomingWebsocketData {
    log?: WebsocketLogMessage,
    settings?: WebsocketSettingsUpdate,
    cameraSettings?: WebsocketCameraSettingsUpdate[],
    ntConnectionInfo?: WebsocketNTUpdate,
    metrics?: Required<MetricData>,
    updatePipelineResult?: WebsocketPipelineResultUpdate,
    calibrationData?: WebsocketCalibrationData
}

enum WebsocketPipelineType {
    Calib3d=-2,
    DriverMode=-1,
    Reflective=0,
    Colored=1,
    AprilTag=2,
    Aruco=3
}

enum CalibrationBoardTypes {
    Chessboard=0,
    DotBoard=1
}

enum RobotOffsetType {
    ROPO_CLEAR=0,
    ROPO_TAKESINGLE=1,
    ROPO_TAKEFIRSTDUAL=2,
    ROPO_TAKESECONDDUAL=3
}

/**
 * Change the nickname of the currently selected pipeline of the provided camera.
 *
 * @param newName the new nickname for the camera.
 * @param cameraIndex the index of the camera
 */
const changeCurrentPipelineNickname = (newName: string, cameraIndex: number) => {
    const payload = {
        changePipelineName: newName,
        cameraIndex: cameraIndex
    };
};

/**
 * Create a new Pipeline for the provided camera.
 *
 * @param newPipelineName the name of the new pipeline.
 * @param pipelineType the type of the new pipeline. Cannot be {@link WebsocketPipelineType.Calib3d} or {@link WebsocketPipelineType.DriverMode}.
 * @param cameraIndex the index of the camera
 */
const createNewPipeline = (newPipelineName: string, pipelineType: Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode>, cameraIndex: number) => {
    const payload = {
        addNewPipeline: [newPipelineName, pipelineType],
        cameraIndex: cameraIndex
    };
};

/**
 * Change the currently selected pipeline of the provided camera.
 *
 * @param cameraIndex the index of the camera's pipeline to change.
 */
const deleteCurrentPipeline = (cameraIndex: number) => {
    const payload = {
        deleteCurrentPipeline: {},
        cameraIndex: cameraIndex
    };
};

/**
 * Change the currently set camera
 *
 * @param cameraIndex the index of the camera to set.
 */
const setCurrentCameraIndex = (cameraIndex: number) => {
    const payload = {
        currentCamera: cameraIndex
    };
};

/**
 * Modify the settings of the currently selected pipeline of the provided camera.
 *
 * @param settings settings to modify. The type of the settings should match the currently selected pipeline type.
 * @param cameraIndex the index of the camera
 */
const changeCurrentPipelineSetting = (settings: ConfigurableReflectivePipelineSettings | ConfigurableColoredShapePipelineSettings | ConfigurableAprilTagPipelineSettings, cameraIndex: number) => {
    const payload = {
        changePipelineSetting: {
            ...settings,
            cameraIndex: cameraIndex
        }
    };
};

/**
 * Start the 3D calibration process for the provided camera. This method should be called along with an update to the store with the current pipeline index set to {@link WebsocketPipelineType.Calib3d}. Note that the backend already handles updating the pipeline index prop.
 *
 * @param calibData initialization calibration data.
 * @param cameraIndex the index of the camera.
 */
const startPnpCalibration = (calibData: {
    count: number,
    minCount: number,
    hasEnough: false,
    videoModeIndex: number,
    squareSizeIn: number,
    patternWidth: number,
    patternHeight: number,
    boardType: CalibrationBoardTypes
}, cameraIndex: number) => {
    const payload = {
        startPnpCalibration: calibData,
        cameraIndex: cameraIndex
    };
};

/**
 * Take a snapshot for the calibration processes
 *
 * @param takeSnapshot whether or not to take a snapshot. Defaults to true
 * @param cameraIndex the index of the camera that is currently in the calibration process
 */
const takeCalibrationSnapshot = (takeSnapshot = true, cameraIndex: number) => {
    const payload = {
        takeCalibrationSnapshot: takeSnapshot,
        cameraIndex: cameraIndex
    };
};

/**
 * Duplicate the pipeline at the provided index.
 *
 * @param pipelineIndex index of the pipeline to duplicate.
 * @param cameraIndex the index of the camera.
 */
const duplicatePipeline = (pipelineIndex: number, cameraIndex: number) => {
    const payload = {
        duplicatePipeline: pipelineIndex,
        cameraIndex: cameraIndex
    };
};

/**
 * Modify the brightness of the LEDs.
 *
 * @param brightness brightness to set [0, 100]
 */
const changeLEDBrightness = (brightness: number) => {
    const payload = {
        enabledLEDPercentage: brightness
    };
};

/**
 * Set the robot offset mode type.
 *
 * @param type Offset Mode to set.
 * @param cameraIndex the index of the camera.
 */
const takeRobotOffsetPoint = (type: RobotOffsetType, cameraIndex: number) => {
    const payload = {
        robotOffsetPoint: type,
        cameraIndex: cameraIndex
    };
};

/**
 * Modify the Pipeline type of the currently selected pipeline of the provided camera.
 *
 * @param type the pipeline type to set.  Cannot be {@link WebsocketPipelineType.Calib3d} or {@link WebsocketPipelineType.DriverMode}.
 * @param cameraIndex the index of the camera.
 */
const changeCurrentPipelineType = (type: Exclude<WebsocketPipelineType, WebsocketPipelineType.Calib3d | WebsocketPipelineType.DriverMode>, cameraIndex: number) => {
    const payload = {
        pipelineType: type,
        cameraIndex: cameraIndex
    };
};
