import { type ActivePipelineSettings, DefaultAprilTagPipelineSettings } from "@/types/PipelineTypes";

export interface GeneralSettings {
    version?: string
    gpuAcceleration?: string
    hardwareModel?: string
    hardwarePlatform?: string
}

export interface MetricData {
    cpuTemp?: string,
    cpuUtil?: string,
    cpuMem?: string,
    gpuMem?: string,
    ramUtil?: string
    gpuMemUtil?: string,
    cpuThr?: string,
    cpuUptime?: string,
    diskUtilPct?: string,
}

export enum NetworkConnectionType {
    DHCP = 0,
    Static = 1
}

export interface NetworkSettings {
    ntServerAddress: string
    connectionType: NetworkConnectionType,
    staticIp: string,
    hostname: string,
    runNTServer: boolean
    shouldMange: boolean,
    networkManagerIface?: string,
    physicalInterface?: string,
    setStaticCommand?: string,
    setDHCPcommand?: string
}

export interface LightingSettings {
    supported: boolean,
    brightness: number
}

export enum LogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG
}

export interface LogMessage {
    level: LogLevel,
    message: string
}

interface Resolution {
    width: number,
    height: number
}

export interface VideoFormat {
    resolution: Resolution
    fps: number,
    pixelFormat: string,
    index?: number,
    diagonalFOV?: number,
    horizontalFOV?: number,
    verticalFOV?: number,
    standardDeviation?: number,
    mean?: number
}

export interface CameraCalibrationResult {
    resolution: Resolution
    distCoeffs: number[],
    standardDeviation: number,
    perViewErrors: number[],
    intrinsics: number[],
}

export interface ConfigurableCameraSettings {
    fov: number
}

export interface CameraSettings {
    nickname: string

    fov: {
        value: number,
        managedByVendor: boolean
    }
    stream: {
        inputPort: number,
        outputPort: number
    }

    validVideoFormats: VideoFormat[]
    completeCalibrations: CameraCalibrationResult[]

    currentPipelineIndex: number,
    pipelineNicknames: string[],
    pipelineSettings: ActivePipelineSettings
}

export const PlaceholderCameraSettings: CameraSettings = {
    nickname: "Placeholder Camera",
    fov: {
        value: 70,
        managedByVendor: true
    },
    stream: {
        inputPort: 0,
        outputPort: 0
    },
    validVideoFormats: [
        {
            resolution: { width: 1920, height: 1080 },
            fps: 60,
            pixelFormat: "RGB"
        },
        {
            resolution: { width: 1280, height: 720 },
            fps: 60,
            pixelFormat: "RGB"
        },
        {
            resolution: { width: 640, height: 480 },
            fps: 30,
            pixelFormat: "RGB"
        }
    ],
    completeCalibrations: [],
    pipelineNicknames: ["Placeholder Pipeline"],
    currentPipelineIndex: 0,
    pipelineSettings: DefaultAprilTagPipelineSettings
};

export enum CalibrationBoardTypes {
    Chessboard=0,
    DotBoard=1
}

export enum RobotOffsetType {
    Clear=0,
    Single=1,
    DualFirst=2,
    DualSecond=3
}
